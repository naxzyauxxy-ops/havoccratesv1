package net.havoc.crates.commands;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /key - virtual key management.
 */
public class KeyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("give", "giveall", "remove", "set", "check");

    private final CratesPlugin plugin;

    public KeyCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("check")) {
            Player target;
            if (args.length >= 2 && sender.hasPermission("havoccrates.admin")) {
                target = Bukkit.getPlayerExact(args[1]);
            } else {
                target = sender instanceof Player ? (Player) sender : null;
            }
            if (target == null) {
                this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args.length >= 2 ? args[1] : "");
                return true;
            }
            Profile profile = this.plugin.getProfileManager().getProfile(target);
            sender.sendMessage(CC.translate("&7Keys of &f" + target.getName() + "&7:"));
            if (profile.getKeys().isEmpty()) {
                sender.sendMessage(CC.translate("&8- &cnone"));
                return true;
            }
            for (Map.Entry<String, Integer> entry : profile.getKeys().entrySet()) {
                sender.sendMessage(CC.translate("&8- &f" + entry.getKey() + "&7: &f" + entry.getValue()));
            }
            return true;
        }

        if (!sender.hasPermission("havoccrates.admin")) {
            this.plugin.message(sender, "NO_PERMISSION");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("giveall")) {
            if (args.length < 3) {
                sender.sendMessage(CC.translate("&cUsage: /" + label + " giveall <crate> <amount>"));
                return true;
            }
            Crate crate = this.plugin.getCrateManager().getCrate(args[1]);
            if (crate == null) {
                this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[1]);
                return true;
            }
            int amount = parseAmount(sender, args[2]);
            if (amount <= 0) {
                return true;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                Profile profile = this.plugin.getProfileManager().getProfile(online);
                profile.addKeys(crate.getName(), amount);
                this.plugin.getProfileManager().saveAsync(profile);
                this.plugin.message(online, "RECEIVED_KEYS",
                        "%amount%", String.valueOf(amount), "%crate%", crate.getDisplayName());
            }
            this.plugin.message(sender, "KEYALL_GIVEN",
                    "%amount%", String.valueOf(amount), "%crate%", crate.getDisplayName());
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(CC.translate("&cUsage: /" + label + " " + sub + " <player> <crate> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args[1]);
            return true;
        }
        Crate crate = this.plugin.getCrateManager().getCrate(args[2]);
        if (crate == null) {
            this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[2]);
            return true;
        }
        int amount = parseAmount(sender, args[3]);
        if (amount < 0) {
            return true;
        }
        Profile profile = this.plugin.getProfileManager().getProfile(target);
        switch (sub) {
            case "give":
                profile.addKeys(crate.getName(), amount);
                this.plugin.message(sender, "GIVEN_KEYS", "%amount%", String.valueOf(amount),
                        "%crate%", crate.getDisplayName(), "%player%", target.getName());
                this.plugin.message(target, "RECEIVED_KEYS", "%amount%", String.valueOf(amount),
                        "%crate%", crate.getDisplayName());
                break;
            case "remove":
                profile.setKeys(crate.getName(), Math.max(0, profile.getKeyAmount(crate.getName()) - amount));
                this.plugin.message(sender, "KEYS_REMOVED_ADMIN", "%amount%", String.valueOf(amount),
                        "%crate%", crate.getDisplayName(), "%player%", target.getName());
                break;
            case "set":
                profile.setKeys(crate.getName(), amount);
                this.plugin.message(sender, "KEYS_SET", "%amount%", String.valueOf(amount),
                        "%crate%", crate.getDisplayName(), "%player%", target.getName());
                break;
            default:
                sender.sendMessage(CC.translate("&cUnknown sub command."));
                return true;
        }
        this.plugin.getProfileManager().saveAsync(profile);
        return true;
    }

    private int parseAmount(CommandSender sender, String raw) {
        try {
            int amount = Integer.parseInt(raw);
            if (amount < 0) {
                sender.sendMessage(CC.translate("&cThe amount has to be positive."));
                return -1;
            }
            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage(CC.translate("&c'" + raw + "' is not a number."));
            return -1;
        }
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
            if (args[0].equalsIgnoreCase("giveall")) {
                completions.addAll(this.plugin.getCrateManager().getCrateNames());
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("giveall")) {
            completions.addAll(this.plugin.getCrateManager().getCrateNames());
        } else if (args.length == 3 || args.length == 4) {
            completions.addAll(Arrays.asList("1", "10", "64"));
        }
        return completions;
    }
}
