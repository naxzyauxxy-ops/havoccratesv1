/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.Object
 *  java.lang.String
 *  java.util.HashMap
 *  java.util.LinkedHashMap
 *  java.util.List
 *  java.util.Map
 *  java.util.Map$Entry
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.ui;

import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CrateEditMenu
implements InventoryHolder {
    private final CratesPlugin plugin;
    private final Crate crate;
    private final Inventory inventory;
    private final Map<Integer, Integer> originalSlots;

    public CrateEditMenu(CratesPlugin plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.originalSlots = new LinkedHashMap(crate.resolveSlots());
        this.inventory = Bukkit.createInventory((InventoryHolder)this, (int)crate.getSize(), (String)CC.translate("&8Editing: &f" + crate.getName()));
        for (Map.Entry entry : this.originalSlots.entrySet()) {
            ItemStack item = (ItemStack)crate.getRewards().get(entry.getValue());
            if (item == null || (Integer)entry.getKey() >= crate.getSize()) continue;
            this.inventory.setItem(((Integer)entry.getKey()).intValue(), item.clone());
        }
    }

    public Crate getCrate() {
        return this.crate;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }

    public void save() {
        HashMap<Integer, List<String>> keptCommands = new HashMap<Integer, List<String>>();
        LinkedHashMap<Integer, ItemStack> rewards = new LinkedHashMap<Integer, ItemStack>();
        LinkedHashMap<Integer, Integer> positions = new LinkedHashMap<Integer, Integer>();
        for (int slot = 0; slot < this.inventory.getSize(); ++slot) {
            List<String> commands;
            ItemStack item = this.inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            Integer previousKey = (Integer)this.originalSlots.get(slot);
            int key = previousKey == null ? this.nextFreeKey(rewards, slot) : previousKey.intValue();
            rewards.put(key, item.clone());
            positions.put(key, slot);
            List<String> list = commands = previousKey == null ? new ArrayList<String>() : this.crate.getCommands(previousKey);
            if (commands.isEmpty()) continue;
            keptCommands.put(key, commands);
        }
        this.crate.getRewards().clear();
        this.crate.getRewards().putAll(rewards);
        this.crate.getPositions().clear();
        this.crate.getPositions().putAll(positions);
        this.crate.getCommands().clear();
        this.crate.getCommands().putAll(keptCommands);
        this.plugin.getCrateManager().save();
    }

    private int nextFreeKey(Map<Integer, ItemStack> used, int preferred) {
        int key = preferred;
        while (used.containsKey(key)) {
            ++key;
        }
        return key;
    }
}
