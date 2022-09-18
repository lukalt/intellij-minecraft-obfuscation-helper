package lt.lukasa.proguardviewer.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.parser.CSRGMappingParser;
import lt.lukasa.proguardviewer.parser.PeekableScanner;
import lt.lukasa.proguardviewer.parser.ProguardMappingParser;
import lt.lukasa.proguardviewer.profiles.Profile;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class ObfuscationMappingService implements IObfuscationMappingService {
    public static final String GROUP_ID = "lt.lukasa.proguardviewer";
    private final Project project;


    public ObfuscationMappingService(Project project) {
        this.project = project;
    }

    private ObfuscationMapping mojangMapping;
    private ObfuscationMapping spigotMapping;

    private Profile currentlyLoadedProfile;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    @Override
    public boolean isMappingLoaded() {
        return isMappingSupported() && mojangMapping != null && spigotMapping != null;
    }

    @Override
    public boolean isMappingSupported() {
        return true;
    }

    public static String getSha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(value.getBytes());
            return Hex.encodeHexString(md.digest());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private ListenableFuture<ObfuscationMapping> load(Project project, String label, String input, Function<PeekableScanner, ObfuscationMapping> parser) {
        SettableFuture<ObfuscationMapping> result = SettableFuture.create();
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(new Task.Backgroundable(project, "Downloading " + label + " mappings") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    File cacheFolder = new File(System.getenv("LOCALAPPDATA"), "proguard-viewer");
                    System.out.println("Cache folder: " + cacheFolder);
                    if (!cacheFolder.exists()) {
                        cacheFolder.mkdirs();
                    }

                    File data = new File(cacheFolder, label + "_" + getSha256(input));
                    if (!data.exists()) {
                        System.out.println("Invoking download of " + input);
                        try (FileOutputStream fos = new FileOutputStream(data)) {
                            URL url = new URL(input);
                            URLConnection connection = url.openConnection();
                            try (InputStream inputStream = connection.getInputStream()) {
                                byte[] buf = new byte[4096];
                                int read;
                                while ((read = inputStream.read(buf)) > 0) {
                                    fos.write(buf, 0, read);
                                }
                            }
                            System.out.println("Download completed!");
                        } catch (Throwable throwable) {
                            result.setException(throwable);
                            return;
                        }
                    }

                    try (InputStream inputStream = new FileInputStream(data)) {
                        try {
                            PeekableScanner scanner = new PeekableScanner(new Scanner(inputStream));
                            ObfuscationMapping mapping = parser.apply(scanner);
                            Notifications.Bus.notify(new Notification(GROUP_ID, "Mappings loaded", "Successfully loaded " + label + " from " + input, NotificationType.INFORMATION), project);
                            result.set(mapping);
                        } finally {
                            inputStream.close();
                        }
                    }
                    // Do not close input stream here

                } catch (Throwable t) {
                    Notifications.Bus.notify(new Notification(GROUP_ID, "Download failed", "Download of " + label + " from " + input + " failed", NotificationType.ERROR), project);
                    result.setException(t);
                }

            }
        }, new AbstractProgressIndicatorBase() {

        });
        return result;
    }


    @Override
    public Result triggerMappingLoad(Runnable loadCallback) {
        IProfileManager profileManager = ApplicationManager.getApplication().getService(IProfileManager.class);
        Profile currentProfile = profileManager.getProfileById(ProjectSettingsStateService.getInstance(project).getCurrentProjectId());
        if(currentProfile == null) {
            this.mojangMapping = null;
            this.spigotMapping = null;
            return Result.DISABLED;
        }
        if (currentProfile.equals(this.currentlyLoadedProfile) && mojangMapping != null && spigotMapping != null) {
            return Result.AVAILABLE_NOW;
        }
        this.currentlyLoadedProfile = currentProfile;
        if (!isMappingSupported()) {
            return Result.DISABLED;
        }
        if (!loading.compareAndSet(false, true)) {
            return Result.TASK_DELAYED;
        }

        AtomicInteger tasksToDo = new AtomicInteger(2);
        System.out.println("Loading profile " + currentProfile.getId());
        Futures.addCallback(load(project, "mojang", currentProfile.getMappings().get("mojang"), ProguardMappingParser::parseProguard), new FutureCallback<>() {

            @Override
            public void onSuccess(ObfuscationMapping result) {
                mojangMapping = result;
                if (tasksToDo.decrementAndGet() == 0) {
                    loading.set(false);
                    System.out.println("All mappings have been loaded!");
                    EventQueue.invokeLater(loadCallback);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("Loading of " + "mojang" + " failed");
                t.printStackTrace();
            }
        }, EventQueue::invokeLater);
        Futures.addCallback(load(project, "spigot", currentProfile.getMappings().get("spigot-classes"), scanner -> CSRGMappingParser.parse(scanner, null)), new FutureCallback<>() {

            @Override
            public void onSuccess(ObfuscationMapping result) {
                spigotMapping = result;
                if (tasksToDo.decrementAndGet() == 0) {
                    loading.set(false);
                    System.out.println("All mappings have been loaded!");
                    EventQueue.invokeLater(loadCallback);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("Loading of " + "spigot" + " failed");
                t.printStackTrace();
            }
        }, EventQueue::invokeLater);
        return Result.TASK_DELAYED;
    }

    @Override
    public ObfuscationMapping getMojangMappingIfPresent() {
        return mojangMapping;
    }

    @Override
    public ObfuscationMapping getSpigotMappingIfPresent() {
        return spigotMapping;
    }
}
