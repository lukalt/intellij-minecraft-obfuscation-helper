package lt.lukasa.proguardviewer.extensions;

import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.psi.*;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMethodMapping;
import lt.lukasa.proguardviewer.service.IObfuscationMappingService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ObfuscationDocumentationProvider extends JavaDocumentationProvider {

    private static String shortenNMS(String input) {
        String[] split = input.split("\\.");
        if (split.length > 3 && split[0].equals("net") && split[1].equals("minecraft")) {
            split[0] = "n";
            split[1] = "m";
            split[2] = split[2].isEmpty() ? "" : split[2].substring(0, 1);
        }
        return String.join(".", split);
    }

    private static String toHtml(List<String> input) {
        return "<ul><li>" + String.join("</li><li>", input) + "</li></ul>";
    }

    public static Optional<ObfuscationMethodMapping> findMatch(List<ObfuscationMethodMapping> candidates, PsiType[] parameterTypes) {
        return candidates.stream().filter(
                m -> {
                    if (m.getParameterTypes().size() != parameterTypes.length) {
                        return false;
                    }
                    for (int i = 0; i < parameterTypes.length; i++) {
                        String expected = parameterTypes[i].getCanonicalText();
                        if (!Objects.equals(expected, m.getParameterTypes().get(i))) {
                            return false;
                        }
                    }
                    return true;
                }
        ).findFirst();
    }

    public static String formatClass(ObfuscationMapping mojang, String input) {

        return mojang.getClassByRealName(input).map(a -> a.getObfuscatedName()).map(a -> shortenNMS(a)).orElse(input);
    }

    private String formatMethod(ObfuscationMapping mojang, ObfuscationMethodMapping method) {

        return formatClass(mojang, method.getReturnType()) + " " + method.getRealName() + "(" + method.getParameterTypes().stream().map(a -> formatClass(mojang, a)).collect(Collectors.joining(", ")) + ")";
    }

    private Optional<List<String>> annotateMethod(ObfuscationMapping spigot, ObfuscationMapping mojang, String className, String name, PsiType[] parameterTypes, PsiTypeElement returnTypeElement) {
        List<String> list = new ArrayList<>();
        spigot.getClassByRealName(className).ifPresent(clazz -> {
            List<ObfuscationMethodMapping> candidates = clazz.getMethodsByObfuscatedName().get(name);
            if (candidates != null && !candidates.isEmpty()) {
                findMatch(candidates, parameterTypes).map((ObfuscationMethodMapping mapping) -> formatMethod(spigot, mapping)).ifPresent(list::add);
            }
        });
        return list.isEmpty() ? Optional.empty() : Optional.of(collapse(list));
    }

    private Optional<List<String>> annotateClass(ObfuscationMapping spigot, ObfuscationMapping mojang, String className) {
        List<String> list = new ArrayList<>();
        mojang.getClassByObfuscatedName(className).ifPresent(c -> {
            list.add(shortenNMS(c.getRealName()));
        });
        spigot.getClassByObfuscatedName(className).ifPresent(c -> {
            list.add(shortenNMS(c.getRealName()));
        });
        spigot.getClassByRealName(className).ifPresent(c -> {
            mojang.getClassByObfuscatedName(c.getObfuscatedName()).ifPresentOrElse(cm -> {
                list.add(shortenNMS(cm.getRealName()));
                list.add(shortenNMS("(" + c.getObfuscatedName() + ")"));
            }, () -> shortenNMS("(" + c.getObfuscatedName() + ")"));
        });
        return list.isEmpty() ? Optional.empty() : Optional.of(collapse(list));
    }

    private Optional<List<String>> annotateField(ObfuscationMapping spigot, ObfuscationMapping mojang, String className, String fieldName) {
        List<String> list = new ArrayList<>();
        mojang.getClassByObfuscatedName(className).ifPresent(c -> {
            c.deobfuscateField(fieldName).ifPresent(fd -> {
                list.add(fd);
            });
        });
        spigot.getClassByObfuscatedName(className).ifPresent(c -> {
            c.deobfuscateField(fieldName).ifPresent(fd -> {
                list.add(fd);
            });
        });
        spigot.getClassByRealName(className).ifPresent(c -> {
            c.obfuscateField(fieldName).ifPresent(fd -> {
                list.add(fd);
                mojang.getClassByObfuscatedName(c.getObfuscatedName()).ifPresent(cm -> {
                    cm.deobfuscateField(fd).ifPresent(mojangUnmapped -> {
                        list.add(mojangUnmapped);
                    });
                });
            });
            mojang.getClassByObfuscatedName(c.getObfuscatedName()).ifPresent(cm -> {
                cm.deobfuscateField(fieldName).ifPresent(mojangUnmapped -> {
                    list.add(mojangUnmapped);
                });
            });
        });
        return list.isEmpty() ? Optional.empty() : Optional.of(collapse(list));
    }

    private static List<String> collapse(List<String> in) {
        Set<String> track = new HashSet<>();
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (track.add(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private Optional<List<String>> annotateAsString(ObfuscationMapping spigot, ObfuscationMapping mojang, PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            return annotateMethod(spigot, mojang, method.getContainingClass().getQualifiedName(), method.getName(), method.getHierarchicalMethodSignature().getParameterTypes(), method.getReturnTypeElement());
        }
        if (element instanceof PsiVariable) {
            PsiVariable variable = (PsiVariable) element;
            final String fieldType = variable.getType().getCanonicalText();

            final Optional<List<String>> cl = annotateClass(spigot, mojang, fieldType);
            String targetClass;
            if (variable.getParent() instanceof PsiClass) {
                targetClass = ((PsiClass) variable.getParent()).getQualifiedName();
            } else {
                targetClass = fieldType;
            }
            final Optional<List<String>> fl = annotateField(spigot, mojang, targetClass, variable.getName());
            if (cl.isPresent() || fl.isPresent()) {
                String s = "";
                if (cl.isPresent()) {
                    s += "<b>Type:</b> " + String.join(", ", cl.get()) + "<br/>";
                }
                if (fl.isPresent()) {
                    s += "<b>Name:</b> " + String.join(", ", fl.get());
                }
                return Optional.of(List.of(s));
            } else {
                return Optional.of(Collections.emptyList());
            }
        }

        if (element instanceof PsiClass) {
            PsiClass variable = (PsiClass) element;
            final String fieldType = variable.getName();
            return annotateClass(spigot, mojang, fieldType).map(a -> Collections.singletonList("<b>Names:</b> " + String.join(", ", a)));
        }

        return Optional.of(Collections.singletonList("Type: " + element.getClass().getName()));
    }

   /* @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        String superText = super.generateDoc(element, originalElement);
        IObfuscationMappingService service = element.getProject().getService(IObfuscationMappingService.class);
        if (!service.triggerMappingLoad()) {
            return superText;
        }
        ObfuscationMapping spigot = service.getSpigotMappingIfPresent();
        ObfuscationMapping mojang = service.getMojangMappingIfPresent();
        if(spigot == null || mojang == null) {
            return superText;
        }
        Optional<List<String>> annotationResponse = annotateAsString(spigot, mojang, element);
        if (annotationResponse.isEmpty()) {
            return superText;
        }
        List<String> annotationText = annotationResponse.get();
        return superText + "<br/>" + String.join("<br/>", annotationText);


    }*/
}
