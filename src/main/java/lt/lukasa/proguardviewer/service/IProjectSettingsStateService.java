package lt.lukasa.proguardviewer.service;

/**
 * @author Lukas Alt
 * @since 17.09.2022
 */
public interface IProjectSettingsStateService {
    String getCurrentProjectId();

    void setCurrentProjectId(String s);
}
