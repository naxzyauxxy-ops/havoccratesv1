package net.havoc.crates;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * HavocCrates - donut style crates with bulk buying.
 */
public class CratesPlugin extends JavaPlugin {

    private static CratesPlugin instance;

    private Config mainConfig;
    private Config cratesConfig;
    private CrateManager crateManager;
    private ProfileManager profileManager;

    public static CratesPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.mainConfig = new Config(this, "config.yml");
        this.cratesConfig = new Config(this, "crates.yml");
        this.profileManager = new ProfileManager(this);
        this.crateManager = new CrateManager(this);

        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new ProfileListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrateInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrateEditListener(this), this);

        register("crates", new CratesCommand(this));
        register("crate", new CrateCommand(this));
        register("cratealerts", new AlertsCommand(this));
        register("key", new KeyCommand(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CratesPlaceholder(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        // Players already online during a reload still need a profile.
        Bukkit.getOnlinePlayers().forEach(player -> this.profileManager.loadAsync(player.getUniqueId()));

        if (!this.mainConfig.getConfiguration().contains("ALERTS")) {
            getLogger().info("config.yml has no ALERTS section - using the built-in defaults "
                    + "(REWARD_RECEIVED, INVENTORY_PARTIAL, RECEIVED_KEYS can be silenced with /cratealerts).");
        }
        getLogger().info("HavocCrates v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Crates are only ever written by an admin action, so nothing is saved here -
        // that keeps a hand edited crates.yml exactly as it is.
        Bukkit.getOnlinePlayers().forEach(player -> player.closeInventory());
        if (this.profileManager != null) {
            this.profileManager.shutdown();
        }
    }

    private void register(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing from plugin.yml");
            return;
        }
        command.setExecutor((org.bukkit.command.CommandExecutor) executor);
        if (executor instanceof org.bukkit.command.TabCompleter) {
            command.setTabCompleter((org.bukkit.command.TabCompleter) executor);
        }
    }

    public void reloadAll() {
        this.mainConfig.reload();
        this.crateManager.load();
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

    /**
     * Sends a message from the MESSAGES section, applying %placeholder% pairs.
     */
    public void message(CommandSender sender, String key, String... replacements) {
        if (isSilenced(sender, key)) {
            return;
        }
        messageAlways(sender, key, replacements);
    }

    /**
     * Sends a message even when the player has crate alerts turned off.
     */
    public void messageAlways(CommandSender sender, String key, String... replacements) {
        String message = this.mainConfig.getConfiguration().getString("MESSAGES." + key);
        if (message == null || message.isEmpty()) {
            return;
        }
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        sender.sendMessage(CC.translate(message));
    }

    /**
     * Messages a player can silence with /cratealerts, used when the config has no ALERTS section
     * (an older config.yml would otherwise silence nothing at all).
     */
    private static final List<String> DEFAULT_TOGGLEABLE =
            Arrays.asList("REWARD_RECEIVED", "INVENTORY_PARTIAL", "RECEIVED_KEYS");

    /**
     * Never silenced, whatever the config says - a player must always learn why something failed.
     */
    private static final List<String> DEFAULT_ALWAYS_SHOW = Arrays.asList(
            "NOT_ENOUGH_KEYS", "INVENTORY_FULL", "NO_PERMISSION", "PLAYERS_ONLY",
            "PLAYER_NOT_FOUND", "NO_CRATE_FOUND", "NO_CRATES_FOUND", "CRATE_PROTECTED",
            "ALERTS_ENABLED", "ALERTS_DISABLED");

    /**
     * A message listed under ALERTS.TOGGLEABLE is skipped for players who ran /cratealerts off.
     * ALERTS.TOGGLEABLE may also be a single "*" to silence everything that is not an error.
     */
    private boolean isSilenced(CommandSender sender, String key) {
        if (!(sender instanceof Player) || this.profileManager == null) {
            return false;
        }
        if (this.profileManager.getProfile((Player) sender).isAlerts()) {
            return false;
        }

        List<String> alwaysShow = this.mainConfig.getConfiguration().getStringList("ALERTS.ALWAYS-SHOW");
        if (alwaysShow == null || alwaysShow.isEmpty()) {
            alwaysShow = DEFAULT_ALWAYS_SHOW;
        }
        if (contains(alwaysShow, key)) {
            return false;
        }

        List<String> toggleable = this.mainConfig.getConfiguration().getStringList("ALERTS.TOGGLEABLE");
        if (toggleable == null || toggleable.isEmpty()) {
            toggleable = DEFAULT_TOGGLEABLE;
        }
        return contains(toggleable, "*") || contains(toggleable, key);
    }

    /**
     * The toggleable list actually in use, and whether it came from config.yml or the defaults.
     */
    public List<String> getToggleableMessages() {
        List<String> toggleable = this.mainConfig.getConfiguration().getStringList("ALERTS.TOGGLEABLE");
        return toggleable == null || toggleable.isEmpty() ? DEFAULT_TOGGLEABLE : toggleable;
    }

    public List<String> getAlwaysShownMessages() {
        List<String> always = this.mainConfig.getConfiguration().getStringList("ALERTS.ALWAYS-SHOW");
        return always == null || always.isEmpty() ? DEFAULT_ALWAYS_SHOW : always;
    }

    public boolean wouldSilence(CommandSender sender, String key) {
        return isSilenced(sender, key);
    }

    private boolean contains(List<String> list, String value) {
        for (String entry : list) {
            if (entry != null && entry.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Plays a sound written as "sound", "sound|volume" or "sound|volume|pitch".
     */
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
                volume = Float.parseFloat(parts[1].trim());
            }
            if (parts.length > 2) {
                pitch = Float.parseFloat(parts[2].trim());
            }
        } catch (NumberFormatException exception) {
            getLogger().warning("Bad sound volume/pitch in " + path + ": " + raw);
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * "ON" / "OFF" for placeholders and settings menus.
     */
    public String getAlertStatus(Player player) {
        boolean enabled = this.profileManager.getProfile(player).isAlerts();
        return CC.translate(enabled
                ? this.mainConfig.getString("ALERTS.STATUS-ENABLED", "&aON")
                : this.mainConfig.getString("ALERTS.STATUS-DISABLED", "&cOFF"));
    }
}
