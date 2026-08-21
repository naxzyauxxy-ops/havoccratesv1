package net.havoc.crates.data.storage;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import net.havoc.crates.util.Config;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Stores key balances in data.yml. Default backend, no external driver required.
 */
public class FlatfileStorage implements KeyStorage {

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
    public void load(Profile profile) {
        profile.setAlerts(this.config.getConfiguration()
                .getBoolean("PLAYERS." + profile.getUuid() + ".ALERTS",
                        this.plugin.getMainConfig().getBoolean("ALERTS.DEFAULT", true)));
        ConfigurationSection section = this.config.getConfiguration()
                .getConfigurationSection("PLAYERS." + profile.getUuid() + ".KEYS");
        profile.getKeys().clear();
        if (section == null) {
            return;
        }
        for (String crate : section.getKeys(false)) {
            profile.getKeys().put(crate.toLowerCase(), section.getInt(crate));
        }
    }

    @Override
    public void save(Profile profile) {
        String base = "PLAYERS." + profile.getUuid();
        synchronized (this.config) {
            this.config.getConfiguration().set(base + ".ALERTS", profile.isAlerts());
            this.config.getConfiguration().set(base + ".KEYS", null);
            profile.getKeys().forEach((crate, amount) ->
                    this.config.getConfiguration().set(base + ".KEYS." + crate, amount));
            this.config.save();
        }
    }

    @Override
    public void close() {
        // nothing to close
    }
}
