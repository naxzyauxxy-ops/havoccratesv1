package net.havoc.crates.crate;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A crate: a set of reward slots, the physical blocks it is bound to and its display name.
 */
public class Crate {

    private final String name;
    private String displayName;
    private final ItemStack[] contents = new ItemStack[54];
    private final Map<Integer, List<String>> commands = new HashMap<>();
    private final Set<String> locations = new LinkedHashSet<>();

    public Crate(String name) {
        this.name = name;
        this.displayName = "&b" + name;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack[] getContents() {
        return this.contents;
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.contents.length) {
            return null;
        }
        return this.contents[slot];
    }

    public void setItem(int slot, ItemStack itemStack) {
        if (slot < 0 || slot >= this.contents.length) {
            return;
        }
        this.contents[slot] = itemStack;
    }

    public void setContents(ItemStack[] items) {
        for (int slot = 0; slot < this.contents.length; slot++) {
            this.contents[slot] = slot < items.length ? items[slot] : null;
        }
    }

    public List<String> getCommands(int slot) {
        List<String> list = this.commands.get(slot);
        return list == null ? new ArrayList<>() : list;
    }

    public void setCommands(int slot, List<String> list) {
        if (list == null || list.isEmpty()) {
            this.commands.remove(slot);
        } else {
            this.commands.put(slot, new ArrayList<>(list));
        }
    }

    public Map<Integer, List<String>> getAllCommands() {
        return this.commands;
    }

    public Set<String> getLocations() {
        return this.locations;
    }

    public void addLocation(Location location) {
        this.locations.add(serialize(location));
    }

    public void removeLocation(Location location) {
        this.locations.remove(serialize(location));
    }

    public boolean isAt(Location location) {
        return this.locations.contains(serialize(location));
    }

    public static String serialize(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";"
                + location.getBlockY() + ";" + location.getBlockZ();
    }

    /**
     * @return the highest occupied slot, rounded up to a full row (min 9, max 54).
     */
    public int getMenuSize() {
        int highest = -1;
        for (int slot = 0; slot < this.contents.length; slot++) {
            if (this.contents[slot] != null) {
                highest = slot;
            }
        }
        if (highest < 0) {
            return 27;
        }
        int size = ((highest / 9) + 1) * 9;
        return Math.max(9, Math.min(54, size));
    }
}
