/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  java.lang.Override
 *  java.lang.String
 *  org.bukkit.configuration.ConfigurationSection
 */
package net.havoc.crates.data.storage;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import net.havoc.crates.data.storage.KeyStorage;
import net.havoc.crates.util.Config;
import org.bukkit.configuration.ConfigurationSection;

public class FlatfileStorage
implements KeyStorage {
    private final CratesPlugin plugin;
    private Config config;

    public FlatfileStorage(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        this.config = new Config(this.plugin, "data.yml");
    }

    @Override
    public synchronized void load(Profile profile) {
        profile.setAlerts(this.config.getConfiguration().getBoolean("PLAYERS." + String.valueOf(profile.getUuid()) + ".ALERTS", this.plugin.getMainConfig().getBoolean("ALERTS.DEFAULT", true)));
        ConfigurationSection section = this.config.getConfiguration().getConfigurationSection("PLAYERS." + String.valueOf(profile.getUuid()) + ".KEYS");
        profile.getKeys().clear();
        if (section == null) {
            return;
        }
        for (String crate : section.getKeys(false)) {
            profile.getKeys().put(crate.toLowerCase(), section.getInt(crate));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void save(Profile profile) {
        String base = "PLAYERS." + String.valueOf(profile.getUuid());
        Config config = this.config;
        synchronized (config) {
            this.config.getConfiguration().set(base + ".ALERTS", profile.isAlerts());
            this.config.getConfiguration().set(base + ".KEYS", null);
            profile.getKeys().forEach((crate, amount) -> this.config.getConfiguration().set(base + ".KEYS." + crate, amount));
            this.config.save();
        }
    }

    @Override
    public void close() {
    }
}
