package lt.lukasa.proguardviewer.extensions;

import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;
import lt.lukasa.proguardviewer.service.IObfuscationMappingService;
import lt.lukasa.proguardviewer.ui.JModelCheckBox;
import lt.lukasa.proguardviewer.util.PsiMappingHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class ObfuscationInlayParameterHintsProvider implements InlayHintsProvider<ObfuscationInlayParameterHintsProvider.Settings> {
    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull ObfuscationInlayParameterHintsProvider.Settings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.add(new JModelCheckBox("Show hints for method declarations", () -> settings.enableMethodsDeclarations, b -> settings.enableMethodsDeclarations = b));
                panel.add(new JModelCheckBox("Show hints for method references", () -> settings.enableMethodsReferences, b -> settings.enableMethodsReferences = b));
                panel.add(new JModelCheckBox("Show hints for field declarations", () -> settings.enableFieldDeclarations, b -> settings.enableFieldDeclarations = b));
                panel.add(new JModelCheckBox("Show hints for field references", () -> settings.enableFieldReferences, b -> settings.enableFieldReferences = b));

                return panel;
            }
        };
    }

    @NotNull
    @Override
    public Settings createSettings() {
        return new Settings();
    }


    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull ObfuscationInlayParameterHintsProvider.Settings settings, @NotNull InlayHintsSink inlayHintsSink) {

        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                if (!element.isValid()) {
                    return true;
                }

                IObfuscationMappingService service = psiFile.getProject().getService(IObfuscationMappingService.class);
                if (service.triggerMappingLoad(ParameterHintsPassFactory::forceHintsUpdateOnNextPass) != IObfuscationMappingService.Result.AVAILABLE_NOW) {
                    return true;
                }
                ObfuscationMapping spigot = service.getSpigotMappingIfPresent();
                ObfuscationMapping mojang = service.getMojangMappingIfPresent();
                if (spigot == null || mojang == null) {
                    return true;
                }
                processPsiItem(psiFile.getProject(), settings, spigot, mojang, element, inlayHintsSink);
                return true;
            }

            private void processPsiMethodDeclaration(Project project, ObfuscationMapping spigot, ObfuscationMapping mojang, PsiMethod method, PsiClass enclosingClass, @NotNull InlayHintsSink inlayHintsSink) {
                String resolvedName = PsiMappingHelper.resolveMethod(spigot, mojang, PsiMappingHelper.getTypeForPsiClass(project, enclosingClass), method.getName(), method.getHierarchicalMethodSignature().getParameterTypes());
                if (resolvedName != null) {
                    inlayHintsSink.addInlineElement((method.getNameIdentifier() != null ? method.getNameIdentifier().getTextOffset() : method.getTextOffset()) + method.getName().length(), true, getFactory().text(resolvedName), false);
                }
            }

            private void processPsiFieldDeclaration(ObfuscationMapping spigot, ObfuscationMapping mojang, PsiField field, PsiClass enclosingClass, @NotNull InlayHintsSink inlayHintsSink) {
                String obfName = spigot.getClassByRealName(enclosingClass.getQualifiedName()).map(a -> a.getObfuscatedName()).orElse(enclosingClass.getQualifiedName());
                mojang.getClassByObfuscatedName(obfName).ifPresent(mojangMapped -> {
                    String fieldName = mojangMapped.getFieldByObfuscation().get(field.getName());
                    if (fieldName != null) {
                        inlayHintsSink.addInlineElement(field.getNameIdentifier().getTextOffset() + field.getName().length(), true, getFactory().text(fieldName), false);
                    }
                });
            }

            private void processPsiMethodReference(Project project, ObfuscationMapping spigot, ObfuscationMapping mojang, PsiMethodReferenceExpression method, @NotNull InlayHintsSink inlayHintsSink) {
                PsiElement resolved = method.resolve();
                if (resolved != null) {
                    if (resolved instanceof PsiMethod) {
                        PsiMethod resolvedMethod = (PsiMethod) resolved;
                        String resolvedName = PsiMappingHelper.resolveMethod(spigot, mojang, PsiMappingHelper.getTypeForPsiClass(project, resolvedMethod.getContainingClass()), resolvedMethod.getName(), resolvedMethod.getHierarchicalMethodSignature().getParameterTypes());

                        if (resolvedName != null) {
                            inlayHintsSink.addInlineElement(method.getTextOffset() + method.getTextLength(), true, getFactory().text(resolvedName), false);
                        }
                    }
                }
            }

            private void processPsiMethodCall(Project project, ObfuscationMapping spigot, ObfuscationMapping mojang, PsiMethodCallExpression method, @NotNull InlayHintsSink inlayHintsSink) {

                if (method.getMethodExpression().getQualifierExpression() != null) {
                    if (method.getMethodExpression().getReference() != null) {
                        PsiElement resolved = method.getMethodExpression().getReference().resolve();
                        if (resolved instanceof PsiMethod) {
                            PsiMethod resolvedMethod = (PsiMethod) resolved;
                            String resolvedName = PsiMappingHelper.resolveMethod(spigot, mojang, PsiMappingHelper.getTypeForPsiClass(project, resolvedMethod.getContainingClass()), resolvedMethod.getName(), resolvedMethod.getHierarchicalMethodSignature().getParameterTypes());
                            if (resolvedName != null) {
                                inlayHintsSink.addInlineElement(method.getArgumentList().getTextOffset(), true, getFactory().text(resolvedName), false);
                            }
                        }
                    }
                }
            }


            private void processPsiFieldReference(ObfuscationMapping spigot, ObfuscationMapping mojang, PsiReference element, PsiField referent, @NotNull InlayHintsSink inlayHintsSink) {
                if (referent.getContainingClass() == null) {
                    return;
                }
                String obfName = spigot.getClassByRealName(referent.getContainingClass().getQualifiedName()).map(a -> a.getObfuscatedName()).orElse(referent.getContainingClass().getQualifiedName());
                mojang.getClassByObfuscatedName(obfName).ifPresent(mojangMapped -> {
                    String fieldName = mojangMapped.getFieldByObfuscation().get(referent.getName());
                    if (fieldName != null) {
                        inlayHintsSink.addInlineElement(element.getAbsoluteRange().getStartOffset() + element.getAbsoluteRange().getLength(), true, getFactory().text(fieldName), false);
                    }
                });
            }

            private void processPsiItem(Project project, Settings settings, ObfuscationMapping spigot, ObfuscationMapping mojang, PsiElement element, @NotNull InlayHintsSink inlayHintsSink) {
                if (element instanceof PsiReference) {
                    if (element instanceof PsiMethodReferenceExpression) {
                        if (settings.enableMethodsReferences) {
                            processPsiMethodReference(project, spigot, mojang, (PsiMethodReferenceExpression) element, inlayHintsSink);
                        }
                    } else {
                        PsiElement resolved = ((PsiReference) element).resolve();
                        if (resolved instanceof PsiField && settings.enableFieldReferences) {
                            processPsiFieldReference(spigot, mojang, (PsiReference) element, (PsiField) resolved, inlayHintsSink);
                        }
                    }
                } else if (element instanceof PsiMember) {
                    if (element instanceof PsiMethod && !((PsiMethod) element).isConstructor()) {
                        if (element.getParent() instanceof PsiClass && settings.enableMethodsDeclarations) {
                            processPsiMethodDeclaration(project, spigot, mojang, (PsiMethod) element, (PsiClass) element.getParent(), inlayHintsSink);
                        }
                    } else if (element instanceof PsiField) {
                        if (element.getParent() instanceof PsiClass && settings.enableFieldDeclarations) {
                            processPsiFieldDeclaration(spigot, mojang, (PsiField) element, (PsiClass) element.getParent(), inlayHintsSink);
                        }
                    }
                } else if (element instanceof PsiMethodCallExpression) {
                    if (settings.enableMethodsReferences) {
                        processPsiMethodCall(project, spigot, mojang, (PsiMethodCallExpression) element, inlayHintsSink);
                    }
                }
            }

        };
    }


    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language.getDisplayName().toLowerCase().contains("java");
    }

    public static class Settings {
        public boolean enableMethodsReferences = true;
        public boolean enableMethodsDeclarations = true;
        public boolean enableFieldDeclarations = true;
        public boolean enableFieldReferences = true;
    }


    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey getKey() {
        return new SettingsKey("lt.lukasa.obfuscation.settings");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "Minecraft Obfuscations";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }


    @NotNull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.CODE_VISION_GROUP;
    }
}
