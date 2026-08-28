package net.havoc.crates.commands;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.ui.CrateEditMenu;
import net.havoc.crates.ui.CrateViewMenu;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /crates - crate administration.
 */
public class CratesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS =
            Arrays.asList("create", "delete", "set", "unset", "edit", "open", "list", "reload", "debug");

    private final CratesPlugin plugin;

    public CratesCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("havoccrates.admin")) {
            this.plugin.message(sender, "NO_PERMISSION");
            return true;
        }
        if (args.length == 0) {
            usage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create": {
                if (args.length < 2) {
                    sender.sendMessage(CC.translate("&cUsage: /" + label + " create <crate>"));
                    return true;
                }
                if (this.plugin.getCrateManager().getCrate(args[1]) != null) {
                    this.plugin.message(sender, "CRATE_ALREADY_EXISTS", "%crate%", args[1]);
                    return true;
                }
                this.plugin.getCrateManager().createCrate(args[1]);
                this.plugin.message(sender, "CRATE_CREATED", "%crate%", args[1]);
                return true;
            }
            case "delete": {
                Crate crate = requireCrate(sender, args, label, "delete");
                if (crate == null) {
                    return true;
                }
                this.plugin.getCrateManager().deleteCrate(crate);
                this.plugin.message(sender, "CRATE_DELETED", "%crate%", crate.getName());
                return true;
            }
            case "set": {
                if (!(sender instanceof Player)) {
                    this.plugin.message(sender, "PLAYERS_ONLY");
                    return true;
                }
                Crate crate = requireCrate(sender, args, label, "set");
                if (crate == null) {
                    return true;
                }
                Block block = ((Player) sender).getTargetBlockExact(6);
                if (block == null || block.getType().isAir()) {
                    this.plugin.message(sender, "NO_TARGET_BLOCK");
                    return true;
                }
                crate.addLocation(block.getLocation());
                this.plugin.getCrateManager().save();
                this.plugin.message(sender, "CRATE_SET", "%crate%", crate.getDisplayName());
                return true;
            }
            case "unset": {
                if (!(sender instanceof Player)) {
                    this.plugin.message(sender, "PLAYERS_ONLY");
                    return true;
                }
                Block block = ((Player) sender).getTargetBlockExact(6);
                if (block == null) {
                    this.plugin.message(sender, "NO_TARGET_BLOCK");
                    return true;
                }
                Crate crate = this.plugin.getCrateManager().getCrate(block.getLocation());
                if (crate == null) {
                    this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", "this block");
                    return true;
                }
                crate.removeLocation(block.getLocation());
                this.plugin.getCrateManager().save();
                this.plugin.message(sender, "CRATE_UNSET", "%crate%", crate.getDisplayName());
                return true;
            }
            case "edit": {
                if (!(sender instanceof Player)) {
                    this.plugin.message(sender, "PLAYERS_ONLY");
                    return true;
                }
                Crate crate = requireCrate(sender, args, label, "edit");
                if (crate == null) {
                    return true;
                }
                new CrateEditMenu(this.plugin, crate).open((Player) sender);
                return true;
            }
            case "open": {
                Crate crate = requireCrate(sender, args, label, "open");
                if (crate == null) {
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                } else {
                    target = sender instanceof Player ? (Player) sender : null;
                }
                if (target == null) {
                    this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args.length >= 3 ? args[2] : "");
                    return true;
                }
                new CrateViewMenu(this.plugin, crate).openMenu(target);
                return true;
            }
            case "list": {
                List<String> names = this.plugin.getCrateManager().getCrateNames();
                if (names.isEmpty()) {
                    this.plugin.message(sender, "NO_CRATES_FOUND");
                    return true;
                }
                sender.sendMessage(CC.translate("&7Crates &8(&f" + names.size() + "&8)&7: &f"
                        + String.join("&7, &f", names)));
                return true;
            }
            case "debug": {
                Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1])
                        : (sender instanceof Player ? (Player) sender : null);
                sender.sendMessage(CC.translate("&8&m------------------------------"));
                sender.sendMessage(CC.translate("&cHavocCrates &7v"
                        + this.plugin.getDescription().getVersion()));
                sender.sendMessage(CC.translate("&7Data folder: &f"
                        + this.plugin.getDataFolder().getAbsolutePath()));
                sender.sendMessage(CC.translate("&7Storage: &f"
                        + this.plugin.getProfileManager().getStorageName()));
                sender.sendMessage(CC.translate("&7Crates loaded: &f"
                        + this.plugin.getCrateManager().getCrates().size()));
                boolean fromConfig = this.plugin.getMainConfig().getConfiguration().contains("ALERTS");
                sender.sendMessage(CC.translate("&7ALERTS section in config.yml: &f" + fromConfig
                        + (fromConfig ? "" : " &8(using built-in defaults)")));
                sender.sendMessage(CC.translate("&7Silenceable: &f"
                        + String.join(", ", this.plugin.getToggleableMessages())));
                if (target == null) {
                    sender.sendMessage(CC.translate("&8&m------------------------------"));
                    return true;
                }
                Profile memory = this.plugin.getProfileManager().getProfile(target);
                Profile disk = this.plugin.getProfileManager().readFromStorage(target.getUniqueId());
                sender.sendMessage(CC.translate("&7Player: &f" + target.getName()));
                sender.sendMessage(CC.translate("&7  alerts in memory: &f" + memory.isAlerts()));
                sender.sendMessage(CC.translate("&7  alerts on disk: &f" + disk.isAlerts()));
                sender.sendMessage(CC.translate("&7  keys in memory: &f"
                        + (memory.getKeys().isEmpty() ? "none" : memory.getKeys().toString())));
                sender.sendMessage(CC.translate("&7  keys on disk: &f"
                        + (disk.getKeys().isEmpty() ? "none" : disk.getKeys().toString())));
                sender.sendMessage(CC.translate("&7  REWARD_RECEIVED would be: &f"
                        + (this.plugin.wouldSilence(target, "REWARD_RECEIVED") ? "SILENCED" : "SHOWN")));
                sender.sendMessage(CC.translate("&8&m------------------------------"));
                return true;
            }
            case "reload": {
                this.plugin.reloadAll();
                this.plugin.message(sender, "CONFIG_RELOADED");
                return true;
            }
            default:
                usage(sender, label);
                return true;
        }
    }

    private Crate requireCrate(CommandSender sender, String[] args, String label, String sub) {
        if (args.length < 2) {
            sender.sendMessage(CC.translate("&cUsage: /" + label + " " + sub + " <crate>"));
            return null;
        }
        Crate crate = this.plugin.getCrateManager().getCrate(args[1]);
        if (crate == null) {
            this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[1]);
            return null;
        }
        return crate;
    }

    private void usage(CommandSender sender, String label) {
        sender.sendMessage(CC.translate("&8&m--------------------------"));
        sender.sendMessage(CC.translate("&7/" + label + " create <crate>"));
        sender.sendMessage(CC.translate("&7/" + label + " delete <crate>"));
        sender.sendMessage(CC.translate("&7/" + label + " set <crate> &8- bind the block you look at"));
        sender.sendMessage(CC.translate("&7/" + label + " unset"));
        sender.sendMessage(CC.translate("&7/" + label + " edit <crate> &8- edit the rewards"));
        sender.sendMessage(CC.translate("&7/" + label + " open <crate> [player]"));
        sender.sendMessage(CC.translate("&7/" + label + " list"));
        sender.sendMessage(CC.translate("&7/" + label + " reload"));
        sender.sendMessage(CC.translate("&7/" + label + " debug [player] &8- version, storage and alert state"));
        sender.sendMessage(CC.translate("&8&m--------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("havoccrates.admin")) {
            return completions;
        }
        if (args.length == 1) {
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            for (String crate : this.plugin.getCrateManager().getCrateNames()) {
                if (crate.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    completions.add(crate);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
