/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.NumberFormatException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.Arrays
 *  java.util.Collection
 *  java.util.List
 *  java.util.Locale
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package net.havoc.crates.commands;

import java.lang.Integer;
import java.lang.NumberFormatException;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class KeyCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> SUB_COMMANDS = Arrays.asList(new String[]{"give", "giveall", "remove", "set", "check"});
    private final CratesPlugin plugin;

    public KeyCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /key or /key check [player]
        if (args.length == 0 || args[0].equalsIgnoreCase("check")) {
            Player target = args.length >= 2 && sender.hasPermission("havoccrates.admin")
                    ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player ? (Player)sender : null);
            if (target == null) {
                this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args.length >= 2 ? args[1] : "");
                return true;
            }
            net.havoc.crates.data.Profile profile = this.plugin.getProfileManager().getProfile(target);
            sender.sendMessage(CC.translate("&7Keys of &f" + target.getName() + "&7:"));
            if (profile.getKeys().isEmpty()) {
                sender.sendMessage(CC.translate("&8- &cnone"));
                return true;
            }
            for (java.util.Map.Entry<String, Integer> entry : profile.getKeys().entrySet()) {
                sender.sendMessage(CC.translate("&8- &f" + entry.getKey() + "&7: &f" + entry.getValue()));
            }
            return true;
        }

        if (!sender.hasPermission("havoccrates.admin")) {
            this.plugin.message(sender, "NO_PERMISSION", new String[0]);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("giveall")) {
            if (args.length < 3) {
                sender.sendMessage(CC.translate("&cUsage: /" + label + " giveall <crate> <amount>"));
                return true;
            }
            net.havoc.crates.crate.Crate crate = this.plugin.getCrateManager().getCrate(args[1]);
            if (crate == null) {
                this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[1]);
                return true;
            }
            int amount = this.parseAmount(sender, args[2]);
            if (amount <= 0) {
                return true;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                net.havoc.crates.data.Profile profile = this.plugin.getProfileManager().getProfile(online);
                profile.addKeys(crate.getKey(), amount);
                this.plugin.getProfileManager().saveAsync(profile);
                this.plugin.message(online, "RECEIVED_KEYS",
                        "%amount%", String.valueOf(amount), "%crate%", crate.getName());
            }
            this.plugin.message(sender, "KEYALL_GIVEN",
                    "%amount%", String.valueOf(amount), "%crate%", crate.getName());
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
        net.havoc.crates.crate.Crate crate = this.plugin.getCrateManager().getCrate(args[2]);
        if (crate == null) {
            this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[2]);
            return true;
        }
        int amount = this.parseAmount(sender, args[3]);
        if (amount < 0) {
            return true;
        }
        net.havoc.crates.data.Profile profile = this.plugin.getProfileManager().getProfile(target);

        if (sub.equals("give")) {
            profile.addKeys(crate.getKey(), amount);
            this.plugin.message(sender, "GIVEN_KEYS", "%amount%", String.valueOf(amount),
                    "%crate%", crate.getName(), "%player%", target.getName());
            this.plugin.message(target, "RECEIVED_KEYS", "%amount%", String.valueOf(amount),
                    "%crate%", crate.getName());
        } else if (sub.equals("remove")) {
            profile.setKeys(crate.getKey(), Math.max(0, profile.getKeyAmount(crate.getKey()) - amount));
            this.plugin.message(sender, "KEYS_REMOVED_ADMIN", "%amount%", String.valueOf(amount),
                    "%crate%", crate.getName(), "%player%", target.getName());
        } else if (sub.equals("set")) {
            profile.setKeys(crate.getKey(), amount);
            this.plugin.message(sender, "KEYS_SET", "%amount%", String.valueOf(amount),
                    "%crate%", crate.getName(), "%player%", target.getName());
        } else {
            sender.sendMessage(CC.translate("&cUnknown sub command."));
            return true;
        }
        this.plugin.getProfileManager().saveAsync(profile);
        return true;
    }

    private int parseAmount(CommandSender sender, String raw) {
        try {
            int amount = Integer.parseInt((String)raw);
            if (amount < 0) {
                sender.sendMessage(CC.translate("&cThe amount has to be positive."));
                return -1;
            }
            return amount;
        }
        catch (NumberFormatException exception) {
            sender.sendMessage(CC.translate("&c'" + raw + "' is not a number."));
            return -1;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList completions = new ArrayList();
        if (!sender.hasPermission("havoccrates.admin")) {
            return completions;
        }
        if (args.length == 1) {
            for (String sub : SUB_COMMANDS) {
                if (!sub.startsWith(args[0].toLowerCase(Locale.ROOT))) continue;
                completions.add(sub);
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
            completions.addAll((Collection)Arrays.asList(new String[]{"1", "10", "64"}));
        }
        return completions;
    }
}
