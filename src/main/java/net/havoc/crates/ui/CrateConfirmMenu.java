package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.InventoryUtil;
import net.havoc.crates.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The purchase confirmation GUI.
 *
 * <p>Layout (defaults, all configurable):
 * <ul>
 *     <li>3 red buttons: -1 / -10 / -64</li>
 *     <li>the reward in the middle, rendered as the stack you are about to buy</li>
 *     <li>3 green buttons: +1 / +10 / +64</li>
 *     <li>cancel, key counter and confirm on the bottom row</li>
 * </ul>
 * Unstackable rewards (armour, tools) are delivered as separate items, so buying 64 chestplates
 * fills the inventory instead of giving a single piece.
 */
public class CrateConfirmMenu extends Menu {

    private final CratesPlugin plugin;
    private final Crate crate;
    private final int rewardSlot;
    private final ItemStack reward;
    private int amount;

    public CrateConfirmMenu(CratesPlugin plugin, Crate crate, int rewardSlot, ItemStack reward, int amount) {
        this.plugin = plugin;
        this.crate = crate;
        this.rewardSlot = rewardSlot;
        this.reward = reward.clone();
        this.reward.setAmount(1);
        this.amount = Math.max(1, amount);
    }

    private FileConfiguration config() {
        return this.plugin.getMainConfig().getConfiguration();
    }

