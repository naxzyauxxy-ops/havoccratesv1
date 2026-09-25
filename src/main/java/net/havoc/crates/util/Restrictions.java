/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Math
 *  java.lang.Object
 *  java.lang.String
 *  java.util.Locale
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 */
package net.havoc.crates.util;

import java.lang.Math;
import java.lang.Object;
import java.lang.String;
import java.util.Locale;
import net.havoc.crates.CratesPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public final class Restrictions {
    private Restrictions() {
    }

    private static ConfigurationSection section(CratesPlugin plugin) {
        ConfigurationSection root = plugin.getMainConfig().getConfiguration().getConfigurationSection("RESTRICTIONS");
        if (root != null) {
            return root;
        }
        return plugin.getMainConfig().getConfiguration().getConfigurationSection("CONFIRM-MENU.RESTRICTIONS");
    }

    public static Rule resolve(CratesPlugin plugin, Material material) {
        int globalMax = Math.max((int)1, (int)plugin.getMainConfig().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
        ConfigurationSection section = Restrictions.section(plugin);
        if (section == null || material == null) {
            return new Rule(1, globalMax, 0, false);
        }
        String name = material.name().toUpperCase(Locale.ROOT);
        ConfigurationSection best = null;
        int bestTier = -1;
        int bestLength = -1;
        for (String key : section.getKeys(false)) {
            ConfigurationSection candidate;
            String upper = key.toUpperCase(Locale.ROOT);
            if (upper.equals("DEFAULT")) continue;
            int tier = -1;
            if (name.equals(upper)) {
                tier = 2;
            } else if (name.endsWith("_" + upper)) {
                tier = 1;
            } else if (name.endsWith(upper)) {
                tier = 0;
            }
            if (tier < 0 || tier <= bestTier && (tier != bestTier || upper.length() <= bestLength) || (candidate = section.getConfigurationSection(key)) == null) continue;
            best = candidate;
            bestTier = tier;
            bestLength = upper.length();
        }
        if (best == null) {
            best = section.getConfigurationSection("DEFAULT");
        }
        if (best == null) {
            return new Rule(1, globalMax, 0, false);
        }
        int min = best.getInt("MIN_QUANTITY", best.getInt("MIN-QUANTITY", 1));
        int max = best.getInt("MAX_QUANTITY", best.getInt("MAX-QUANTITY", globalMax));
        int maxPurchases = best.getInt("MAX_PURCHASES", best.getInt("MAX-PURCHASES", 0));
        boolean hide = best.getBoolean("HIDE_QUANTITY_BUTTONS", best.getBoolean("HIDE-QUANTITY-BUTTONS", false));
        return new Rule(min, max, Math.max((int)0, (int)maxPurchases), hide);
    }

    /**
     * A rule with no material restriction, used by the mystery present.
     */
    public static Rule unlimited(int max) {
        return new Rule(1, Math.max(1, max), 0, false);
    }

    public static class Rule {
        private final int min;
        private final int max;
        private final int maxPurchases;
        private final boolean hideButtons;

        Rule(int min, int max, int maxPurchases, boolean hideButtons) {
            this.min = Math.max((int)1, (int)min);
            this.max = Math.max((int)this.min, (int)max);
            this.maxPurchases = maxPurchases;
            this.hideButtons = hideButtons;
        }

        public int getMaxPurchases() {
            return this.maxPurchases;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        public boolean isHideButtons() {
            return this.hideButtons || this.max <= this.min;
        }
    }
}
