package lt.lukasa.proguardviewer.configurable;

import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.labels.BoldLabel;
import lt.lukasa.proguardviewer.profiles.Profile;
import lt.lukasa.proguardviewer.profiles.ProfilePreset;
import lt.lukasa.proguardviewer.service.IObfuscationMappingService;
import lt.lukasa.proguardviewer.service.IProfileManager;
import lt.lukasa.proguardviewer.service.IProjectSettingsStateService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class ProjectSettingsConfigurable implements Configurable {
    private final Project project;

    public ProjectSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Minecraft Obfuscation Helper";
    }

    private static Box alignLeft(Container container) {
        Box box = Box.createHorizontalBox();
        box.add(container);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(alignLeft(new BoldLabel("Available Profiles:")));
        IProfileManager profileManager = ApplicationManager.getApplication().getService(IProfileManager.class);

        List<Profile> elements = profileManager.getAvailableProfiles();
        ComboBox<Profile> profileSelection = new ComboBox<>(new ComboBoxModel<>() {
            @Override
            public void setSelectedItem(Object o) {
                project.getService(IProjectSettingsStateService.class).setCurrentProjectId(o instanceof Profile ? ((Profile) o).getId() : null);
                IObfuscationMappingService service = project.getService(IObfuscationMappingService.class);

                service.triggerMappingLoad(ParameterHintsPassFactory::forceHintsUpdateOnNextPass);

            }

            @Override
            public Object getSelectedItem() {
                String selectedId = project.getService(IProjectSettingsStateService.class).getCurrentProjectId();
                if (selectedId == null) {
                    return null;
                }
                return profileManager.getAvailableProfiles().stream().filter(a -> a.getId().equals(selectedId)).findFirst().orElse(null);
            }


            @Override
            public int getSize() {
                return elements.size() + 1;
            }

            @Override
            public Profile getElementAt(int i) {
                return i == 0 ? null : elements.get(i - 1);
            }

            @Override
            public void addListDataListener(ListDataListener listDataListener) {
            }

            @Override
            public void removeListDataListener(ListDataListener listDataListener) {
            }
        });

        final CollectionListModel<Profile> profileCollectionListModel = new CollectionListModel<>(elements, true);
        JBList<Profile> profileList = new JBList<>(profileCollectionListModel);
        profileList.setCellRenderer(new ListCellRenderer<>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Profile> jList, Profile profile, int i, boolean b, boolean b1) {
                final JLabel component = b ? new BoldLabel(profile.getId()) : new JLabel(profile.getId());
                if (profile.isPreset()) {
                    component.setEnabled(false);
                }
                return component;
            }
        });

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(profileList)
                .disableUpDownActions();
        decorator.setAddAction(button -> {
            AddCustomProfileDialog dialog = new AddCustomProfileDialog();
            if (dialog.showAndGet()) {

                Map<String, String> mappings = new HashMap<>();
                mappings.put("mojang", dialog.mojangUrl.getText());
                mappings.put("spigot-classes", dialog.spigotClassUrl.getText());
                mappings.put("spigot-members", dialog.spigotMemberUrl.getText());
                String id = dialog.id.getText();
                if (elements.stream().anyMatch(a -> a.getId().equals(id))) {
                    return;
                }
                profileManager.addCustomProfile(new Profile(id, mappings));
                profileList.updateUI();
                profileSelection.updateUI();
            }
        });
        decorator.setRemoveAction(button -> {
            Profile selected = profileList.getSelectedValue();
            if (selected != null && !(selected instanceof ProfilePreset)) {
                profileManager.removeCustomProfile(selected);
                profileList.updateUI();
                profileSelection.updateUI();
            }
        });
        panel.add(decorator.createPanel());
        panel.add(alignLeft(new BoldLabel("Selected Profile:")));

        profileSelection.setRenderer(new ListCellRenderer<>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Profile> jList, Profile profile, int i, boolean b, boolean b1) {
                return profile == null ? new JLabel("-") : (b ? new BoldLabel(profile.getId()) : new JLabel(profile.getId()));
            }
        });
        panel.add(profileSelection);
        return panel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    static class AddCustomProfileDialog extends DialogWrapper {
        final JBTextField id = new JBTextField();
        final JBTextField mojangUrl = new JBTextField();
        final JBTextField spigotClassUrl = new JBTextField();
        final JBTextField spigotMemberUrl = new JBTextField();

        protected AddCustomProfileDialog() {
            super(true);
            setTitle("Add custom profile");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            dialogPanel.add(new JLabel("ID"));
            dialogPanel.add(id);
            dialogPanel.add(new JLabel("Mojang Mapping URL"));
            dialogPanel.add(mojangUrl);
            dialogPanel.add(new JLabel("Spigot Class Mapping URL"));
            dialogPanel.add(spigotClassUrl);
            dialogPanel.add(new JLabel("Spigot Members Mapping URL (optional)"));
            dialogPanel.add(spigotMemberUrl);

            return dialogPanel;
        }
    }
}
