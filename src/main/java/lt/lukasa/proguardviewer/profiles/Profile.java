package lt.lukasa.proguardviewer.profiles;

import java.util.Map;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class Profile {
    private String id;
    private Map<String, String> mappings;

    public Profile(String id, Map<String, String> mappings) {
        this.id = id;
        this.mappings = mappings;
    }

    public Profile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id='" + id + '\'' +
                ", mappings=" + mappings +
                '}';
    }



    public boolean isPreset() {
        return this instanceof ProfilePreset;
    }
}
