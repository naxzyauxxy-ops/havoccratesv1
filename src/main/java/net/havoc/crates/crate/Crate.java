/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.Math
 *  java.lang.NumberFormatException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.HashMap
 *  java.util.HashSet
 *  java.util.LinkedHashMap
 *  java.util.LinkedHashSet
 *  java.util.List
 *  java.util.Map
 *  java.util.Map$Entry
 *  java.util.Set
 *  java.util.TreeMap
 *  org.bukkit.Location
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.crate;

import java.lang.Integer;
import java.lang.Math;
import java.lang.NumberFormatException;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class Crate {
    private final String name;
    private String title;
    private int rows = 3;
    private final Map<Integer, ItemStack> rewards = new TreeMap();
    private final Map<Integer, Integer> positions = new HashMap();
    private final Map<Integer, List<String>> commands = new HashMap();
    /** reward key -> weight used by the mystery present roll */
    private final Map<Integer, Double> chances = new HashMap<Integer, Double>();
    private final Set<String> locations = new LinkedHashSet();

    public Crate(String name) {
        this.name = name;
        this.title = "&8" + name;
    }

    public String getName() {
        return this.name;
    }

    public String getKey() {
        return this.name.toLowerCase();
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDisplayName() {
        return this.name;
    }

    public int getRows() {
        return this.rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max((int)1, (int)Math.min((int)6, (int)rows));
    }

    public int getSize() {
        return this.rows * 9;
    }

    public Map<Integer, ItemStack> getRewards() {
        return this.rewards;
    }

    public Map<Integer, Integer> getPositions() {
        return this.positions;
    }

    public Map<Integer, List<String>> getCommands() {
        return this.commands;
    }

    public List<String> getCommands(int rewardKey) {
        List list = (List)this.commands.get(rewardKey);
        return list == null ? new ArrayList() : list;
    }

    public void setCommands(int rewardKey, List<String> list) {
        ArrayList cleaned = new ArrayList();
        if (list != null) {
            for (String line : list) {
                if (line == null || line.trim().isEmpty()) continue;
                cleaned.add(line);
            }
        }
        if (cleaned.isEmpty()) {
            this.commands.remove(rewardKey);
        } else {
            this.commands.put(rewardKey, cleaned);
        }
    }

    public Map<Integer, Double> getChances() {
        return this.chances;
    }

    /**
     * Weight of one reward in the present roll. Rewards with no CHANCES entry weigh 1, so a
     * crate with no chances at all rolls every reward evenly.
     */
    public double getChance(int rewardKey) {
        Double value = this.chances.get(rewardKey);
        return value == null ? 1.0 : Math.max(0.0, value.doubleValue());
    }

    public double getTotalWeight() {
        double total = 0.0;
        for (Integer key : this.rewards.keySet()) {
            total += this.getChance(key.intValue());
        }
        return total;
    }

    /**
     * Picks a reward at random, weighted by CHANCES.
     *
     * @return the reward key, or null when the crate has no rewards.
     */
    public Integer rollReward(java.util.Random random) {
        double total = this.getTotalWeight();
        if (this.rewards.isEmpty() || total <= 0.0) {
            return null;
        }
        double roll = random.nextDouble() * total;
        Integer last = null;
        for (Integer key : this.rewards.keySet()) {
            last = key;
            roll -= this.getChance(key.intValue());
            if (roll <= 0.0) {
                return key;
            }
        }
        return last;
    }

    public Set<String> getLocations() {
        return this.locations;
    }

    public void addLocation(Location location) {
        this.locations.add(Crate.serialize(location));
    }

    public void removeLocation(Location location) {
        this.locations.remove(Crate.serialize(location));
    }

    public static String serialize(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public static String normalizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.trim().split("[,;]");
        if (parts.length < 4) {
            return null;
        }
        try {
            return parts[0].trim() + "," + Integer.parseInt((String)parts[1].trim()) + "," + Integer.parseInt((String)parts[2].trim()) + "," + Integer.parseInt((String)parts[3].trim());
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    public Map<Integer, Integer> resolveSlots() {
        LinkedHashMap slotToKey = new LinkedHashMap();
        int size = this.getSize();
        HashSet taken = new HashSet();
        ArrayList auto = new ArrayList();
        for (Map.Entry entry : this.rewards.entrySet()) {
            ItemStack item = (ItemStack)entry.getValue();
            if (item == null || item.getType().isAir()) continue;
            Integer position = (Integer)this.positions.get(entry.getKey());
            if (position != null && position >= 0 && position < size && !taken.contains(position)) {
                taken.add(position);
                slotToKey.put(position, ((Integer)entry.getKey()));
                continue;
            }
            auto.add(((Integer)entry.getKey()));
        }
        List<Integer> slots = Crate.centeredSlots(auto.size(), size, (Set<Integer>)taken);
        for (int index = 0; index < auto.size() && index < slots.size(); ++index) {
            slotToKey.put(((Integer)slots.get(index)), ((Integer)auto.get(index)));
        }
        return slotToKey;
    }

    private static List<Integer> centeredSlots(int count, int size, Set<Integer> taken) {
        int inRow;
        ArrayList slots = new ArrayList();
        if (count <= 0) {
            return slots;
        }
        int rows = Math.max((int)1, (int)(size / 9));
        int rowsNeeded = Math.min((int)rows, (int)((int)Math.ceil((double)((double)count / 9.0))));
        int startRow = (rows - rowsNeeded) / 2;
        int remaining = count;
        for (int row = 0; row < rowsNeeded && remaining > 0; remaining -= inRow, ++row) {
            inRow = Math.min((int)9, (int)((int)Math.ceil((double)((double)remaining / (double)(rowsNeeded - row)))));
            int startColumn = (9 - inRow) / 2;
            for (int column = 0; column < inRow; ++column) {
                int slot = (startRow + row) * 9 + startColumn + column;
                if (slot >= size || taken.contains(slot)) continue;
                slots.add(slot);
                taken.add(slot);
            }
        }
        for (int slot = 0; slot < size && slots.size() < count; ++slot) {
            if (taken.contains(slot)) continue;
            slots.add(slot);
            taken.add(slot);
        }
        return slots;
    }
}
