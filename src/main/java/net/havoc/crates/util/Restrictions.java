package net.havoc.crates.util;

import net.havoc.crates.CratesPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Per material purchase limits, read from the RESTRICTIONS section:
 *
 * <pre>
 * RESTRICTIONS:
 *   TOTEM_OF_UNDYING:
 *     MAX_QUANTITY: 1
 *     MIN_QUANTITY: 1
 *     HIDE_QUANTITY_BUTTONS: true
 *   DEFAULT:
 *     MAX_QUANTITY: 64
 *     MIN_QUANTITY: 1
 * </pre>
 *
 * <p>A key matches a material exactly, or as a suffix, so SHULKER_BOX also covers
 * PINK_SHULKER_BOX. The most specific (longest) match wins.
 */
public final class Restrictions {

    public static class Rule {

        private final int min;
        private final int max;
        private final int maxPurchases;
        private final boolean hideButtons;

        Rule(int min, int max, int maxPurchases, boolean hideButtons) {
            this.min = Math.max(1, min);
            this.max = Math.max(this.min, max);
            this.maxPurchases = maxPurchases;
            this.hideButtons = hideButtons;
        }

        /**
         * Optional hard cap on the number of purchases, ignoring stack maths. 0 = not set.
         */
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

    private Restrictions() {
    }

    private static ConfigurationSection section(CratesPlugin plugin) {
        ConfigurationSection root = plugin.getMainConfig().getConfiguration()
                .getConfigurationSection("RESTRICTIONS");
        if (root != null) {
            return root;
        }
        return plugin.getMainConfig().getConfiguration()
                .getConfigurationSection("CONFIRM-MENU.RESTRICTIONS");
    }

    public static Rule resolve(CratesPlugin plugin, Material material) {
        int globalMax = Math.max(1, plugin.getMainConfig().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
        ConfigurationSection section = section(plugin);
        if (section == null || material == null) {
            return new Rule(1, globalMax, 0, false);
        }

        String name = material.name().toUpperCase(Locale.ROOT);
        ConfigurationSection best = null;
        int bestTier = -1;
        int bestLength = -1;
        for (String key : section.getKeys(false)) {
            String upper = key.toUpperCase(Locale.ROOT);
            if (upper.equals("DEFAULT")) {
                continue;
            }
            // 2 = exact (SPLASH_POTION), 1 = word suffix (WHITE_SHULKER_BOX -> SHULKER_BOX),
            // 0 = plain suffix (CROSSBOW -> BOW, DIAMOND_PICKAXE -> AXE).
            int tier = -1;
            if (name.equals(upper)) {
                tier = 2;
            } else if (name.endsWith("_" + upper)) {
                tier = 1;
            } else if (name.endsWith(upper)) {
                tier = 0;
            }
            if (tier < 0) {
                continue;
            }
            if (tier > bestTier || (tier == bestTier && upper.length() > bestLength)) {
                ConfigurationSection candidate = section.getConfigurationSection(key);
                if (candidate != null) {
                    best = candidate;
                    bestTier = tier;
                    bestLength = upper.length();
                }
            }
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
        boolean hide = best.getBoolean("HIDE_QUANTITY_BUTTONS",
                best.getBoolean("HIDE-QUANTITY-BUTTONS", false));
        return new Rule(min, max, Math.max(0, maxPurchases), hide);
    }
}
