package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A plain chest the admin can drag rewards into. Saved on close, keeping each reward's commands
 * with whatever slot it ends up in.
 */
public class CrateEditMenu implements InventoryHolder {

    private final CratesPlugin plugin;
    private final Crate crate;
    private final Inventory inventory;
    /** slot -> reward key, as it looked when the menu was opened */
    private final Map<Integer, Integer> originalSlots;

    public CrateEditMenu(CratesPlugin plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.originalSlots = new LinkedHashMap<>(crate.resolveSlots());
        this.inventory = Bukkit.createInventory(this, crate.getSize(),
                CC.translate("&8Editing: &f" + crate.getName()));
        for (Map.Entry<Integer, Integer> entry : this.originalSlots.entrySet()) {
            ItemStack item = crate.getRewards().get(entry.getValue());
            if (item != null && entry.getKey() < crate.getSize()) {
                this.inventory.setItem(entry.getKey(), item.clone());
            }
        }
    }

    public Crate getCrate() {
        return this.crate;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }

    /**
     * Rewrites the crate's rewards from what is in the chest, storing explicit positions so the
     * layout the admin sees is the layout players get.
     */
    public void save() {
        Map<Integer, List<String>> keptCommands = new HashMap<>();
        Map<Integer, ItemStack> rewards = new LinkedHashMap<>();
        Map<Integer, Integer> positions = new LinkedHashMap<>();

        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            ItemStack item = this.inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Integer previousKey = this.originalSlots.get(slot);
            int key = previousKey == null ? nextFreeKey(rewards, slot) : previousKey;
            rewards.put(key, item.clone());
            positions.put(key, slot);
            List<String> commands = previousKey == null
                    ? new ArrayList<>() : this.crate.getCommands(previousKey);
            if (!commands.isEmpty()) {
                keptCommands.put(key, commands);
            }
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
            key++;
        }
        return key;
    }
}
