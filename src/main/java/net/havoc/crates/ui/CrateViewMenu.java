package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.ItemBuilder;
import net.havoc.crates.util.Restrictions;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows a crate's rewards using the crate's own TITLE and ROWS, with the same centered
 * alignment as before (7 rewards land on 10-16 of a 3 row menu, 5 on 11-15, and so on).
 *
 * <p>Left click buys one, right click jumps to a full stack, capped by the item's RESTRICTIONS
 * limit and by the keys owned.
 */
public class CrateViewMenu extends Menu {

    private final CratesPlugin plugin;
    private final Crate crate;

    public CrateViewMenu(CratesPlugin plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
    }

    private FileConfiguration config() {
        return this.plugin.getMainConfig().getConfiguration();
    }

    @Override
    public String getTitle(Player player) {
        String title = this.crate.getTitle();
        if (title == null || title.isEmpty()) {
            title = config().getString("CRATE-VIEW-MENU.TITLE", "%crate%");
        }
        return CC.translate(title.replace("%crate%", this.crate.getName()));
    }

    @Override
    public int getSize(Player player) {
        return this.crate.getSize();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int size = getSize(player);

        if (config().getBoolean("CRATE-VIEW-MENU.PLACEHOLDER", true)) {
            ItemStack filler = ItemBuilder
                    .of(config().getString("CRATE-VIEW-MENU.PLACEHOLDER-MATERIAL", "GRAY_STAINED_GLASS_PANE"),
                            Material.GRAY_STAINED_GLASS_PANE)
                    .name(config().getString("CRATE-VIEW-MENU.PLACEHOLDER-NAME", " "))
                    .build();
            for (int slot = 0; slot < size; slot++) {
                buttons.put(slot, new FillerButton(filler));
            }
        }

        for (Map.Entry<Integer, Integer> entry : this.crate.resolveSlots().entrySet()) {
            int slot = entry.getKey();
            ItemStack item = this.crate.getRewards().get(entry.getValue());
            if (slot < 0 || slot >= size || item == null) {
                continue;
            }
            buttons.put(slot, new CrateItemButton(entry.getValue(), item));
        }
        return buttons;
    }

    private static class FillerButton extends Button {

        private final ItemStack itemStack;

        FillerButton(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return this.itemStack.clone();
        }
    }

    private class CrateItemButton extends Button {

        private final int rewardKey;
        private final ItemStack itemStack;

        CrateItemButton(int rewardKey, ItemStack itemStack) {
            this.rewardKey = rewardKey;
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> configured = config().getStringList("CRATE-VIEW-MENU.ITEM-LORE");
            if (configured == null || configured.isEmpty()) {
                return this.itemStack.clone();
            }
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int stack = stackAmount(player);
            List<String> lore = new ArrayList<>();
            for (String line : configured) {
                if (stack <= 1 && line.contains("%stack%")) {
                    // Single purchase items (totems, shulkers) have nothing to bulk buy.
                    continue;
                }
                lore.add(line
                        .replace("%keys%", String.valueOf(keys))
                        .replace("%stack%", String.valueOf(stack))
                        .replace("%crate%", CrateViewMenu.this.crate.getName()));
            }
            return new ItemBuilder(this.itemStack).appendLore(lore).build();
        }

        /**
         * A full stack of this reward, limited by RESTRICTIONS and by the keys owned.
         */
        private int stackAmount(Player player) {
            Restrictions.Rule rule = Restrictions.resolve(CrateViewMenu.this.plugin, this.itemStack.getType());
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int stackSize = Math.max(1, this.itemStack.getMaxStackSize());
            // Armour and other unstackable rewards still buy a full 64 unless restricted.
            int wanted = stackSize > 1 ? stackSize : rule.getMax();
            int amount = Math.min(wanted, rule.getMax());
            if (keys > 0) {
                amount = Math.min(amount, keys);
            }
            return Math.max(rule.getMin(), amount);
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int amount = clickType.isRightClick() ? stackAmount(player) : 1;
            new CrateConfirmMenu(CrateViewMenu.this.plugin, CrateViewMenu.this.crate,
                    this.rewardKey, this.itemStack, amount).openMenu(player);
        }
    }
}
