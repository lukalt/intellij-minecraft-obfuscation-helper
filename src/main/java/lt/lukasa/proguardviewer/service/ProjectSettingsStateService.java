package lt.lukasa.proguardviewer.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;

/**
 * @author Lukas Alt
 * @since 17.09.2022
 */
@State(name = "ProjectSettings")
public class ProjectSettingsStateService implements PersistentStateComponent<ProjectSettingsStateService.State>, IProjectSettingsStateService {

    public static IProjectSettingsStateService getInstance(Project project) {
        return project.getService(IProjectSettingsStateService.class);
    }

    static class State {
        public String selectedProfileId;
    }

    private State myState = new State();

    public State getState() {
        return myState;
    }

    public void loadState(State state) {
        myState = state;
    }

    @Override
    public String getCurrentProjectId() {
        return myState.selectedProfileId;
    }

    @Override
    public void setCurrentProjectId(String s) {
        myState.selectedProfileId = s;

    }
}