package com.org73n37.crudapp.logic.plugin;

public interface PluginLifecycleListener {
    void onPluginLoaded(Class<?> pluginClass) throws Exception;
}
