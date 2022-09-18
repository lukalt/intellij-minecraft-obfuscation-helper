package lt.lukasa.proguardviewer.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lt.lukasa.proguardviewer.mappings.ObfuscationClassMapping;
import lt.lukasa.proguardviewer.profiles.Profile;
import lt.lukasa.proguardviewer.profiles.ProfilePreset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class ProfileService implements IProfileManager {
    private final List<Profile> profiles;
    private final static Gson GSON = new Gson();

    public ProfileService() {
        this.profiles = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(ObfuscationClassMapping.class.getResourceAsStream("/default-profiles.json"), StandardCharsets.UTF_8)) {
            List<ProfilePreset> presets = GSON.fromJson(reader, new TypeToken<List<ProfilePreset>>() {}.getType());
            System.out.println("Loaded " + presets.size() + " presets");
            profiles.addAll(presets);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File cacheFolder = new File(System.getenv("LOCALAPPDATA"), "proguard-viewer");
        File customProfilesFile = new File(cacheFolder, "custom-profiles.json");
        if (customProfilesFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(customProfilesFile), StandardCharsets.UTF_8)) {
                List<ProfilePreset> customProfiles = GSON.fromJson(reader, new TypeToken<List<Profile>>() {}.getType());
                System.out.println("Loaded " + customProfiles.size() + " custom profiles");
                profiles.addAll(customProfiles);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Profile> getAvailableProfiles() {
        return this.profiles;
    }

    @Override
    public synchronized void addCustomProfile(Profile profile) {
        this.profiles.add(profile);
        save();
    }

    @Override
    public synchronized void removeCustomProfile(Profile profile) {
        this.profiles.remove(profile);
        save();
    }

    @Override
    public Profile getProfileById(String currentProjectId) {
        if(currentProjectId == null) {
            return null;
        }
        return this.profiles.stream().filter(a -> a.getId().equals(currentProjectId)).findAny().orElse(null);
    }

    private void save() {
        File cacheFolder = new File(System.getenv("LOCALAPPDATA"), "proguard-viewer");
        File customProfilesFile = new File(cacheFolder, "custom-profiles.json");
        if(!cacheFolder.exists()) {
            customProfilesFile.mkdirs();
        }

        try(PrintWriter writer = new PrintWriter(customProfilesFile)) {
            writer.print(new Gson().toJson(profiles.stream().filter(a -> !(a instanceof ProfilePreset)).collect(Collectors.toList())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
