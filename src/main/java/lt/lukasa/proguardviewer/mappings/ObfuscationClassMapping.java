package lt.lukasa.proguardviewer.mappings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ObfuscationClassMapping {
    private final String realName;
    private final String obfuscatedName;
    private final Map<String, String> fieldByObfuscation = new HashMap<>();
    private final Map<String, String> fieldByRealName = new HashMap<>();
    private final Map<String, List<ObfuscationMethodMapping>> methodsByObfuscatedName = new HashMap<>();
    private final Map<String, List<ObfuscationMethodMapping>> methodsByRealName = new HashMap<>();

    public ObfuscationClassMapping(String realName, String obfuscatedName) {
        this.realName = realName;
        this.obfuscatedName = obfuscatedName;
    }

    public String getRealName() {
        return realName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public Map<String, String> getFieldByObfuscation() {
        return fieldByObfuscation;
    }

    public Map<String, String> getFieldByRealName() {
        return fieldByRealName;
    }

    public Map<String, List<ObfuscationMethodMapping>> getMethodsByObfuscatedName() {
        return methodsByObfuscatedName;
    }

    public Map<String, List<ObfuscationMethodMapping>> getMethodsByRealName() {
        return methodsByRealName;
    }

    public Optional<String> deobfuscateField(String fieldName) {
        return Optional.ofNullable(this.fieldByObfuscation.get(fieldName));
    }

    public Optional<String> obfuscateField(String fieldName) {
        return Optional.ofNullable(this.fieldByRealName.get(fieldName));
    }

    @Override
    public String toString() {
        return "ProguardClassMapping{" +
                "realName='" + realName + '\'' +
                ", obfuscatedName='" + obfuscatedName + '\'' +
                ", fieldByObfuscation=" + fieldByObfuscation +
                ", fieldByRealName=" + fieldByRealName +
                ", obfuscatedMethods=" + methodsByObfuscatedName +
                '}';
    }
}
