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
        private final boolean hideButtons;

        Rule(int min, int max, boolean hideButtons) {
            this.min = Math.max(1, min);
            this.max = Math.max(this.min, max);
            this.hideButtons = hideButtons;
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
            return new Rule(1, globalMax, false);
        }

        String name = material.name().toUpperCase(Locale.ROOT);
        ConfigurationSection best = null;
        int bestLength = -1;
        for (String key : section.getKeys(false)) {
            String upper = key.toUpperCase(Locale.ROOT);
            if (upper.equals("DEFAULT")) {
                continue;
            }
            boolean matches = name.equals(upper) || name.endsWith("_" + upper);
            if (matches && upper.length() > bestLength) {
                ConfigurationSection candidate = section.getConfigurationSection(key);
                if (candidate != null) {
                    best = candidate;
                    bestLength = upper.length();
                }
            }
        }
        if (best == null) {
            best = section.getConfigurationSection("DEFAULT");
        }
        if (best == null) {
            return new Rule(1, globalMax, false);
        }
        int min = best.getInt("MIN_QUANTITY", best.getInt("MIN-QUANTITY", 1));
        int max = best.getInt("MAX_QUANTITY", best.getInt("MAX-QUANTITY", globalMax));
        boolean hide = best.getBoolean("HIDE_QUANTITY_BUTTONS",
                best.getBoolean("HIDE-QUANTITY-BUTTONS", false));
        return new Rule(min, Math.min(max, globalMax), hide);
    }
}
