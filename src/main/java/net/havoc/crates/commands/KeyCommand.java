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

    /*
     * Exception decompiling
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter$TooOptimisticMatchException
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:1802596)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:4489513)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:4491475)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:3820062)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:3820183)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:1024810)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:4466968)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:604767)
         *     at java.lang.Throwable$FakeClass.fakeMethod(Throwable.java:4234497)
         */
        throw new IllegalStateException("Decompilation failed");
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
