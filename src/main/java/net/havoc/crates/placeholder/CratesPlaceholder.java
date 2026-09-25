/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  java.lang.String
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 */
package net.havoc.crates.placeholder;

import java.lang.Object;
import java.lang.String;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import org.bukkit.OfflinePlayer;

public class CratesPlaceholder
extends PlaceholderExpansion {
    private final CratesPlugin plugin;

    public CratesPlaceholder(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "havoccrates";
    }

    public String getAuthor() {
        return "HavocCrates";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "0";
        }
        Profile profile = this.plugin.getProfileManager().getProfile(player.getUniqueId());
        if (params.equalsIgnoreCase("alerts_status") || params.equalsIgnoreCase("alerts_state")) {
            return player.isOnline() && player.getPlayer() != null ? this.plugin.getAlertStatus(player.getPlayer()) : String.valueOf((boolean)profile.isAlerts());
        }
        if (params.equalsIgnoreCase("alerts")) {
            return String.valueOf((boolean)profile.isAlerts());
        }
        if (params.equalsIgnoreCase("keys_total")) {
            return String.valueOf((int)profile.getTotalKeys());
        }
        if (params.toLowerCase().startsWith("keys_")) {
            return String.valueOf((int)profile.getKeyAmount(params.substring("keys_".length())));
        }
        return null;
    }
}
