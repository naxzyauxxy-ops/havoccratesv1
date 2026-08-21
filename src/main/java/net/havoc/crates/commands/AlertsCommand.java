package net.havoc.crates.commands;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
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
 * /cratealerts [on|off] - silences or restores the crate messages for the player running it.
 */
public class AlertsCommand implements CommandExecutor, TabCompleter {

    private final CratesPlugin plugin;

    public AlertsCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            this.plugin.message(sender, "PLAYERS_ONLY");
            return true;
        }
        Player player = (Player) sender;
        Profile profile = this.plugin.getProfileManager().getProfile(player);

        boolean enabled;
        if (args.length == 0) {
            enabled = !profile.isAlerts();
        } else {
            String value = args[0].toLowerCase(Locale.ROOT);
            if (value.equals("on") || value.equals("enable") || value.equals("true")) {
                enabled = true;
            } else if (value.equals("off") || value.equals("disable") || value.equals("false")) {
                enabled = false;
            } else {
                enabled = !profile.isAlerts();
            }
        }

        profile.setAlerts(enabled);
        this.plugin.getProfileManager().saveAsync(profile);
        // Sent with the raw sender so turning the messages back on is always confirmed.
        this.plugin.messageAlways(player, enabled ? "ALERTS_ENABLED" : "ALERTS_DISABLED");
        player.playSound(player.getLocation(), "ui.button.click", 0.7f, enabled ? 1.4f : 0.8f);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String option : Arrays.asList("on", "off")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(option);
                }
            }
        }
        return completions;
    }
}
