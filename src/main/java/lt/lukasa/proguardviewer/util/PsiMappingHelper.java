package lt.lukasa.proguardviewer.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import lt.lukasa.proguardviewer.mappings.ObfuscationClassMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.mappings.ObfuscationMethodMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class PsiMappingHelper {
    public static PsiType getTypeForPsiClass(Project project, PsiClass psiClass) {
        return JavaPsiFacade.getInstance(project).getElementFactory().createType(psiClass);
    }

    public static String resolveMethod(ObfuscationMapping spigot, ObfuscationMapping mojang, PsiType baseType, String methodName, PsiType[] parameters) {
        Optional<ObfuscationClassMapping> mapping = spigot.getClassByRealName(baseType.getCanonicalText());

        if (mapping.isPresent()) {
            ObfuscationClassMapping clazz = mapping.get();
            Optional<ObfuscationClassMapping> mojangMapped = mojang.getClassByObfuscatedName(clazz.getObfuscatedName());
            if (mojangMapped.isPresent()) {
                List<ObfuscationMethodMapping> candidates = mojangMapped.get().getMethodsByObfuscatedName().get(methodName);
                if (candidates != null && !candidates.isEmpty()) {
                    Optional<ObfuscationMethodMapping> method = matchMethod(spigot, mojang, candidates, parameters == null ? new PsiType[0] : parameters);
                    if (method.isPresent()) {
                        return method.get().getRealName();
                    }
                }
            }
        }
        for (PsiType superType : baseType.getSuperTypes()) {
            String result = resolveMethod(spigot, mojang, superType, methodName, parameters);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static String trimType(String input) {
        int ind = input.indexOf("<");
        return ind >= 0 ? input.substring(0, ind) : input;
    }
    private static Optional<ObfuscationMethodMapping> matchMethod(ObfuscationMapping spigot, ObfuscationMapping mojang, List<ObfuscationMethodMapping> candidates, PsiType[] parameterTypes) {
        return candidates.stream().filter(
                m -> {
                    if (m.getParameterTypes().size() != parameterTypes.length) {
                        return false;
                    }
                    for (int i = 0; i < parameterTypes.length; i++) {
                        String expected = m.getParameterTypes().get(i);
                        Optional<String> r = spigot.getClassByRealName(parameterTypes[i].getCanonicalText())
                                .flatMap(s -> mojang.getClassByObfuscatedName(s.getObfuscatedName()))
                                .map(s -> s.getRealName());
                        String given = trimType(r.orElse(parameterTypes[i].getCanonicalText()));
                        if (!Objects.equals(expected, given)) {
                            return false;
                        }
                    }
                    return true;
                }
        ).findFirst();
    }
}