    /**
     * Highest amount the player is allowed to select: capped by MAX-AMOUNT and by their keys.
     */
    public int getMaxSelectable(Player player) {
        int configured = Math.max(1, config().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
        int keys = this.plugin.getProfileManager().getProfile(player).getKeyAmount(this.crate.getName());
        return Math.max(1, Math.min(configured, Math.max(1, keys)));
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(Player player, int amount) {
        this.amount = Math.max(1, Math.min(getMaxSelectable(player), amount));
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate(config().getString("CONFIRM-MENU.TITLE", "&8Confirm")
                .replace("%crate%", CC.strip(this.crate.getDisplayName()))
                .replace("%amount%", String.valueOf(this.amount)));
    }

    @Override
    public int getSize(Player player) {
        int size = config().getInt("CONFIRM-MENU.SIZE", 27);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 27;
        }
        return size;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int size = getSize(player);

        if (config().getBoolean("CONFIRM-MENU.FILLER.ENABLED", true)) {
            ItemStack filler = ItemBuilder
                    .of(config().getString("CONFIRM-MENU.FILLER.MATERIAL", "GRAY_STAINED_GLASS_PANE"),
                            Material.GRAY_STAINED_GLASS_PANE)
                    .name(config().getString("CONFIRM-MENU.FILLER.NAME", " "))
                    .build();
            for (int slot = 0; slot < size; slot++) {
                buttons.put(slot, new StaticButton(filler));
            }
        }

        // The reward preview, stacked to the selected amount.
        int itemSlot = config().getInt("CONFIRM-MENU.ITEM-SLOT", 13);
        buttons.put(itemSlot, new RewardDisplayButton());

        // Green add buttons (3 slots by default: +1, +10, +64).
        List<Integer> addValues = intList("CONFIRM-MENU.AMOUNTS.ADD.VALUES", Arrays.asList(1, 10, 64));
        List<Integer> addSlots = intList("CONFIRM-MENU.AMOUNTS.ADD.SLOTS", Arrays.asList(15, 16, 17));
        for (int index = 0; index < Math.min(addValues.size(), addSlots.size()); index++) {
            buttons.put(addSlots.get(index), new AmountButton(addValues.get(index), true));
        }

        // Red remove buttons (3 slots by default: -1, -10, -64).
        List<Integer> removeValues = intList("CONFIRM-MENU.AMOUNTS.REMOVE.VALUES", Arrays.asList(1, 10, 64));
        List<Integer> removeSlots = intList("CONFIRM-MENU.AMOUNTS.REMOVE.SLOTS", Arrays.asList(11, 10, 9));
        for (int index = 0; index < Math.min(removeValues.size(), removeSlots.size()); index++) {
            buttons.put(removeSlots.get(index), new AmountButton(removeValues.get(index), false));
        }

        buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.CONFIRM.SLOT", 26), new ConfirmButton());
        buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.CANCEL.SLOT", 18), new CancelButton());
        if (config().getBoolean("CONFIRM-MENU.BUTTONS.INFO.ENABLED", true)) {
            buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.INFO.SLOT", 22), new InfoButton());
        }

        buttons.keySet().removeIf(slot -> slot < 0 || slot >= size);
        return buttons;
    }

    private List<Integer> intList(String path, List<Integer> def) {
        List<Integer> list = config().getIntegerList(path);
        return list == null || list.isEmpty() ? def : list;
    }

    private List<String> replaceLore(List<String> lore, Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        List<String> out = new ArrayList<>();
        for (String line : lore) {
            out.add(line
                    .replace("%amount%", String.valueOf(this.amount))
                    .replace("%cost%", String.valueOf(this.amount))
                    .replace("%keys%", String.valueOf(profile.getKeyAmount(this.crate.getName())))
                    .replace("%crate%", this.crate.getDisplayName())
                    .replace("%max%", String.valueOf(getMaxSelectable(player))));
        }
        return out;
    }

    /**
     * Takes the keys and hands over the items.
     */
    private void purchase(Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        int cost = this.amount;
        if (profile.getKeyAmount(this.crate.getName()) < cost) {
            this.plugin.message(player, "NOT_ENOUGH_KEYS",
                    "%crate%", this.crate.getDisplayName(),
                    "%amount%", String.valueOf(cost));
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        List<ItemStack> stacks = InventoryUtil.split(this.reward, this.amount);
        boolean dropOverflow = config().getBoolean("CONFIRM-MENU.DROP-OVERFLOW", true);
        if (!dropOverflow && !InventoryUtil.fits(player, stacks)) {
            this.plugin.message(player, "INVENTORY_FULL");
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        if (!profile.takeKeys(this.crate.getName(), cost)) {
            this.plugin.message(player, "NOT_ENOUGH_KEYS",
                    "%crate%", this.crate.getDisplayName(),
                    "%amount%", String.valueOf(cost));
            return;
        }
        this.plugin.getProfileManager().saveAsync(profile);

        List<ItemStack> leftovers = InventoryUtil.give(player, stacks, dropOverflow);
        if (!leftovers.isEmpty()) {
            // Only reachable when overflow dropping is off and the inventory filled up mid-delivery.
            for (ItemStack leftover : leftovers) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        runCommands(player);

        this.plugin.message(player, "REWARD_RECEIVED",
                "%amount%", String.valueOf(this.amount),
                "%crate%", this.crate.getDisplayName(),
                "%item%", itemName());
        player.playSound(player.getLocation(),
                config().getString("CONFIRM-MENU.SOUND", "entity.player.levelup"), 1.0f, 1.0f);

        int remainingKeys = profile.getKeyAmount(this.crate.getName());
        if (remainingKeys <= 0) {
            player.closeInventory();
            return;
        }
        setAmount(player, this.amount);
        update(player);
    }

    private void runCommands(Player player) {
        List<String> commands = this.crate.getCommands(this.rewardSlot);
        if (commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            String command = raw
                    .replace("%player%", player.getName())
                    .replace("%crate%", this.crate.getName());
            if (raw.contains("%amount%")) {
                // The command handles the bulk itself.
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("%amount%", String.valueOf(this.amount)));
            } else {
                for (int index = 0; index < this.amount; index++) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
    }

    private String itemName() {
        if (this.reward.hasItemMeta() && this.reward.getItemMeta() != null
                && this.reward.getItemMeta().hasDisplayName()) {
            return this.reward.getItemMeta().getDisplayName();
        }
        return this.reward.getType().name().toLowerCase().replace('_', ' ');
    }

    /**
     * Non-interactive decoration.
     */
    private static class StaticButton extends Button {

        private final ItemStack itemStack;

        StaticButton(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return this.itemStack.clone();
        }
    }

    /**
     * The reward, rendered as the stack the player is buying.
     */
    private class RewardDisplayButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            ItemStack display = CrateConfirmMenu.this.reward.clone();
            // Visual stack size: the client can only render up to 64.
            display.setAmount(Math.max(1, Math.min(64, CrateConfirmMenu.this.amount)));
            return new ItemBuilder(display)
                    .appendLore(CC.translate(replaceLore(
                            config().getStringList("CONFIRM-MENU.ITEM-LORE"), player)))
                    .build();
        }
    }

    /**
     * A green (+) or red (-) amount button.
     */
    private class AmountButton extends Button {

        private final int value;
        private final boolean add;

        AmountButton(int value, boolean add) {
            this.value = value;
            this.add = add;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            String base = this.add ? "CONFIRM-MENU.AMOUNTS.ADD" : "CONFIRM-MENU.AMOUNTS.REMOVE";
            String material = config().getString(base + ".MATERIAL",
                    this.add ? "LIME_STAINED_GLASS_PANE" : "RED_STAINED_GLASS_PANE");
            String name = config().getString(base + ".NAME", this.add ? "&a&l+%value%" : "&c&l-%value%");
            List<String> lore = replaceLore(config().getStringList(base + ".LORE"), player);
            return ItemBuilder
                    .of(material, this.add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                    .amount(Math.max(1, Math.min(64, this.value)))
                    .name(name.replace("%value%", String.valueOf(this.value)))
                    .lore(lore)
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int max = getMaxSelectable(player);
            if (clickType.isShiftClick()) {
                setAmount(player, this.add ? max : 1);
            } else {
                setAmount(player, this.add
                        ? CrateConfirmMenu.this.amount + this.value
                        : CrateConfirmMenu.this.amount - this.value);
            }
            player.playSound(player.getLocation(), "ui.button.click", 0.6f, this.add ? 1.4f : 0.8f);
        }

        @Override
        public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
            return true;
        }
    }

    private class ConfirmButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder
                    .of(config().getString("CONFIRM-MENU.BUTTONS.CONFIRM.MATERIAL", "LIME_STAINED_GLASS_PANE"),
                            Material.LIME_STAINED_GLASS_PANE)
                    .name(config().getString("CONFIRM-MENU.BUTTONS.CONFIRM.NAME", "&a&lCONFIRM")
                            .replace("%amount%", String.valueOf(CrateConfirmMenu.this.amount)))
                    .lore(replaceLore(config().getStringList("CONFIRM-MENU.BUTTONS.CONFIRM.LORE"), player))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            purchase(player);
        }
    }

    private class CancelButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder
                    .of(config().getString("CONFIRM-MENU.BUTTONS.CANCEL.MATERIAL", "RED_STAINED_GLASS_PANE"),
                            Material.RED_STAINED_GLASS_PANE)
                    .name(config().getString("CONFIRM-MENU.BUTTONS.CANCEL.NAME", "&c&lCANCEL"))
                    .lore(replaceLore(config().getStringList("CONFIRM-MENU.BUTTONS.CANCEL.LORE"), player))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            new CrateViewMenu(CrateConfirmMenu.this.plugin, CrateConfirmMenu.this.crate).openMenu(player);
        }
    }

    private class InfoButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder
                    .of(config().getString("CONFIRM-MENU.BUTTONS.INFO.MATERIAL", "TRIPWIRE_HOOK"),
                            Material.TRIPWIRE_HOOK)
                    .name(config().getString("CONFIRM-MENU.BUTTONS.INFO.NAME", "&eYour keys: &f%keys%")
                            .replace("%keys%", String.valueOf(CrateConfirmMenu.this.plugin.getProfileManager()
                                    .getProfile(player).getKeyAmount(CrateConfirmMenu.this.crate.getName()))))
                    .lore(replaceLore(config().getStringList("CONFIRM-MENU.BUTTONS.INFO.LORE"), player))
                    .hideAttributes()
                    .build();
        }
    }
}
