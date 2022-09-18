package lt.lukasa.proguardviewer.service;

import lt.lukasa.proguardviewer.mappings.ObfuscationMapping;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public interface IObfuscationMappingService {
    boolean isMappingLoaded();

    boolean isMappingSupported();

    Result triggerMappingLoad(Runnable loadCallback);

    default Result triggerMappingLoad() {
        return triggerMappingLoad(() -> {});
    }

    ObfuscationMapping getMojangMappingIfPresent();

    ObfuscationMapping getSpigotMappingIfPresent();

    enum Result {
        AVAILABLE_NOW,
        TASK_DELAYED,
        DISABLED
    }
}
