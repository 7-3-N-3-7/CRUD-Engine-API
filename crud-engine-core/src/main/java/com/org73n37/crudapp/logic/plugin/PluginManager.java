package com.org73n37.crudapp.logic.plugin;

import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import com.org73n37.crudapp.logic.CrudEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
public class PluginManager {
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
    private final CrudEngine crudEngine;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<PluginLifecycleListener> listeners = new ArrayList<>();

    public PluginManager(CrudEngine crudEngine) {
        this.crudEngine = crudEngine;
    }

    /**
     * Dynamically loads a JAR file, scans it to identify the entry class annotated with @CrudResource,
     * and registers the controller and metadata into the runtime application context.
     */
    public void loadPluginAtRuntime(File jarFile) throws Exception {
        if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("Invalid plugin file path");
        }

        log.info("🔌 Loading plugin JAR at runtime: {}", jarFile.getName());
        URL jarUrl = jarFile.toURI().toURL();
        
        // Use Thread context ClassLoader as parent to resolve existing spring dependencies
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader())) {
            Class<?> pluginEntryClass = null;

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName()
                                .replace('/', '.')
                                .substring(0, entry.getName().length() - 6);
                        
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            if (clazz.isAnnotationPresent(CrudResource.class)) {
                                pluginEntryClass = clazz;
                                break;
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            log.debug("Skipping class {} during scan", className);
                        }
                    }
                }
            }

            if (pluginEntryClass != null) {
                log.info("Found predefined plugin entry class: {}", pluginEntryClass.getName());
                
                // 1. Register in CrudEngine memory cache
                crudEngine.registerResource(pluginEntryClass);
                
                // 2. Programmatically register WebFlux RestController bean & detect paths
                for (PluginLifecycleListener listener : listeners) {
                    listener.onPluginLoaded(pluginEntryClass);
                }
            } else {
                throw new IllegalStateException("No class annotated with @CrudResource found in plugin JAR!");
            }
        }
    }
}
