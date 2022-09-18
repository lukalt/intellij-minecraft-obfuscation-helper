package lt.lukasa.proguardviewer.mappings;

import java.util.List;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ObfuscationMethodMapping {
    private final String returnType;
    private final String realName;
    private final String obfuscatedName;
    private final List<String> parameterTypes;

    public ObfuscationMethodMapping(String returnType, String name, String obfuscatedName, List<String> parameterTypes) {
        this.returnType = returnType;
        this.realName = name;
        this.obfuscatedName = obfuscatedName;
        this.parameterTypes = parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getRealName() {
        return realName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public String toString() {
        return returnType + " " + realName + "(" + String.join(",", parameterTypes) + ") -> " + obfuscatedName;
    }
}
