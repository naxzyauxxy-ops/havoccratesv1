/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.Arrays
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

import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CratesCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> SUB_COMMANDS = Arrays.asList(new String[]{"create", "delete", "set", "unset", "edit", "open", "list", "reload", "debug"});
    private final CratesPlugin plugin;

    public CratesCommand(CratesPlugin plugin) {
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

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList completions;
        block5: {
            block6: {
                block4: {
                    completions = new ArrayList();
                    if (!sender.hasPermission("havoccrates.admin")) {
                        return completions;
                    }
                    if (args.length != 1) break block4;
                    for (String sub : SUB_COMMANDS) {
                        if (!sub.startsWith(args[0].toLowerCase(Locale.ROOT))) continue;
                        completions.add(sub);
                    }
                    break block5;
                }
                if (args.length != 2) break block6;
                for (String crate : this.plugin.getCrateManager().getCrateNames()) {
                    if (!crate.startsWith(args[1].toLowerCase(Locale.ROOT))) continue;
                    completions.add(crate);
                }
                break block5;
            }
            if (args.length != 3 || !args[0].equalsIgnoreCase("open")) break block5;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) continue;
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
