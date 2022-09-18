package lt.lukasa.proguardviewer.service;

import lt.lukasa.proguardviewer.profiles.Profile;

import java.util.List;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public interface IProfileManager {
    List<Profile> getAvailableProfiles();

    void addCustomProfile(Profile profile);

    void removeCustomProfile(Profile profile);

    Profile getProfileById(String currentProjectId);

}
