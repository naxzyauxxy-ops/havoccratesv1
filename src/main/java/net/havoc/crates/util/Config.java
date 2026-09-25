/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.io.File
 *  java.io.IOException
 *  java.lang.Object
 *  java.lang.String
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package net.havoc.crates.util;

import java.io.File;
import java.io.IOException;
import java.lang.Object;
import java.lang.String;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration configuration;

    public Config(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name.endsWith(".yml") ? name : name + ".yml";
        this.file = new File(plugin.getDataFolder(), this.name);
        if (!this.file.exists()) {
            if (plugin.getResource(this.name) != null) {
                plugin.saveResource(this.name, false);
            } else {
                try {
                    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                        plugin.getLogger().warning("Could not create the plugin data folder.");
                    }
                    if (!this.file.createNewFile()) {
                        plugin.getLogger().warning("Could not create " + this.name);
                    }
                }
                catch (IOException exception) {
                    plugin.getLogger().severe("Could not create " + this.name + ": " + exception.getMessage());
                }
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public FileConfiguration getConfiguration() {
        return this.configuration;
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public void save() {
        try {
            this.configuration.save(this.file);
        }
        catch (IOException exception) {
            this.plugin.getLogger().severe("Could not save " + this.name + ": " + exception.getMessage());
        }
    }

    public String getString(String path, String def) {
        return this.configuration.getString(path, def);
    }

    public int getInt(String path, int def) {
        return this.configuration.getInt(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return this.configuration.getBoolean(path, def);
    }
}
