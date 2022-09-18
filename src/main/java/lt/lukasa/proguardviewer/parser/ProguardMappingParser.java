package lt.lukasa.proguardviewer.parser;

import lt.lukasa.proguardviewer.mappings.ObfuscationClassMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMethodMapping;
import lt.lukasa.proguardviewer.util.StringUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ProguardMappingParser {
    private final static Pattern CLASS_PATTERN = Pattern.compile("([a-zA-Z\\\\.0-9\\$]+) -> ([a-zA-Z\\\\.0-9\\$]+):");
    private final static Pattern FIELD_PATTERN = Pattern.compile("([a-zA-Z_0-9.\\[\\]\\$]+) ([a-zA-Z_0-9]+) -> ([a-zA-Z_0-9]+)");
    private final static Pattern METHOD_PATTERN = Pattern.compile("(?:([0-9]+):([0-9]+):)?([(a-zA-Z_0-9\\$. ,\\[\\]]+\\)|<init>) -> ([()a-zA-Z_0-9.\\[\\]]+|<init>)");


    public static ObfuscationMapping parseProguard(PeekableScanner scanner) {
        final ObfuscationMapping proguardMapping = new ObfuscationMapping();
        Map<String, ObfuscationClassMapping> classes = proguardMapping.getClassesByObfuscation();
        Map<String, ObfuscationClassMapping> mappings = proguardMapping.getClassesByRealName();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            while (line.startsWith("#")) {
                line = scanner.nextLine();
            }
            final Matcher matcher = CLASS_PATTERN.matcher(line);
            if (matcher.matches()) {
                final String realClassName = StringUtil.mapClassName(matcher.group(1));
                final String obfuscatedClassName = StringUtil.mapClassName(matcher.group(2));
                ObfuscationClassMapping clazz = processClass("    ", realClassName, obfuscatedClassName, scanner);
                classes.put(obfuscatedClassName, clazz);
                mappings.put(realClassName, clazz);
            }
        }

        return proguardMapping;
    }


    private static ObfuscationClassMapping processClass(String indent, String originalName, String obfuscatedName, PeekableScanner scanner) {
        ObfuscationClassMapping classMapping = new ObfuscationClassMapping(originalName, obfuscatedName);

        Map<String, String> fieldByObfuscation = classMapping.getFieldByObfuscation();
        Map<String, String> fieldByRealName = classMapping.getFieldByRealName();
        Map<String, List<ObfuscationMethodMapping>> methodsByObfuscation = classMapping.getMethodsByObfuscatedName();
        Map<String, List<ObfuscationMethodMapping>> methodsByRealName = classMapping.getMethodsByRealName();
        while (scanner.hasNextLine()) {
            if (!scanner.peekNextLine().startsWith(indent)) {
                break;
            }
            String line = scanner.nextLine();
            Matcher methodMatcher = METHOD_PATTERN.matcher(line.substring(indent.length()));
            if (methodMatcher.matches()) {
                int offset = 2;
                String unobfuscated = methodMatcher.group(offset + 1);
                String obfuscated = methodMatcher.group(offset + 2);
                if (unobfuscated.equals("<init>")) {
                    continue;
                }
                String type = unobfuscated.substring(0, unobfuscated.indexOf(" "));
                String realName = unobfuscated.substring(unobfuscated.indexOf(" ") + 1, unobfuscated.indexOf("("));
                final int beginIndex = unobfuscated.indexOf("(") + 1;
                final int endIndex = unobfuscated.length() - 1;
                String parametersStr = endIndex < beginIndex ? "" : unobfuscated.substring(beginIndex, endIndex);

                final ObfuscationMethodMapping mapping = new ObfuscationMethodMapping(type, realName, obfuscatedName, parametersStr.isEmpty() ? Collections.emptyList() : Arrays.stream(parametersStr.split(",")).map(StringUtil::mapClassName).collect(Collectors.toList()));
                methodsByObfuscation.computeIfAbsent(obfuscated, a -> new ArrayList<>()).add(mapping);
                methodsByRealName.computeIfAbsent(realName, a -> new ArrayList<>()).add(mapping);
            } else {
                Matcher fieldMatcher = FIELD_PATTERN.matcher(line.substring(indent.length()));
                if (fieldMatcher.matches()) {
                    fieldByObfuscation.put(fieldMatcher.group(3), fieldMatcher.group(2));
                    fieldByRealName.put(fieldMatcher.group(2), fieldMatcher.group(3));
                }
            }
        }
        return classMapping;
    }
}
