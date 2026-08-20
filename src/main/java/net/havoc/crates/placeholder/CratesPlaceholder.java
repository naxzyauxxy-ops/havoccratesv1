package net.havoc.crates.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI hook.
 *
 * <p>%havoccrates_keys_&lt;crate&gt;% and %havoccrates_keys_total%
 */
public class CratesPlaceholder extends PlaceholderExpansion {

    private final CratesPlugin plugin;

    public CratesPlaceholder(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "havoccrates";
    }

    @Override
    public String getAuthor() {
        return "HavocCrates";
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "0";
        }
        Profile profile = this.plugin.getProfileManager().getProfile(player.getUniqueId());
        if (params.equalsIgnoreCase("keys_total")) {
            return String.valueOf(profile.getTotalKeys());
        }
        if (params.toLowerCase().startsWith("keys_")) {
            return String.valueOf(profile.getKeyAmount(params.substring("keys_".length())));
        }
        return null;
    }
}
