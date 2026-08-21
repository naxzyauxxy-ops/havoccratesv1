package net.havoc.crates.commands;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /crate [crate] - opens a crate menu from anywhere in the world.
 *
 * <p>/crate on its own lists the crates and how many keys the player holds for each.
 * Admins can also do /crate &lt;crate&gt; &lt;player&gt; to open it for someone else.
 */
public class CrateCommand implements CommandExecutor, TabCompleter {

    private final CratesPlugin plugin;

    public CrateCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("havoccrates.crate")) {
            this.plugin.message(sender, "NO_PERMISSION");
            return true;
        }

        if (args.length == 0) {
            sendList(sender, label);
            return true;
        }

        Crate crate = this.plugin.getCrateManager().getCrate(args[0]);
        if (crate == null) {
            this.plugin.message(sender, "NO_CRATE_FOUND", "%crate%", args[0]);
            return true;
        }

        // /crate <crate> <player> for admins.
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("havoccrates.admin")) {
                this.plugin.message(sender, "NO_PERMISSION");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                this.plugin.message(sender, "PLAYER_NOT_FOUND", "%player%", args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            this.plugin.message(sender, "PLAYERS_ONLY");
            return true;
        }

        if (!canOpen(sender, target, crate)) {
            return true;
        }

        new CrateViewMenu(this.plugin, crate).openMenu(target);
        target.playSound(target.getLocation(),
                this.plugin.getMainConfig().getString("CRATE-COMMAND.SOUND", "block.chest.open"), 1.0f, 1.0f);
        if (!target.equals(sender)) {
            this.plugin.message(sender, "CRATE_OPENED_FOR",
                    "%crate%", crate.getName(), "%player%", target.getName());
        }
        return true;
    }

    /**
     * Optional gates: a per crate permission and a "you need a key first" check.
     */
    private boolean canOpen(CommandSender sender, Player target, Crate crate) {
        boolean perCrate = this.plugin.getMainConfig()
                .getBoolean("CRATE-COMMAND.PER-CRATE-PERMISSION", false);
        if (perCrate && !target.hasPermission("havoccrates.crate." + crate.getKey())
                && !target.hasPermission("havoccrates.admin")) {
            this.plugin.message(sender, "NO_PERMISSION");
            return false;
        }

        boolean requireKeys = this.plugin.getMainConfig()
                .getBoolean("CRATE-COMMAND.REQUIRE-KEYS", false);
        if (requireKeys && !target.hasPermission("havoccrates.admin")) {
            Profile profile = this.plugin.getProfileManager().getProfile(target);
            if (profile.getKeyAmount(crate.getKey()) <= 0) {
                this.plugin.message(sender, "NOT_ENOUGH_KEYS",
                        "%crate%", crate.getName(), "%amount%", "1");
                return false;
            }
        }
        return true;
    }

    private void sendList(CommandSender sender, String label) {
        List<Crate> crates = new ArrayList<>(this.plugin.getCrateManager().getCrates().values());
        if (crates.isEmpty()) {
            this.plugin.message(sender, "NO_CRATES_FOUND");
            return;
        }
        String header = this.plugin.getMainConfig().getConfiguration()
                .getString("MESSAGES.CRATE_LIST_HEADER", "&cCrates &8(&#FF5555%amount%&8)");
        sender.sendMessage(CC.translate(header.replace("%amount%", String.valueOf(crates.size()))));

        String entry = this.plugin.getMainConfig().getConfiguration().getString("MESSAGES.CRATE_LIST_ENTRY",
                "&8 ▪ &#FF5555%crate% &8- &7keys: &c%keys% &8(&7/%label% %crate%&8)");
        for (Crate crate : crates) {
            int keys = sender instanceof Player
                    ? this.plugin.getProfileManager().getProfile((Player) sender).getKeyAmount(crate.getKey())
                    : 0;
            sender.sendMessage(CC.translate(entry
                    .replace("%crate%", crate.getName())
                    .replace("%keys%", String.valueOf(keys))
                    .replace("%label%", label)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("havoccrates.crate")) {
            return completions;
        }
        if (args.length == 1) {
            for (String crate : this.plugin.getCrateManager().getCrateNames()) {
                if (crate.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(crate);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("havoccrates.admin")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
