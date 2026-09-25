/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.CharSequence
 *  java.lang.Float
 *  java.lang.NumberFormatException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.Arrays
 *  java.util.List
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package net.havoc.crates;

import java.lang.CharSequence;
import java.lang.Float;
import java.lang.NumberFormatException;
import java.lang.Object;
import java.lang.String;
import java.util.Arrays;
import java.util.List;
import net.havoc.crates.commands.AlertsCommand;
import net.havoc.crates.commands.CrateCommand;
import net.havoc.crates.commands.CratesCommand;
import net.havoc.crates.commands.KeyCommand;
import net.havoc.crates.crate.CrateManager;
import net.havoc.crates.data.ProfileManager;
import net.havoc.crates.listeners.CrateEditListener;
import net.havoc.crates.listeners.CrateInteractListener;
import net.havoc.crates.listeners.ProfileListener;
import net.havoc.crates.menu.MenuListener;
import net.havoc.crates.placeholder.CratesPlaceholder;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CratesPlugin
extends JavaPlugin {
    private static CratesPlugin instance;
    private Config mainConfig;
    private Config cratesConfig;
    private CrateManager crateManager;
    private ProfileManager profileManager;
    private net.havoc.crates.hologram.HologramManager hologramManager;
    private static final List<String> DEFAULT_TOGGLEABLE;
    private static final List<String> DEFAULT_ALWAYS_SHOW;

    public static CratesPlugin getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        this.mainConfig = new Config(this, "config.yml");
        this.cratesConfig = new Config(this, "crates.yml");
        this.profileManager = new ProfileManager(this);
        this.crateManager = new CrateManager(this);
        Bukkit.getPluginManager().registerEvents((Listener)new MenuListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new ProfileListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new CrateInteractListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new CrateEditListener(this), (Plugin)this);
        this.register("crates", new CratesCommand(this));
        this.register("crate", new CrateCommand(this));
        this.register("cratealerts", new AlertsCommand(this));
        this.register("key", new KeyCommand(this));
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CratesPlaceholder(this).register();
            this.getLogger().info("Hooked into PlaceholderAPI.");
        }
        Bukkit.getOnlinePlayers().forEach(player -> this.profileManager.loadAsync(player.getUniqueId()));
        if (!this.mainConfig.getConfiguration().contains("ALERTS")) {
            this.getLogger().info("config.yml has no ALERTS section - using the built-in defaults (REWARD_RECEIVED, INVENTORY_PARTIAL, RECEIVED_KEYS can be silenced with /cratealerts).");
        }
        this.hologramManager = new net.havoc.crates.hologram.HologramManager(this);
        this.hologramManager.start();
        this.getLogger().info("HavocCrates v" + this.getDescription().getVersion() + " enabled.");
    }

    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> player.closeInventory());
        if (this.hologramManager != null) {
            this.hologramManager.despawnAll();
        }
        if (this.profileManager != null) {
            this.profileManager.shutdown();
        }
    }

    private void register(String name, Object executor) {
        PluginCommand command = this.getCommand(name);
        if (command == null) {
            this.getLogger().warning("Command /" + name + " is missing from plugin.yml");
            return;
        }
        command.setExecutor((CommandExecutor)executor);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter)executor);
        }
    }

    public void reloadAll() {
        this.mainConfig.reload();
        this.crateManager.load();
        if (this.hologramManager != null) {
            this.hologramManager.refresh();
        }
    }

    public net.havoc.crates.hologram.HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public Config getMainConfig() {
        return this.mainConfig;
    }

    public Config getCratesConfig() {
        return this.cratesConfig;
    }

    public CrateManager getCrateManager() {
        return this.crateManager;
    }

    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    public void message(CommandSender sender, String key, String ... replacements) {
        if (this.isSilenced(sender, key)) {
            return;
        }
        this.messageAlways(sender, key, replacements);
    }

    public void messageAlways(CommandSender sender, String key, String ... replacements) {
        String message = this.mainConfig.getConfiguration().getString("MESSAGES." + key);
        if (message == null || message.isEmpty()) {
            return;
        }
        int index = 0;
        while (index + 1 < replacements.length) {
            message = message.replace((CharSequence)replacements[index], (CharSequence)replacements[index + 1]);
            index += 2;
        }
        sender.sendMessage(CC.translate(message));
    }

    private boolean isSilenced(CommandSender sender, String key) {
        if (!(sender instanceof Player) || this.profileManager == null) {
            return false;
        }
        if (this.profileManager.getProfile((Player)sender).isAlerts()) {
            return false;
        }
        List<String> alwaysShow = this.mainConfig.getConfiguration().getStringList("ALERTS.ALWAYS-SHOW");
        if (alwaysShow == null || alwaysShow.isEmpty()) {
            alwaysShow = DEFAULT_ALWAYS_SHOW;
        }
        if (this.contains(alwaysShow, key)) {
            return false;
        }
        List<String> toggleable = this.mainConfig.getConfiguration().getStringList("ALERTS.TOGGLEABLE");
        if (toggleable == null || toggleable.isEmpty()) {
            toggleable = DEFAULT_TOGGLEABLE;
        }
        return this.contains(toggleable, "*") || this.contains(toggleable, key);
    }

    public List<String> getToggleableMessages() {
        List toggleable = this.mainConfig.getConfiguration().getStringList("ALERTS.TOGGLEABLE");
        return toggleable == null || toggleable.isEmpty() ? DEFAULT_TOGGLEABLE : toggleable;
    }

    public List<String> getAlwaysShownMessages() {
        List always = this.mainConfig.getConfiguration().getStringList("ALERTS.ALWAYS-SHOW");
        return always == null || always.isEmpty() ? DEFAULT_ALWAYS_SHOW : always;
    }

    public boolean wouldSilence(CommandSender sender, String key) {
        return this.isSilenced(sender, key);
    }

    private boolean contains(List<String> list, String value) {
        for (String entry : list) {
            if (entry == null || !entry.trim().equalsIgnoreCase(value)) continue;
            return true;
        }
        return false;
    }

    /**
     * Spawns a particle burst written as "PARTICLE", "PARTICLE|count" or "PARTICLE|count|spread".
     * An unknown particle name is logged and ignored rather than breaking the purchase.
     */
    public void playEffect(Player player, String path, String def) {
        String raw = this.mainConfig.getString(path, def);
        if (raw == null || raw.trim().isEmpty() || raw.equalsIgnoreCase("none")) {
            return;
        }
        String[] parts = raw.split("\\|");
        int count = 25;
        double spread = 0.6;
        try {
            if (parts.length > 1) {
                count = Integer.parseInt(parts[1].trim());
            }
            if (parts.length > 2) {
                spread = Double.parseDouble(parts[2].trim());
            }
        } catch (NumberFormatException exception) {
            this.getLogger().warning("Bad particle count/spread in " + path + ": " + raw);
        }
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(
                    parts[0].trim().toUpperCase(java.util.Locale.ROOT));
            player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0),
                    count, spread, spread, spread, 0.0);
        } catch (IllegalArgumentException exception) {
            this.getLogger().warning("Unknown particle in " + path + ": " + parts[0]);
        }
    }

    public void playSound(Player player, String path, String def) {
        String raw = this.mainConfig.getString(path, def);
        if (raw == null || raw.trim().isEmpty() || raw.equalsIgnoreCase("none")) {
            return;
        }
        String[] parts = raw.split("\\|");
        String sound = parts[0].trim();
        float volume = 1.0f;
        float pitch = 1.0f;
        try {
            if (parts.length > 1) {
                volume = Float.parseFloat((String)parts[1].trim());
            }
            if (parts.length > 2) {
                pitch = Float.parseFloat((String)parts[2].trim());
            }
        }
        catch (NumberFormatException exception) {
            this.getLogger().warning("Bad sound volume/pitch in " + path + ": " + raw);
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public String getAlertStatus(Player player) {
        boolean enabled = this.profileManager.getProfile(player).isAlerts();
        return CC.translate(enabled ? this.mainConfig.getString("ALERTS.STATUS-ENABLED", "&aON") : this.mainConfig.getString("ALERTS.STATUS-DISABLED", "&cOFF"));
    }

    static {
        DEFAULT_TOGGLEABLE = Arrays.asList(new String[]{"REWARD_RECEIVED", "INVENTORY_PARTIAL", "RECEIVED_KEYS"});
        DEFAULT_ALWAYS_SHOW = Arrays.asList(new String[]{"NOT_ENOUGH_KEYS", "INVENTORY_FULL", "NO_PERMISSION", "PLAYERS_ONLY", "PLAYER_NOT_FOUND", "NO_CRATE_FOUND", "NO_CRATES_FOUND", "CRATE_PROTECTED", "ALERTS_ENABLED", "ALERTS_DISABLED"});
    }
}
