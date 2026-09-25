/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.CharSequence
 *  java.lang.Integer
 *  java.lang.Math
 *  java.lang.Object
 *  java.lang.Override
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.HashMap
 *  java.util.List
 *  java.util.Map
 *  java.util.Map$Entry
 *  org.bukkit.Material
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.ui;

import java.lang.CharSequence;
import java.lang.Integer;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.ui.CrateConfirmMenu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.ItemBuilder;
import net.havoc.crates.util.Restrictions;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CrateViewMenu
extends Menu {
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
            title = this.config().getString("CRATE-VIEW-MENU.TITLE", "%crate%");
        }
        return CC.translate(title.replace((CharSequence)"%crate%", (CharSequence)this.crate.getName()));
    }

    @Override
    public int getSize(Player player) {
        return this.crate.getSize();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<Integer, Button>();
        int size = this.getSize(player);
        if (this.config().getBoolean("CRATE-VIEW-MENU.PLACEHOLDER", true)) {
            ItemStack filler = ItemBuilder.of(this.config().getString("CRATE-VIEW-MENU.PLACEHOLDER-MATERIAL", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE).name(this.config().getString("CRATE-VIEW-MENU.PLACEHOLDER-NAME", " ")).build();
            for (int slot = 0; slot < size; ++slot) {
                buttons.put(slot, new FillerButton(filler));
            }
        }
        java.util.Set<Integer> used = new java.util.HashSet<Integer>();
        // A present crate keeps its rewards a surprise: only the present is shown, so nobody can
        // pick the item they want straight out of the menu.
        boolean hideRewards = this.hasPresent() && this.config().getBoolean("PRESENT.HIDE-REWARDS", true);
        if (!hideRewards) {
            for (Map.Entry entry : this.crate.resolveSlots().entrySet()) {
                int slot = (Integer)entry.getKey();
                ItemStack item = (ItemStack)this.crate.getRewards().get(entry.getValue());
                if (slot < 0 || slot >= size || item == null) continue;
                used.add(Integer.valueOf(slot));
                buttons.put(slot, new CrateItemButton((Integer)entry.getValue(), item));
            }
        }
        int presentSlot = hideRewards ? this.centerSlot(size) : this.presentSlot(size, used);
        if (presentSlot >= 0) {
            buttons.put(Integer.valueOf(presentSlot), new PresentButton());
        }
        return buttons;
    }

    /**
     * True when this crate is one of the crates listed under PRESENT.CRATES.
     */
    private boolean hasPresent() {
        if (!this.config().getBoolean("PRESENT.ENABLED", true)) {
            return false;
        }
        List<String> crates = this.config().getStringList("PRESENT.CRATES");
        if (crates == null || crates.isEmpty()) {
            return false;
        }
        for (String name : crates) {
            if (name == null) continue;
            if (name.equalsIgnoreCase("ALL") || name.equalsIgnoreCase(this.crate.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Middle of the menu, used when the rewards are hidden and the present stands alone.
     */
    private int centerSlot(int size) {
        if (!this.hasPresent()) {
            return -1;
        }
        int configured = this.config().getInt("PRESENT.SLOT", -1);
        if (configured >= 0) {
            return configured < size ? configured : -1;
        }
        int rows = Math.max(1, size / 9);
        return (rows / 2) * 9 + 4;
    }

    /**
     * Where the mystery present sits: PRESENT.SLOT, or centered on the first row below the
     * rewards when it is -1. Returns -1 when the present is disabled for this crate.
     */
    private int presentSlot(int size, java.util.Set<Integer> used) {
        if (!this.hasPresent()) {
            return -1;
        }
        int configured = this.config().getInt("PRESENT.SLOT", -1);
        if (configured >= 0) {
            return configured < size ? configured : -1;
        }
        // Auto: middle of the last row that has no rewards in it.
        for (int row = size / 9 - 1; row >= 0; --row) {
            boolean free = true;
            for (int column = 0; column < 9; ++column) {
                if (!used.contains(Integer.valueOf(row * 9 + column))) continue;
                free = false;
                break;
            }
            if (free) {
                return row * 9 + 4;
            }
        }
        // Every row holds rewards - fall back to any free slot.
        for (int slot = size - 1; slot >= 0; --slot) {
            if (!used.contains(Integer.valueOf(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * The mystery present: one key, one random reward from this crate.
     */
    private class PresentButton
    extends Button {
        private PresentButton() {
        }

        private ItemStack icon(Player player) {
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int stack = Math.max(1, Math.min(
                    CrateViewMenu.this.config().getInt("PRESENT.MAX-AMOUNT",
                            CrateViewMenu.this.config().getInt("CONFIRM-MENU.MAX-AMOUNT", 64)),
                    Math.max(1, keys)));
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : CrateViewMenu.this.config().getStringList("PRESENT.LORE")) {
                if (line.contains("%chances%")) {
                    if (CrateViewMenu.this.config().getBoolean("PRESENT.SHOW-CHANCES", true)) {
                        lore.addAll(net.havoc.crates.ui.CrateConfirmMenu.chanceLines(
                                CrateViewMenu.this.plugin, CrateViewMenu.this.crate));
                    }
                    continue;
                }
                lore.add(line.replace("%keys%", String.valueOf(keys))
                        .replace("%stack%", String.valueOf(stack))
                        .replace("%rewards%", String.valueOf(CrateViewMenu.this.crate.getRewards().size()))
                        .replace("%crate%", CrateViewMenu.this.crate.getName()));
            }
            return ItemBuilder.of(CrateViewMenu.this.config().getString("PRESENT.MATERIAL", "PINK_SHULKER_BOX"),
                            Material.PINK_SHULKER_BOX)
                    .name(CrateViewMenu.this.config().getString("PRESENT.NAME", "&c\u2744 &aMystery Present &c\u2744")
                            .replace("%crate%", CrateViewMenu.this.crate.getName()))
                    .lore(lore)
                    .hideAttributes()
                    .build();
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return this.icon(player);
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int keys = CrateViewMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int max = Math.max(1, CrateViewMenu.this.config().getInt("PRESENT.MAX-AMOUNT",
                    CrateViewMenu.this.config().getInt("CONFIRM-MENU.MAX-AMOUNT", 64)));
            int amount = clickType.isRightClick() ? Math.max(1, Math.min(max, Math.max(1, keys))) : 1;
            new CrateConfirmMenu(CrateViewMenu.this.plugin, CrateViewMenu.this.crate,
                    this.icon(player), amount).openMenu(player);
        }
    }

    private static class FillerButton
    extends Button {
        private final ItemStack itemStack;

        FillerButton(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return this.itemStack.clone();
        }
    }

    private class CrateItemButton
    extends Button {
        private final int rewardKey;
        private final ItemStack itemStack;

        CrateItemButton(int rewardKey, ItemStack itemStack) {
            this.rewardKey = rewardKey;
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> configured = CrateViewMenu.this.config().getStringList("CRATE-VIEW-MENU.ITEM-LORE");
            if (configured == null || configured.isEmpty()) {
                return this.itemStack.clone();
            }
            int keys = CrateViewMenu.this.plugin.getProfileManager().getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int stack = this.stackAmount(player);
            int bundle = Math.max((int)1, (int)this.itemStack.getAmount());
            ArrayList<String> lore = new ArrayList<String>();
            for (String line : configured) {
                if (stack <= 1 && (line.contains((CharSequence)"%stack%") || line.contains((CharSequence)"%items%"))) continue;
                lore.add(line.replace((CharSequence)"%keys%", (CharSequence)String.valueOf((int)keys)).replace((CharSequence)"%stack%", (CharSequence)String.valueOf((int)stack)).replace((CharSequence)"%each%", (CharSequence)String.valueOf((int)bundle)).replace((CharSequence)"%items%", (CharSequence)String.valueOf((int)(stack * bundle))).replace((CharSequence)"%one%", (CharSequence)String.valueOf((int)bundle)).replace((CharSequence)"%crate%", (CharSequence)CrateViewMenu.this.crate.getName()));
            }
            return new ItemBuilder(this.itemStack).appendLore((List<String>)lore).build();
        }

        private int stackAmount(Player player) {
            int min;
            int max;
            Restrictions.Rule rule = Restrictions.resolve(CrateViewMenu.this.plugin, this.itemStack.getType());
            int keys = CrateViewMenu.this.plugin.getProfileManager().getProfile(player).getKeyAmount(CrateViewMenu.this.crate.getKey());
            int bundle = Math.max((int)1, (int)this.itemStack.getAmount());
            boolean inItems = CrateViewMenu.this.plugin.getMainConfig().getBoolean("CONFIRM-MENU.RESTRICTIONS-IN-ITEMS", true);
            int globalMax = Math.max((int)1, (int)CrateViewMenu.this.plugin.getMainConfig().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
            if (rule.getMaxPurchases() > 0) {
                max = Math.min((int)globalMax, (int)rule.getMaxPurchases());
                min = 1;
            } else if (inItems) {
                max = Math.max((int)1, (int)Math.min((int)globalMax, (int)(rule.getMax() / bundle)));
                min = Math.max((int)1, (int)((int)Math.ceil((double)((double)rule.getMin() / (double)bundle))));
            } else {
                max = Math.min((int)globalMax, (int)rule.getMax());
                min = rule.getMin();
            }
            int stackSize = Math.max((int)1, (int)this.itemStack.getMaxStackSize());
            int wanted = stackSize > 1 ? Math.max((int)1, (int)(stackSize / bundle)) : max;
            int amount = Math.min((int)wanted, (int)max);
            if (keys > 0) {
                amount = Math.min((int)amount, (int)keys);
            }
            return Math.max((int)min, (int)amount);
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int amount = clickType.isRightClick() ? this.stackAmount(player) : 1;
            new CrateConfirmMenu(CrateViewMenu.this.plugin, CrateViewMenu.this.crate, this.rewardKey, this.itemStack, amount).openMenu(player);
        }
    }
}
