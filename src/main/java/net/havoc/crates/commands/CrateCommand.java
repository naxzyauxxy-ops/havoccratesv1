/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.CharSequence
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
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

import java.lang.CharSequence;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.ui.CrateViewMenu;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CrateCommand
implements CommandExecutor,
TabCompleter {
    private final CratesPlugin plugin;

    public CrateCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (!sender.hasPermission("havoccrates.crate")) {
            this.plugin.message(sender, "NO_PERMISSION", new String[0]);
            return true;
        }
        if (args.length == 0) {
            this.sendList(sender, label);
            return true;
        }
        Crate crate = this.plugin.getCrateManager().getCrate(args[0]);
        if (crate == null) {
            this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[0]);
            return true;
        }
        if (args.length >= 2) {
            if (!sender.hasPermission("havoccrates.admin")) {
                this.plugin.message(sender, "NO_PERMISSION", new String[0]);
                return true;
            }
            target = Bukkit.getPlayerExact((String)args[1]);
            if (target == null) {
                this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player)sender;
        } else {
            this.plugin.message(sender, "PLAYERS_ONLY", new String[0]);
            return true;
        }
        if (!this.canOpen(sender, target, crate)) {
            return true;
        }
        new CrateViewMenu(this.plugin, crate).openMenu(target);
        this.plugin.playSound(target, "CRATE-COMMAND.SOUND", "minecraft:block.chest.open|1.0|1.0");
        if (!target.equals(sender)) {
            this.plugin.message(sender, "CRATE_OPENED_FOR", "%crate%", crate.getName(), "%player%", target.getName());
        }
        return true;
    }

    private boolean canOpen(CommandSender sender, Player target, Crate crate) {
        Profile profile;
        boolean perCrate = this.plugin.getMainConfig().getBoolean("CRATE-COMMAND.PER-CRATE-PERMISSION", false);
        if (perCrate && !target.hasPermission("havoccrates.crate." + crate.getKey()) && !target.hasPermission("havoccrates.admin")) {
            this.plugin.message(sender, "NO_PERMISSION", new String[0]);
            return false;
        }
        boolean requireKeys = this.plugin.getMainConfig().getBoolean("CRATE-COMMAND.REQUIRE-KEYS", false);
        if (requireKeys && !target.hasPermission("havoccrates.admin") && (profile = this.plugin.getProfileManager().getProfile(target)).getKeyAmount(crate.getKey()) <= 0) {
            this.plugin.message(sender, "NOT_ENOUGH_KEYS", "%crate%", crate.getName(), "%amount%", "1");
            return false;
        }
        return true;
    }

    private void sendList(CommandSender sender, String label) {
        ArrayList<Crate> crates = new ArrayList<Crate>(this.plugin.getCrateManager().getCrates().values());
        if (crates.isEmpty()) {
            this.plugin.message(sender, "NO_CRATES_FOUND", new String[0]);
            return;
        }
        String header = this.plugin.getMainConfig().getConfiguration().getString("MESSAGES.CRATE_LIST_HEADER", "&cCrates &8(&#FF5555%amount%&8)");
        sender.sendMessage(CC.translate(header.replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)crates.size()))));
        String entry = this.plugin.getMainConfig().getConfiguration().getString("MESSAGES.CRATE_LIST_ENTRY", "&8 \u25aa &#FF5555%crate% &8- &7keys: &c%keys% &8(&7/%label% %crate%&8)");
        for (Crate crate : crates) {
            int keys = sender instanceof Player ? this.plugin.getProfileManager().getProfile((Player)sender).getKeyAmount(crate.getKey()) : 0;
            sender.sendMessage(CC.translate(entry.replace((CharSequence)"%crate%", (CharSequence)crate.getName()).replace((CharSequence)"%keys%", (CharSequence)String.valueOf((int)keys)).replace((CharSequence)"%label%", (CharSequence)label)));
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList completions;
        block4: {
            block3: {
                completions = new ArrayList();
                if (!sender.hasPermission("havoccrates.crate")) {
                    return completions;
                }
                if (args.length != 1) break block3;
                for (String crate : this.plugin.getCrateManager().getCrateNames()) {
                    if (!crate.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) continue;
                    completions.add(crate);
                }
                break block4;
            }
            if (args.length != 2 || !sender.hasPermission("havoccrates.admin")) break block4;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) continue;
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
