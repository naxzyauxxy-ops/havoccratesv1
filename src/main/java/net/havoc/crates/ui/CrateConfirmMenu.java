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
 *  java.util.Arrays
 *  java.util.HashMap
 *  java.util.Iterator
 *  java.util.List
 *  java.util.Locale
 *  java.util.Map
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.data.Profile;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.ui.CrateViewMenu;
import net.havoc.crates.util.CC;
import net.havoc.crates.util.InventoryUtil;
import net.havoc.crates.util.ItemBuilder;
import net.havoc.crates.util.Restrictions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CrateConfirmMenu
extends Menu {
    private final CratesPlugin plugin;
    private final Crate crate;
    private final int rewardKey;
    private final ItemStack reward;
    private final int bundle;
    private final Restrictions.Rule rule;
    private final int minAmount;
    private final int maxAmount;
    /** true when this menu is opening mystery presents instead of one fixed reward */
    private final boolean present;
    private int amount;

    /**
     * Opens the confirm menu for a mystery present: one key buys one random reward from the
     * crate, weighted by the CHANCES section in crates.yml.
     */
    public CrateConfirmMenu(CratesPlugin plugin, Crate crate, ItemStack presentIcon, int amount) {
        this.plugin = plugin;
        this.crate = crate;
        this.present = true;
        this.rewardKey = -1;
        this.bundle = 1;
        this.reward = presentIcon.clone();
        this.reward.setAmount(1);
        int presentMax = Math.max(1, plugin.getMainConfig().getInt("PRESENT.MAX-AMOUNT",
                plugin.getMainConfig().getInt("CONFIRM-MENU.MAX-AMOUNT", 64)));
        this.rule = Restrictions.unlimited(presentMax);
        this.minAmount = 1;
        this.maxAmount = presentMax;
        this.amount = this.clamp(amount);
    }

    public CrateConfirmMenu(CratesPlugin plugin, Crate crate, int rewardKey, ItemStack reward, int amount) {
        this.present = false;
        this.plugin = plugin;
        this.crate = crate;
        this.rewardKey = rewardKey;
        this.bundle = Math.max((int)1, (int)reward.getAmount());
        this.reward = reward.clone();
        this.reward.setAmount(1);
        this.rule = Restrictions.resolve(plugin, this.reward.getType());
        int globalMax = Math.max((int)1, (int)plugin.getMainConfig().getInt("CONFIRM-MENU.MAX-AMOUNT", 64));
        boolean inItems = plugin.getMainConfig().getBoolean("CONFIRM-MENU.RESTRICTIONS-IN-ITEMS", true);
        if (this.rule.getMaxPurchases() > 0) {
            this.maxAmount = Math.min((int)globalMax, (int)this.rule.getMaxPurchases());
            this.minAmount = 1;
        } else if (inItems) {
            this.maxAmount = Math.max((int)1, (int)Math.min((int)globalMax, (int)(this.rule.getMax() / this.bundle)));
            this.minAmount = Math.max((int)1, (int)((int)Math.ceil((double)((double)this.rule.getMin() / (double)this.bundle))));
        } else {
            this.maxAmount = Math.min((int)globalMax, (int)this.rule.getMax());
            this.minAmount = this.rule.getMin();
        }
        this.amount = this.clamp(amount);
    }

    public int getBundle() {
        return this.bundle;
    }

    private boolean hideQuantityButtons() {
        return this.rule.isHideButtons() || this.maxAmount <= this.minAmount;
    }

    private FileConfiguration config() {
        return this.plugin.getMainConfig().getConfiguration();
    }

    private int clamp(int value) {
        return Math.max((int)this.minAmount, (int)Math.min((int)this.maxAmount, (int)value));
    }

    public int getMaxSelectable(Player player) {
        int keys = this.plugin.getProfileManager().getProfile(player).getKeyAmount(this.crate.getKey());
        return Math.max((int)this.minAmount, (int)Math.min((int)this.maxAmount, (int)Math.max((int)this.minAmount, (int)keys)));
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(Player player, int amount) {
        this.amount = Math.max((int)this.minAmount, (int)Math.min((int)this.getMaxSelectable(player), (int)amount));
    }

    @Override
    public String getTitle(Player player) {
        if (this.present) {
            return CC.translate(this.config().getString("PRESENT.CONFIRM-TITLE",
                    this.config().getString("CONFIRM-MENU.TITLE", "&8Confirm"))
                    .replace("%crate%", this.crate.getName())
                    .replace("%amount%", String.valueOf(this.amount)));
        }
        return CC.translate(this.config().getString("CONFIRM-MENU.TITLE", "&8\u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d").replace((CharSequence)"%crate%", (CharSequence)this.crate.getName()).replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)this.amount)));
    }

    @Override
    public int getSize(Player player) {
        int size = this.config().getInt("CONFIRM-MENU.SIZE", 27);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 27;
        }
        return size;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        int slot2;
        HashMap<Integer, Button> buttons = new HashMap<Integer, Button>();
        int size = this.getSize(player);
        if (this.config().getBoolean("CONFIRM-MENU.FILLER.ENABLED", true)) {
            ItemStack filler = ItemBuilder.of(this.config().getString("CONFIRM-MENU.FILLER.MATERIAL", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE).name(this.config().getString("CONFIRM-MENU.FILLER.NAME", " ")).build();
            for (slot2 = 0; slot2 < size; ++slot2) {
                buttons.put(slot2, new StaticButton(filler));
            }
        }
        if (this.config().getBoolean("CONFIRM-MENU.BORDER.ENABLED", true) && size >= 27) {
            // MATERIALS cycles around the frame - two or more panes make a candy cane.
            List<String> borderMaterials = this.config().getStringList("CONFIRM-MENU.BORDER.MATERIALS");
            if (borderMaterials == null || borderMaterials.isEmpty()) {
                borderMaterials = Arrays.asList(new String[]{
                        this.config().getString("CONFIRM-MENU.BORDER.MATERIAL", "RED_STAINED_GLASS_PANE")});
            }
            String borderName = this.config().getString("CONFIRM-MENU.BORDER.NAME", " ");
            int rows = size / 9;
            int borderIndex = 0;
            for (int slot3 = 0; slot3 < size; ++slot3) {
                int row = slot3 / 9;
                int column = slot3 % 9;
                if (row != 0 && row != rows - 1 && column != 0 && column != 8) continue;
                ItemStack border = ItemBuilder.of(borderMaterials.get(borderIndex % borderMaterials.size()),
                        Material.RED_STAINED_GLASS_PANE).name(borderName).build();
                buttons.put(slot3, new StaticButton(border));
                ++borderIndex;
            }
        }
        buttons.put(this.config().getInt("CONFIRM-MENU.ITEM-SLOT", 22), new RewardDisplayButton());
        if (!this.hideQuantityButtons()) {
            buttons.putAll(this.quantityButtons("ADD", player));
            buttons.putAll(this.quantityButtons("REMOVE", player));
        }
        Iterator iterator = this.slotsOf("CONFIRM-MENU.BUTTONS.CONFIRM", (List<Integer>)Arrays.asList(new Integer[]{41, 42, 43})).iterator();
        while (iterator.hasNext()) {
            slot2 = (Integer)iterator.next();
            buttons.put(slot2, new ConfirmButton());
        }
        iterator = this.slotsOf("CONFIRM-MENU.BUTTONS.CANCEL", (List<Integer>)Arrays.asList(new Integer[]{37, 38, 39})).iterator();
        while (iterator.hasNext()) {
            slot2 = (Integer)iterator.next();
            buttons.put(slot2, new CancelButton());
        }
        if (this.config().getBoolean("CONFIRM-MENU.BUTTONS.INFO.ENABLED", true)) {
            iterator = this.slotsOf("CONFIRM-MENU.BUTTONS.INFO", (List<Integer>)Arrays.asList(new Integer[]{13})).iterator();
            while (iterator.hasNext()) {
                slot2 = (Integer)iterator.next();
                buttons.put(slot2, new InfoButton());
            }
        }
        buttons.keySet().removeIf(slot -> slot.intValue() < 0 || slot.intValue() >= size);
        return buttons;
    }

    private List<Integer> slotsOf(String path, List<Integer> def) {
        List<Integer> slots = this.config().getIntegerList(path + ".SLOTS");
        if (slots != null && !slots.isEmpty()) {
            return slots;
        }
        int single = this.config().getInt(path + ".SLOT", Integer.MIN_VALUE);
        return single == Integer.MIN_VALUE ? def : Arrays.asList(new Integer[]{single});
    }

    private Map<Integer, Button> quantityButtons(String group, Player player) {
        HashMap<Integer, Button> buttons = new HashMap<Integer, Button>();
        boolean hideUnusable = this.config().getBoolean("CONFIRM-MENU.HIDE-UNUSABLE-BUTTONS", true);
        boolean add = group.equals("ADD");
        ConfigurationSection section = this.config().getConfigurationSection("CONFIRM-MENU.QUANTITY_ADJUST." + group);
        if (section == null) {
            section = this.config().getConfigurationSection("QUANTITY_ADJUST." + group);
        }
        if (section != null) {
            String groupMaterial = section.getString("MATERIAL", add ? "LIME_CONCRETE" : "RED_CONCRETE");
            List groupLore = section.getStringList("LORE");
            for (String key : section.getKeys(false)) {
                int slot;
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null || (slot = entry.getInt("SLOT", -1)) < 0) continue;
                int value = entry.getInt("INCREMENT", entry.getInt("DECREMENT", entry.getInt("AMOUNT", entry.getInt("VALUE", 1))));
                String material = entry.getString("MATERIAL", groupMaterial);
                String name = entry.getString("NAME", (add ? "&a+" : "&c-") + value);
                List lore = entry.getStringList("LORE");
                if (lore == null || lore.isEmpty()) {
                    lore = groupLore;
                }
                Mode mode = Mode.of(entry.getString("MODE"), key, add);
                if (hideUnusable && !this.isUsable(mode, value, player)) continue;
                buttons.put(slot, new QuantityButton(mode, value, material, name, (List<String>)lore, add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE));
            }
            return buttons;
        }
        String base = "CONFIRM-MENU.AMOUNTS." + group;
        List values = this.config().getIntegerList(base + ".VALUES");
        List slots = this.config().getIntegerList(base + ".SLOTS");
        if (values.isEmpty() || slots.isEmpty()) {
            values = Arrays.asList(new Integer[]{1, 10, 64});
            slots = add ? Arrays.asList(new Integer[]{15, 16, 17}) : Arrays.asList(new Integer[]{11, 10, 9});
        }
        String material = this.config().getString(base + ".MATERIAL", add ? "LIME_STAINED_GLASS_PANE" : "RED_STAINED_GLASS_PANE");
        String name = this.config().getString(base + ".NAME", add ? "&a&l+%value%" : "&c&l-%value%");
        List lore = this.config().getStringList(base + ".LORE");
        for (int index = 0; index < Math.min((int)values.size(), (int)slots.size()); ++index) {
            Mode mode;
            Mode mode2 = mode = add ? Mode.ADD : Mode.SUBTRACT;
            if (hideUnusable && !this.isUsable(mode, (Integer)values.get(index), player)) continue;
            buttons.put(((Integer)slots.get(index)), new QuantityButton(mode, (Integer)values.get(index), material, name, (List<String>)lore, add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE));
        }
        return buttons;
    }

    private int resultOf(Mode mode, int value, Player player) {
        int max = this.getMaxSelectable(player);
        int min = this.minAmount;
        switch (mode.ordinal()) {
            case 2: {
                return Math.max((int)min, (int)Math.min((int)max, (int)value));
            }
            case 3: {
                return max;
            }
            case 4: {
                return min;
            }
            case 1: {
                return Math.max((int)min, (int)(this.amount - value));
            }
        }
        return Math.min((int)max, (int)(this.amount + value));
    }

    private boolean isUsable(Mode mode, int value, Player player) {
        int max = this.getMaxSelectable(player);
        int min = this.minAmount;
        int result = this.resultOf(mode, value, player);
        switch (mode.ordinal()) {
            case 0: {
                return this.amount < max;
            }
            case 1: {
                return this.amount >= value && this.amount > min;
            }
            case 2: {
                return value <= max && result != this.amount;
            }
            case 3: {
                return this.amount != max;
            }
            case 4: {
                return this.amount != min;
            }
        }
        return result != this.amount;
    }

    private List<String> replaceLore(List<String> lore, Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        ArrayList out = new ArrayList();
        if (lore == null) {
            return out;
        }
        for (String line : lore) {
            if (this.bundle <= 1 && line.contains((CharSequence)"%items%")) continue;
            out.add(line.replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)this.amount)).replace((CharSequence)"%items%", (CharSequence)String.valueOf((int)(this.amount * this.bundle))).replace((CharSequence)"%each%", (CharSequence)String.valueOf((int)this.bundle)).replace((CharSequence)"%cost%", (CharSequence)String.valueOf((int)this.amount)).replace((CharSequence)"%keys%", (CharSequence)String.valueOf((int)profile.getKeyAmount(this.crate.getKey()))).replace((CharSequence)"%crate%", (CharSequence)this.crate.getName()).replace((CharSequence)"%min%", (CharSequence)String.valueOf((int)this.minAmount)).replace((CharSequence)"%max%", (CharSequence)String.valueOf((int)this.getMaxSelectable(player))));
        }
        return out;
    }

    private void purchase(Player player) {
        if (this.present) {
            this.openPresents(player);
            return;
        }
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        int keys = profile.getKeyAmount(this.crate.getKey());
        if (keys < 1) {
            this.plugin.message((CommandSender)player, "NOT_ENOUGH_KEYS", "%crate%", this.crate.getName(), "%amount%", String.valueOf((int)this.amount));
            this.plugin.playSound(player, "SOUNDS.ERROR", "minecraft:entity.villager.no|1.0|1.0");
            return;
        }
        int requested = Math.min((int)this.amount, (int)keys);
        boolean dropOverflow = this.config().getBoolean("CONFIRM-MENU.DROP-OVERFLOW", true);
        int delivered = requested;
        boolean commandOnly = this.isCommandOnly();
        if (!commandOnly && !dropOverflow) {
            int roomItems = InventoryUtil.fitCount(player, this.reward, requested * this.bundle);
            int roomPurchases = roomItems / this.bundle;
            if (roomPurchases <= 0) {
                this.plugin.message((CommandSender)player, "INVENTORY_FULL", new String[0]);
                this.plugin.playSound(player, "SOUNDS.ERROR", "minecraft:entity.villager.no|1.0|1.0");
                return;
            }
            delivered = Math.min((int)requested, (int)roomPurchases);
        }
        if (!profile.takeKeys(this.crate.getKey(), delivered)) {
            this.plugin.message((CommandSender)player, "NOT_ENOUGH_KEYS", "%crate%", this.crate.getName(), "%amount%", String.valueOf((int)delivered));
            return;
        }
        this.plugin.getProfileManager().saveAsync(profile);
        if (!commandOnly) {
            List<ItemStack> stacks = InventoryUtil.split(this.reward, delivered * this.bundle);
            List<ItemStack> leftovers = InventoryUtil.give(player, stacks, dropOverflow);
            for (ItemStack leftover : leftovers) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        int purchased = delivered;
        this.runCommands(player, purchased);
        this.plugin.message((CommandSender)player, "REWARD_RECEIVED", "%amount%", String.valueOf((int)purchased), "%items%", String.valueOf((int)(purchased * this.bundle)), "%each%", String.valueOf((int)this.bundle), "%crate%", this.crate.getName(), "%item%", this.itemName());
        if (purchased < requested) {
            this.plugin.message((CommandSender)player, "INVENTORY_PARTIAL", "%amount%", String.valueOf((int)purchased), "%items%", String.valueOf((int)(purchased * this.bundle)), "%left%", String.valueOf((int)(requested - purchased)), "%left_items%", String.valueOf((int)((requested - purchased) * this.bundle)), "%item%", this.itemName());
        }
        this.plugin.playEffect(player, "EFFECTS.PURCHASE", "SNOWFLAKE|35|0.6");
        this.plugin.playSound(player, "SOUNDS.PURCHASE", this.config().getString("CONFIRM-MENU.SOUND", "minecraft:entity.player.levelup|1.0|1.0"));
        if (profile.getKeyAmount(this.crate.getKey()) <= 0) {
            player.closeInventory();
            return;
        }
        this.setAmount(player, this.amount);
        this.update(player);
    }

    /**
     * Opens mystery presents: each key rolls one reward from the crate, weighted by CHANCES.
     *
     * <p>Rolls are independent, so opening 5 presents can hand out 5 different rewards. With
     * DROP-OVERFLOW off, opening stops as soon as a roll no longer fits and only the presents
     * actually opened are charged.
     */
    private void openPresents(Player player) {
        Profile profile = this.plugin.getProfileManager().getProfile(player);
        int keys = profile.getKeyAmount(this.crate.getKey());
        if (keys < 1) {
            this.plugin.message((CommandSender)player, "NOT_ENOUGH_KEYS", "%crate%", this.crate.getName(),
                    "%amount%", String.valueOf(this.amount));
            this.plugin.playSound(player, "SOUNDS.ERROR", "minecraft:entity.villager.no|1.0|1.0");
            return;
        }
        if (this.crate.getRewards().isEmpty()) {
            this.plugin.message((CommandSender)player, "PRESENT_EMPTY", "%crate%", this.crate.getName());
            this.plugin.playSound(player, "SOUNDS.ERROR", "minecraft:entity.villager.no|1.0|1.0");
            return;
        }

        int requested = Math.min(this.amount, keys);
        boolean dropOverflow = this.config().getBoolean("CONFIRM-MENU.DROP-OVERFLOW", true);
        boolean replaceItem = this.config().getBoolean("CONFIRM-MENU.COMMANDS-REPLACE-ITEM", true);
        java.util.Random random = java.util.concurrent.ThreadLocalRandom.current();
        java.util.LinkedHashMap<String, Integer> summary = new java.util.LinkedHashMap<String, Integer>();
        int opened = 0;

        for (int index = 0; index < requested; ++index) {
            Integer key = this.crate.rollReward(random);
            if (key == null) {
                break;
            }
            ItemStack rolled = this.crate.getRewards().get(key);
            if (rolled == null || rolled.getType().isAir()) {
                break;
            }
            int rolledBundle = Math.max(1, rolled.getAmount());
            ItemStack unit = rolled.clone();
            unit.setAmount(1);
            boolean commandOnly = !this.crate.getCommands(key.intValue()).isEmpty() && replaceItem;

            if (!commandOnly) {
                if (!dropOverflow && InventoryUtil.fitCount(player, unit, rolledBundle) < rolledBundle) {
                    // No room for this gift - stop here and charge only for what was opened.
                    break;
                }
                List<ItemStack> stacks = InventoryUtil.split(unit, rolledBundle);
                List<ItemStack> leftovers = InventoryUtil.give(player, stacks, dropOverflow);
                for (ItemStack leftover : leftovers) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            this.runCommandsFor(player, key.intValue(), 1);
            ++opened;

            String name = CrateConfirmMenu.displayName(rolled);
            Integer current = summary.get(name);
            summary.put(name, Integer.valueOf((current == null ? 0 : current.intValue()) + rolledBundle));
        }

        if (opened <= 0) {
            this.plugin.message((CommandSender)player, "INVENTORY_FULL", new String[0]);
            this.plugin.playSound(player, "SOUNDS.ERROR", "minecraft:entity.villager.no|1.0|1.0");
            return;
        }

        profile.takeKeys(this.crate.getKey(), opened);
        this.plugin.getProfileManager().saveAsync(profile);

        StringBuilder rewards = new StringBuilder();
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            if (rewards.length() > 0) {
                rewards.append(this.config().getString("PRESENT.REWARD-SEPARATOR", "&7, "));
            }
            rewards.append(this.config().getString("PRESENT.REWARD-ENTRY", "&a%amount%x %item%")
                    .replace("%amount%", String.valueOf(entry.getValue()))
                    .replace("%item%", entry.getKey()));
        }
        this.plugin.message((CommandSender)player, "PRESENT_RECEIVED",
                "%amount%", String.valueOf(opened),
                "%crate%", this.crate.getName(),
                "%rewards%", CC.translate(rewards.toString()));
        if (opened < requested) {
            this.plugin.message((CommandSender)player, "INVENTORY_PARTIAL",
                    "%amount%", String.valueOf(opened),
                    "%items%", String.valueOf(opened),
                    "%left%", String.valueOf(requested - opened),
                    "%left_items%", String.valueOf(requested - opened),
                    "%item%", this.itemName());
        }

        this.plugin.playEffect(player, "EFFECTS.PURCHASE", "SNOWFLAKE|35|0.6");
        this.plugin.playSound(player, "SOUNDS.PURCHASE",
                this.config().getString("PRESENT.SOUND",
                        this.config().getString("CONFIRM-MENU.SOUND", "minecraft:entity.player.levelup|1.0|1.0")));

        if (profile.getKeyAmount(this.crate.getKey()) <= 0) {
            player.closeInventory();
            return;
        }
        this.setAmount(player, this.amount);
        this.update(player);
    }

    /**
     * PRESENT.LORE with %chances% expanded into one line per reward.
     */
    private List<String> presentLore() {
        ArrayList<String> lore = new ArrayList<String>();
        List<String> configured = this.config().getStringList("PRESENT.LORE");
        boolean showChances = this.config().getBoolean("PRESENT.SHOW-CHANCES", true);
        for (String line : configured) {
            if (line.contains("%chances%")) {
                if (showChances) {
                    lore.addAll(CrateConfirmMenu.chanceLines(this.plugin, this.crate));
                }
                continue;
            }
            lore.add(line);
        }
        return lore;
    }

    /**
     * Runs the commands of one specific reward, used by the present roll.
     */
    private void runCommandsFor(Player player, int rewardKey, int times) {
        List<String> commands = this.crate.getCommands(rewardKey);
        if (commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String command = raw.replace("{player}", player.getName()).replace("%player%", player.getName())
                    .replace("{crate}", this.crate.getName()).replace("%crate%", this.crate.getName());
            if (raw.contains("{amount}") || raw.contains("%amount%")) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(),
                        command.replace("{amount}", String.valueOf(times)).replace("%amount%", String.valueOf(times)));
                continue;
            }
            for (int index = 0; index < times; ++index) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), command);
            }
        }
    }

    /**
     * Display name of a reward, used in the present summary and in the odds list.
     */
    public static String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /**
     * One lore line per reward with its chance, for PRESENT.LORE's %chances% placeholder.
     */
    public static List<String> chanceLines(CratesPlugin plugin, Crate crate) {
        ArrayList<String> lines = new ArrayList<String>();
        double total = crate.getTotalWeight();
        if (total <= 0.0) {
            return lines;
        }
        String format = plugin.getMainConfig().getString("PRESENT.CHANCE-LINE", "&8 \u25aa &f%item% &8- &a%chance%%");
        for (Map.Entry<Integer, ItemStack> entry : crate.getRewards().entrySet()) {
            double percent = crate.getChance(entry.getKey().intValue()) / total * 100.0;
            String chance = percent >= 10.0
                    ? String.valueOf(Math.round(percent))
                    : String.format(Locale.ROOT, "%.1f", percent);
            lines.add(format.replace("%item%", CrateConfirmMenu.displayName(entry.getValue()))
                    .replace("%chance%", chance)
                    .replace("%weight%", String.valueOf(crate.getChance(entry.getKey().intValue()))));
        }
        return lines;
    }

    private boolean isCommandOnly() {
        if (this.present) {
            return false;
        }
        return !this.crate.getCommands(this.rewardKey).isEmpty() && this.config().getBoolean("CONFIRM-MENU.COMMANDS-REPLACE-ITEM", true);
    }

    private void runCommands(Player player, int purchased) {
        List<String> commands = this.crate.getCommands(this.rewardKey);
        if (commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            boolean bulkAware;
            if (raw == null || raw.trim().isEmpty()) continue;
            String command = raw.replace((CharSequence)"{player}", (CharSequence)player.getName()).replace((CharSequence)"%player%", (CharSequence)player.getName()).replace((CharSequence)"{crate}", (CharSequence)this.crate.getName()).replace((CharSequence)"%crate%", (CharSequence)this.crate.getName());
            boolean bl = bulkAware = raw.contains((CharSequence)"{amount}") || raw.contains((CharSequence)"%amount%");
            if (bulkAware) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command.replace((CharSequence)"{amount}", (CharSequence)String.valueOf((int)purchased)).replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)purchased)));
                continue;
            }
            for (int index = 0; index < purchased; ++index) {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command);
            }
        }
    }

    private String itemName() {
        if (this.reward.hasItemMeta() && this.reward.getItemMeta() != null && this.reward.getItemMeta().hasDisplayName()) {
            return this.reward.getItemMeta().getDisplayName();
        }
        return this.reward.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static class StaticButton
    extends Button {
        private final ItemStack itemStack;

        StaticButton(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return this.itemStack.clone();
        }
    }

    private class RewardDisplayButton
    extends Button {
        private RewardDisplayButton() {
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            ItemStack display = CrateConfirmMenu.this.reward.clone();
            display.setAmount(Math.max((int)1, (int)Math.min((int)64, (int)(CrateConfirmMenu.this.amount * CrateConfirmMenu.this.bundle))));
            List<String> source = CrateConfirmMenu.this.present
                    ? CrateConfirmMenu.this.presentLore()
                    : CrateConfirmMenu.this.config().getStringList("CONFIRM-MENU.ITEM-LORE");
            List<String> lore = CrateConfirmMenu.this.replaceLore(source, player);
            return lore.isEmpty() ? display : new ItemBuilder(display).appendLore(lore).build();
        }
    }

    private class ConfirmButton
    extends Button {
        private ConfirmButton() {
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            // A present gets its own wording ("unwrap") while a normal purchase keeps "confirm".
            String base = CrateConfirmMenu.this.present
                    && CrateConfirmMenu.this.config().contains("PRESENT.CONFIRM-BUTTON")
                    ? "PRESENT.CONFIRM-BUTTON"
                    : "CONFIRM-MENU.BUTTONS.CONFIRM";
            List<String> lore = CrateConfirmMenu.this.config().getStringList(base + ".LORE");
            if (lore == null || lore.isEmpty()) {
                lore = CrateConfirmMenu.this.config().getStringList("CONFIRM-MENU.BUTTONS.CONFIRM.LORE");
            }
            return ItemBuilder.of(CrateConfirmMenu.this.config().getString(base + ".MATERIAL",
                            CrateConfirmMenu.this.config().getString("CONFIRM-MENU.BUTTONS.CONFIRM.MATERIAL",
                                    "LIME_STAINED_GLASS_PANE")), Material.LIME_STAINED_GLASS_PANE)
                    .name(CrateConfirmMenu.this.config().getString(base + ".NAME",
                                    "&#00FC00\u1d04\u1d0f\u0274\ua730\u026a\u0280\u1d0d")
                            .replace("%amount%", String.valueOf(CrateConfirmMenu.this.amount)))
                    .lore(CrateConfirmMenu.this.replaceLore(lore, player))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            CrateConfirmMenu.this.purchase(player);
        }
    }

    private class CancelButton
    extends Button {
        private CancelButton() {
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder.of(CrateConfirmMenu.this.config().getString("CONFIRM-MENU.BUTTONS.CANCEL.MATERIAL", "RED_STAINED_GLASS_PANE"), Material.RED_STAINED_GLASS_PANE).name(CrateConfirmMenu.this.config().getString("CONFIRM-MENU.BUTTONS.CANCEL.NAME", "&#FC0000\u1d04\u1d00\u0274\u1d04\u1d07\u029f")).lore(CrateConfirmMenu.this.replaceLore((List<String>)CrateConfirmMenu.this.config().getStringList("CONFIRM-MENU.BUTTONS.CANCEL.LORE"), player)).build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            new CrateViewMenu(CrateConfirmMenu.this.plugin, CrateConfirmMenu.this.crate).openMenu(player);
        }
    }

    private class InfoButton
    extends Button {
        private InfoButton() {
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            int keys = CrateConfirmMenu.this.plugin.getProfileManager().getProfile(player).getKeyAmount(CrateConfirmMenu.this.crate.getKey());
            return ItemBuilder.of(CrateConfirmMenu.this.config().getString("CONFIRM-MENU.BUTTONS.INFO.MATERIAL", "TRIPWIRE_HOOK"), Material.TRIPWIRE_HOOK).name(CrateConfirmMenu.this.config().getString("CONFIRM-MENU.BUTTONS.INFO.NAME", "&eYour keys: &f%keys%").replace((CharSequence)"%keys%", (CharSequence)String.valueOf((int)keys)).replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)CrateConfirmMenu.this.amount))).lore(CrateConfirmMenu.this.replaceLore((List<String>)CrateConfirmMenu.this.config().getStringList("CONFIRM-MENU.BUTTONS.INFO.LORE"), player)).hideAttributes().build();
        }
    }

    private static enum Mode {
        ADD,
        SUBTRACT,
        SET,
        MAX,
        MIN;


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
            if (upper.startsWith("REMOVE") || upper.startsWith("SUB") || upper.startsWith("MINUS") || upper.startsWith("TAKE")) {
                return SUBTRACT;
            }
            if (upper.startsWith("ADD") || upper.startsWith("PLUS") || upper.startsWith("INCREASE")) {
                return ADD;
            }
            return addGroup ? ADD : SUBTRACT;
        }
    }

    private class QuantityButton
    extends Button {
        private final Mode mode;
        private final int value;
        private final String material;
        private final String name;
        private final List<String> lore;
        private final Material fallback;

        QuantityButton(Mode mode, int value, String material, String name, List<String> lore, Material fallback) {
            this.mode = mode;
            this.value = value;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.fallback = fallback;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return ItemBuilder.of(this.material, this.fallback).amount(Math.max((int)1, (int)Math.min((int)64, (int)this.value))).name(this.name.replace((CharSequence)"%value%", (CharSequence)String.valueOf((int)this.value)).replace((CharSequence)"%amount%", (CharSequence)String.valueOf((int)CrateConfirmMenu.this.amount))).lore(CrateConfirmMenu.this.replaceLore(this.lore, player)).hideAttributes().build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            int max = CrateConfirmMenu.this.getMaxSelectable(player);
            int min = CrateConfirmMenu.this.minAmount;
            int current = CrateConfirmMenu.this.amount;
            switch (this.mode.ordinal()) {
                case 2: {
                    CrateConfirmMenu.this.setAmount(player, this.value);
                    break;
                }
                case 3: {
                    CrateConfirmMenu.this.setAmount(player, max);
                    break;
                }
                case 4: {
                    CrateConfirmMenu.this.setAmount(player, min);
                    break;
                }
                case 1: {
                    CrateConfirmMenu.this.setAmount(player, clickType.isShiftClick() ? min : current - this.value);
                    break;
                }
                default: {
                    CrateConfirmMenu.this.setAmount(player, clickType.isShiftClick() ? max : current + this.value);
                }
            }
            CrateConfirmMenu.this.plugin.playSound(player, "SOUNDS.BUTTON-CLICK", "minecraft:block.bubble_column.bubble_pop|0.8|1.2");
        }

        @Override
        public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
            return true;
        }
    }
}
