package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.InventoryUtil;
import net.havoc.crates.util.ItemBuilder;
import net.havoc.crates.util.Restrictions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The purchase confirmation GUI.
 *
 * <p>Quantity buttons come from CONFIRM-MENU.QUANTITY_ADJUST, e.g. green LIME_CONCRETE
 * "Add 1 / Add 10 / Set to 64" on 15/16/17 and red RED_CONCRETE "Remove 1 / 10 / 64" on 11/10/9.
 * Per material limits come from RESTRICTIONS, so a totem or a shulker box is locked to one and
 * its quantity buttons are hidden entirely.
 *
 * <p>Unstackable rewards are delivered as separate items, so buying 64 chestplates fills the
 * inventory instead of handing over a single piece.
 */
public class CrateConfirmMenu extends Menu {

    private final CratesPlugin plugin;
    private final Crate crate;
    private final int rewardKey;
    private final ItemStack reward;
    private final Restrictions.Rule rule;
    private int amount;

    public CrateConfirmMenu(CratesPlugin plugin, Crate crate, int rewardKey, ItemStack reward, int amount) {
        this.plugin = plugin;
        this.crate = crate;
        this.rewardKey = rewardKey;
        this.reward = reward.clone();
        this.reward.setAmount(1);
        this.rule = Restrictions.resolve(plugin, this.reward.getType());
        this.amount = clamp(amount);
    }

    private FileConfiguration config() {
        return this.plugin.getMainConfig().getConfiguration();
    }

    private int clamp(int value) {
        return Math.max(this.rule.getMin(), Math.min(this.rule.getMax(), value));
    }

