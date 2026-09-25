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
import net.havoc.crates.data.Profile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class AlertsCommand
implements CommandExecutor,
TabCompleter {
    private final CratesPlugin plugin;

    public AlertsCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String value;
        if (!(sender instanceof Player)) {
            this.plugin.message(sender, "PLAYERS_ONLY", new String[0]);
            return true;
        }
        Player player = (Player)sender;
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        boolean enabled = args.length == 0 ? !profile.isAlerts() : ((value = args[0].toLowerCase(Locale.ROOT)).equals("on") || value.equals("enable") || value.equals("true") ? true : (value.equals("off") || value.equals("disable") || value.equals("false") ? false : !profile.isAlerts()));
        profile.setAlerts(enabled);
        this.plugin.getProfileManager().saveAsync(profile);
        this.plugin.messageAlways((CommandSender)player, enabled ? "ALERTS_ENABLED" : "ALERTS_DISABLED", new String[0]);
        this.plugin.playSound(player, "SOUNDS.BUTTON-CLICK", "minecraft:block.bubble_column.bubble_pop|0.8|1.2");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList completions = new ArrayList();
        if (args.length == 1) {
            for (String option : Arrays.asList(new String[]{"on", "off"})) {
                if (!option.startsWith(args[0].toLowerCase(Locale.ROOT))) continue;
                completions.add(option);
            }
        }
        return completions;
    }
}
