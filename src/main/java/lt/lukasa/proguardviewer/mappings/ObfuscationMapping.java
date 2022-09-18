package lt.lukasa.proguardviewer.mappings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ObfuscationMapping {
    private final Map<String, ObfuscationClassMapping> classesByObfuscation = new HashMap<>();
    private final Map<String, ObfuscationClassMapping> classesByRealName = new HashMap<>();

    public Map<String, ObfuscationClassMapping> getClassesByObfuscation() {
        return classesByObfuscation;
    }

    public Map<String, ObfuscationClassMapping> getClassesByRealName() {
        return classesByRealName;
    }

    @Override
    public String toString() {
        return classesByObfuscation.toString();
    }

    public boolean hasObfuscatedClass(String obfuscatedName) {
        return this.classesByObfuscation.containsKey(obfuscatedName);
    }

    public boolean hasRealClass(String obfuscatedName) {
        return this.classesByRealName.containsKey(obfuscatedName);
    }

    public Optional<ObfuscationClassMapping> getClassByObfuscatedName(String obfuscatedName) {
        return Optional.ofNullable(this.classesByObfuscation.get(obfuscatedName));
    }

    public Optional<ObfuscationClassMapping> getClassByRealName(String realName) {
        return Optional.ofNullable(this.classesByRealName.get(realName));
    }
}
