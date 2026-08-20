package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.ItemBuilder;
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
 * Shows every reward inside a crate.
 *
 * <p>Left click buys one, right click jumps straight to a full stack (capped by the keys you own).
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
        return CC.translate(config().getString("CRATE-VIEW-MENU.TITLE", "%crate%")
                .replace("%crate%", this.crate.getDisplayName()));
    }

    @Override
    public int getSize(Player player) {
        return this.crate.getMenuSize();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int size = getSize(player);

        if (config().getBoolean("CRATE-VIEW-MENU.PLACEHOLDER", true)) {
            ItemStack filler = ItemBuilder
                    .of(config().getString("CRATE-VIEW-MENU.PLACEHOLDER-MATERIAL", "GRAY_STAINED_GLASS_PANE"),
                            Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build();
            for (int slot = 0; slot < size; slot++) {
                buttons.put(slot, new FillerButton(filler));
            }
        }

        for (int slot = 0; slot < size; slot++) {
            ItemStack item = this.crate.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            buttons.put(slot, new CrateItemButton(slot, item));
        }
        return buttons;
    }

    private class FillerButton extends Button {

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

        private final int slot;
        private final ItemStack itemStack;

        CrateItemButton(int slot, ItemStack itemStack) {
            this.slot = slot;
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> lore = new ArrayList<>();
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getName());
            for (String line : config().getStringList("CRATE-VIEW-MENU.ITEM-LORE")) {
                lore.add(line
                        .replace("%keys%", String.valueOf(keys))
                        .replace("%stack%", String.valueOf(stackAmount(player)))
                        .replace("%crate%", CrateViewMenu.this.crate.getDisplayName()));
            }
            if (lore.isEmpty()) {
                return this.itemStack.clone();
            }
            return new ItemBuilder(this.itemStack).appendLore(lore).build();
        }

        /**
         * A full stack of this item, never more than the keys the player actually has.
         */
        private int stackAmount(Player player) {
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getName());
            int max = Math.max(1, config().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
            int stackSize = Math.max(1, this.itemStack.getMaxStackSize());
            // Armour and other unstackable rewards still buy a full "stack" of 64 pieces.
            int wanted = stackSize > 1 ? stackSize : max;
            return Math.max(1, Math.min(Math.min(wanted, max), Math.max(1, keys)));
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int amount = clickType.isRightClick() ? stackAmount(player) : 1;
            new CrateConfirmMenu(CrateViewMenu.this.plugin, CrateViewMenu.this.crate,
                    this.slot, this.itemStack, amount).openMenu(player);
        }
    }
}