    /**
     * Highest amount that can be selected: the material's limit, capped by the keys owned.
     */
    public int getMaxSelectable(Player player) {
        int keys = this.plugin.getProfileManager().getProfile(player).getKeyAmount(this.crate.getKey());
        return Math.max(this.rule.getMin(), Math.min(this.rule.getMax(), Math.max(this.rule.getMin(), keys)));
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(Player player, int amount) {
        this.amount = Math.max(this.rule.getMin(), Math.min(getMaxSelectable(player), amount));
    }

    @Override
    public String getTitle(Player player) {
        return CC.translate(config().getString("CONFIRM-MENU.TITLE", "&8ᴄᴏɴꜰɪʀᴍ")
                .replace("%crate%", this.crate.getName())
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

        buttons.put(config().getInt("CONFIRM-MENU.ITEM-SLOT", 13), new RewardDisplayButton());

        // Quantity buttons, hidden for one-per-purchase items such as totems and shulker boxes.
        if (!this.rule.isHideButtons()) {
            buttons.putAll(quantityButtons("ADD"));
            buttons.putAll(quantityButtons("REMOVE"));
        }

        buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.CONFIRM.SLOT", 26), new ConfirmButton());
        buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.CANCEL.SLOT", 18), new CancelButton());
        if (config().getBoolean("CONFIRM-MENU.BUTTONS.INFO.ENABLED", true)) {
            buttons.put(config().getInt("CONFIRM-MENU.BUTTONS.INFO.SLOT", 22), new InfoButton());
        }

        buttons.keySet().removeIf(slot -> slot < 0 || slot >= size);
        return buttons;
    }

    /**
     * Reads one QUANTITY_ADJUST group (ADD or REMOVE). Falls back to the older AMOUNTS layout.
     */
    private Map<Integer, Button> quantityButtons(String group) {
        Map<Integer, Button> buttons = new HashMap<>();
        boolean add = group.equals("ADD");
        ConfigurationSection section = config()
                .getConfigurationSection("CONFIRM-MENU.QUANTITY_ADJUST." + group);
        if (section == null) {
            section = config().getConfigurationSection("QUANTITY_ADJUST." + group);
        }

        if (section != null) {
            String groupMaterial = section.getString("MATERIAL", add ? "LIME_CONCRETE" : "RED_CONCRETE");
            List<String> groupLore = section.getStringList("LORE");
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                int slot = entry.getInt("SLOT", -1);
                if (slot < 0) {
                    continue;
                }
                int value = entry.getInt("INCREMENT",
                        entry.getInt("DECREMENT", entry.getInt("AMOUNT", entry.getInt("VALUE", 1))));
                String material = entry.getString("MATERIAL", groupMaterial);
                String name = entry.getString("NAME", (add ? "&a+" : "&c-") + value);
                List<String> lore = entry.getStringList("LORE");
                if (lore == null || lore.isEmpty()) {
                    lore = groupLore;
                }
                Mode mode = Mode.of(entry.getString("MODE"), key, add);
                buttons.put(slot, new QuantityButton(mode, value, material, name, lore,
                        add ? Material.LIME_CONCRETE : Material.RED_CONCRETE));
            }
            return buttons;
        }

        // Legacy AMOUNTS layout: VALUES + SLOTS lists.
        String base = "CONFIRM-MENU.AMOUNTS." + group;
        List<Integer> values = config().getIntegerList(base + ".VALUES");
        List<Integer> slots = config().getIntegerList(base + ".SLOTS");
        if (values.isEmpty() || slots.isEmpty()) {
            values = Arrays.asList(1, 10, 64);
            slots = add ? Arrays.asList(15, 16, 17) : Arrays.asList(11, 10, 9);
        }
        String material = config().getString(base + ".MATERIAL",
                add ? "LIME_STAINED_GLASS_PANE" : "RED_STAINED_GLASS_PANE");
        String name = config().getString(base + ".NAME", add ? "&a&l+%value%" : "&c&l-%value%");
        List<String> lore = config().getStringList(base + ".LORE");
        for (int index = 0; index < Math.min(values.size(), slots.size()); index++) {
            buttons.put(slots.get(index), new QuantityButton(add ? Mode.ADD : Mode.SUBTRACT,
                    values.get(index), material, name, lore,
                    add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE));
        }
        return buttons;
    }

    private List<String> replaceLore(List<String> lore, Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        List<String> out = new ArrayList<>();
        if (lore == null) {
            return out;
        }
        for (String line : lore) {
            out.add(line
                    .replace("%amount%", String.valueOf(this.amount))
                    .replace("%cost%", String.valueOf(this.amount))
                    .replace("%keys%", String.valueOf(profile.getKeyAmount(this.crate.getKey())))
                    .replace("%crate%", this.crate.getName())
                    .replace("%min%", String.valueOf(this.rule.getMin()))
                    .replace("%max%", String.valueOf(getMaxSelectable(player))));
        }
        return out;
    }

    private void purchase(Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        int cost = this.amount;
        if (profile.getKeyAmount(this.crate.getKey()) < cost) {
            this.plugin.message(player, "NOT_ENOUGH_KEYS",
                    "%crate%", this.crate.getName(), "%amount%", String.valueOf(cost));
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

        if (!profile.takeKeys(this.crate.getKey(), cost)) {
            this.plugin.message(player, "NOT_ENOUGH_KEYS",
                    "%crate%", this.crate.getName(), "%amount%", String.valueOf(cost));
            return;
        }
        this.plugin.getProfileManager().saveAsync(profile);

        List<ItemStack> leftovers = InventoryUtil.give(player, stacks, dropOverflow);
        for (ItemStack leftover : leftovers) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        runCommands(player);

        this.plugin.message(player, "REWARD_RECEIVED",
                "%amount%", String.valueOf(this.amount),
                "%crate%", this.crate.getName(),
                "%item%", itemName());
        player.playSound(player.getLocation(),
                config().getString("CONFIRM-MENU.SOUND", "entity.player.levelup"), 1.0f, 1.0f);

        if (profile.getKeyAmount(this.crate.getKey()) <= 0) {
            player.closeInventory();
            return;
        }
        setAmount(player, this.amount);
        update(player);
    }

    /**
     * Runs the reward's commands. Supports both {player} and %player% style placeholders.
     */
    private void runCommands(Player player) {
        List<String> commands = this.crate.getCommands(this.rewardKey);
        if (commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String command = raw
                    .replace("{player}", player.getName()).replace("%player%", player.getName())
                    .replace("{crate}", this.crate.getName()).replace("%crate%", this.crate.getName());
            boolean bulkAware = raw.contains("{amount}") || raw.contains("%amount%");
            if (bulkAware) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                        .replace("{amount}", String.valueOf(this.amount))
                        .replace("%amount%", String.valueOf(this.amount)));
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
        return this.reward.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /**
     * What a quantity button does when clicked.
     */
    private enum Mode {
        ADD, SUBTRACT, SET, MAX, MIN;

        static Mode of(String configured, String key, boolean addGroup) {
            String source = configured != null ? configured : key;
            String upper = source.toUpperCase(Locale.ROOT);
            if (upper.startsWith("SET")) {
                return SET;
            }
            if (upper.startsWith("MAX")) {
                return MAX;
            }
            if (upper.startsWith("MIN") || upper.startsWith("RESET")) {
                return MIN;
            }
            if (upper.startsWith("REMOVE") || upper.startsWith("SUB") || upper.startsWith("MINUS")
                    || upper.startsWith("TAKE")) {
                return SUBTRACT;
            }
            if (upper.startsWith("ADD") || upper.startsWith("PLUS") || upper.startsWith("INCREASE")) {
                return ADD;
            }
            return addGroup ? ADD : SUBTRACT;
        }
    }

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
     * The reward, rendered as the stack the player is about to buy.
     */
    private class RewardDisplayButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            ItemStack display = CrateConfirmMenu.this.reward.clone();
            display.setAmount(Math.max(1, Math.min(64, CrateConfirmMenu.this.amount)));
            List<String> lore = replaceLore(config().getStringList("CONFIRM-MENU.ITEM-LORE"), player);
            return lore.isEmpty() ? display : new ItemBuilder(display).appendLore(lore).build();
        }
    }

    private class QuantityButton extends Button {

        private final Mode mode;
        private final int value;
        private final String material;
        private final String name;
        private final List<String> lore;
        private final Material fallback;

        QuantityButton(Mode mode, int value, String material, String name, List<String> lore,
                       Material fallback) {
            this.mode = mode;
            this.value = value;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.fallback = fallback;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder.of(this.material, this.fallback)
                    .amount(Math.max(1, Math.min(64, this.value)))
                    .name(this.name
                            .replace("%value%", String.valueOf(this.value))
                            .replace("%amount%", String.valueOf(CrateConfirmMenu.this.amount)))
                    .lore(replaceLore(this.lore, player))
                    .hideAttributes()
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int max = getMaxSelectable(player);
            int min = CrateConfirmMenu.this.rule.getMin();
            int current = CrateConfirmMenu.this.amount;
            switch (this.mode) {
                case SET:
                    setAmount(player, this.value);
                    break;
                case MAX:
                    setAmount(player, max);
                    break;
                case MIN:
                    setAmount(player, min);
                    break;
                case SUBTRACT:
                    setAmount(player, clickType.isShiftClick() ? min : current - this.value);
                    break;
                case ADD:
                default:
                    setAmount(player, clickType.isShiftClick() ? max : current + this.value);
                    break;
            }
            boolean up = CrateConfirmMenu.this.amount >= current;
            player.playSound(player.getLocation(), "ui.button.click", 0.6f, up ? 1.4f : 0.8f);
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
                    .name(config().getString("CONFIRM-MENU.BUTTONS.CONFIRM.NAME", "&#00FC00ᴄᴏɴꜰɪʀᴍ")
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
                    .name(config().getString("CONFIRM-MENU.BUTTONS.CANCEL.NAME", "&#FC0000ᴄᴀɴᴄᴇʟ"))
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
            int keys = CrateConfirmMenu.this.plugin.getProfileManager()
                    .getProfile(player).getKeyAmount(CrateConfirmMenu.this.crate.getKey());
            return ItemBuilder
                    .of(config().getString("CONFIRM-MENU.BUTTONS.INFO.MATERIAL", "TRIPWIRE_HOOK"),
                            Material.TRIPWIRE_HOOK)
                    .name(config().getString("CONFIRM-MENU.BUTTONS.INFO.NAME", "&eYour keys: &f%keys%")
                            .replace("%keys%", String.valueOf(keys))
                            .replace("%amount%", String.valueOf(CrateConfirmMenu.this.amount)))
                    .lore(replaceLore(config().getStringList("CONFIRM-MENU.BUTTONS.INFO.LORE"), player))
                    .hideAttributes()
                    .build();
        }
    }
}
