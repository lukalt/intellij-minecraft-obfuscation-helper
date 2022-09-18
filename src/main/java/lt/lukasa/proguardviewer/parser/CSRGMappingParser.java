package lt.lukasa.proguardviewer.parser;

import lt.lukasa.proguardviewer.mappings.ObfuscationClassMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMethodMapping;
import lt.lukasa.proguardviewer.util.CharBuffer;
import lt.lukasa.proguardviewer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class CSRGMappingParser {
    public static ObfuscationMapping parseDefaultSpigotMapping() {
        try (Scanner classScanner = new Scanner(ObfuscationClassMapping.class.getResourceAsStream("/minecraft-server-1.19.2-R0.1-SNAPSHOT-maps-spigot.csrg"))) {
            try (Scanner memberScanner = new Scanner(ObfuscationClassMapping.class.getResourceAsStream("/minecraft-server-1.19.2-R0.1-SNAPSHOT-maps-spigot-members.csrg"))) {
                return parse(new PeekableScanner(classScanner), new PeekableScanner(memberScanner));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String parseJvmType(CharBuffer chars) {
        char first = chars.take();
        switch (first) {
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'D':
                return "double";
            case 'F':
                return "float";
            case 'I':
                return "integer";
            case 'J':
                return "long";
            case 'L': {
                StringBuilder builder = new StringBuilder();
                char next;
                while ((next = chars.take()) != ';') {
                    builder.append(next);
                }
                return StringUtil.mapClassName(builder.toString().replace('/', '.'));
            }
            case 'S':
                return "short";
            case 'Z':
                return "boolean";
            case '[':
                return parseJvmType(chars) + "[]";
            case 'V':
                return "void";
            default:
                throw new IllegalArgumentException("Could not parse JVM type: '" + first + "'");
        }
    }

    public static ObfuscationMapping parse(PeekableScanner classMappings, PeekableScanner memberMappings) {
        final ObfuscationMapping proguardMapping = new ObfuscationMapping();
        Map<String, ObfuscationClassMapping> byObfuscation = proguardMapping.getClassesByObfuscation();
        Map<String, ObfuscationClassMapping> byRealName = proguardMapping.getClassesByRealName();

        while (classMappings.hasNextLine()) {
            String line = classMappings.nextLine();
            if (line.startsWith("#")) {
                continue;
            }
            String[] split = line.replace('/', '.').split(" ", 2);
            ObfuscationClassMapping mapping = new ObfuscationClassMapping(StringUtil.mapClassName(split[1]), StringUtil.mapClassName(split[0]));
            byObfuscation.put(mapping.getObfuscatedName(), mapping);
            byRealName.put(mapping.getRealName(), mapping);
        }

        if (memberMappings != null) {
            while (memberMappings.hasNextLine()) {
                String line = memberMappings.nextLine();
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split(" ");

                String className = split[0].replace('/', '.');
                ObfuscationClassMapping classMapping = byRealName.get(className);
                if (classMapping == null) {
                    continue;
                }
                if (split.length == 3) { // field
                    String obfuscatedFieldName = split[1];
                    String realFieldName = split[2];
                    classMapping.getFieldByObfuscation().put(obfuscatedFieldName, realFieldName);
                    classMapping.getFieldByRealName().put(realFieldName, obfuscatedFieldName);
                } else if (split.length == 4) { // method
                    String obfuscatedMethodName = split[1];
                    String realMethodName = split[3];
                    CharBuffer signature = new CharBuffer(split[2]);
                    List<String> argumentTypes = new ArrayList<>();
                    assert signature.take() == '(';
                    while (signature.peek() != ')') {
                        argumentTypes.add(parseJvmType(signature));
                    }
                    assert signature.take() == ')';
                    String returnType = parseJvmType(signature);
                    assert !signature.hasNext();
                    ObfuscationMethodMapping method = new ObfuscationMethodMapping(returnType, realMethodName, obfuscatedMethodName, argumentTypes);
                    classMapping.getMethodsByObfuscatedName().computeIfAbsent(method.getObfuscatedName(), a -> new ArrayList<>()).add(method);
                    classMapping.getMethodsByRealName().computeIfAbsent(method.getRealName(), a -> new ArrayList<>()).add(method);
                }
            }

        }

        return proguardMapping;
    }
}
