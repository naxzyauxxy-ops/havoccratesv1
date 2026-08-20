package net.havoc.crates.crate;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A crate, matching the layout used by crates.yml:
 *
 * <pre>
 * Common:
 *   TITLE: '&8ᴄʜᴏᴏsᴇ 1 ɪᴛᴇᴍ'
 *   ROWS: 3
 *   LOCATIONS:
 *   - world,99,-21,70
 *   COMMANDS:
 *     '10': 'give {player} diamond'
 *   POSITIONS:
 *     '10': -1          # -1 = auto centered
 *   REWARDS:
 *     '10': &lt;ItemStack&gt;
 * </pre>
 */
public class Crate {

    private final String name;
    private String title;
    private int rows = 3;

    /** reward key -> item */
    private final Map<Integer, ItemStack> rewards = new TreeMap<>();
    /** reward key -> menu slot, -1 (or missing) means auto centered */
    private final Map<Integer, Integer> positions = new HashMap<>();
    /** reward key -> console commands */
    private final Map<Integer, List<String>> commands = new HashMap<>();
    /** "world,x,y,z" */
    private final Set<String> locations = new LinkedHashSet<>();

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

    /** Kept for messages that want a nice crate name. */
    public String getDisplayName() {
        return this.name;
    }

    public int getRows() {
        return this.rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
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
        List<String> list = this.commands.get(rewardKey);
        return list == null ? new ArrayList<>() : list;
    }

    public void setCommands(int rewardKey, List<String> list) {
        List<String> cleaned = new ArrayList<>();
        if (list != null) {
            for (String line : list) {
                if (line != null && !line.trim().isEmpty()) {
                    cleaned.add(line);
                }
            }
        }
        if (cleaned.isEmpty()) {
            this.commands.remove(rewardKey);
        } else {
            this.commands.put(rewardKey, cleaned);
        }
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

    public static String serialize(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + ","
                + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Accepts both "world,x,y,z" and the older "world;x;y;z".
     */
    public static String normalizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.trim().split("[,;]");
        if (parts.length < 4) {
            return null;
        }
        try {
            return parts[0].trim() + "," + Integer.parseInt(parts[1].trim()) + ","
                    + Integer.parseInt(parts[2].trim()) + "," + Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Works out where every reward is drawn.
     *
     * <p>A reward with an explicit POSITION uses it; everything else is centered, exactly like the
     * old menus (7 rewards in a 3 row menu land on 10-16, 5 land on 11-15, and so on).
     *
     * @return menu slot -> reward key
     */
    public Map<Integer, Integer> resolveSlots() {
        Map<Integer, Integer> slotToKey = new LinkedHashMap<>();
        int size = getSize();
        Set<Integer> taken = new HashSet<>();
        List<Integer> auto = new ArrayList<>();

        for (Map.Entry<Integer, ItemStack> entry : this.rewards.entrySet()) {
            ItemStack item = entry.getValue();
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Integer position = this.positions.get(entry.getKey());
            if (position != null && position >= 0 && position < size && !taken.contains(position)) {
                taken.add(position);
                slotToKey.put(position, entry.getKey());
            } else {
                auto.add(entry.getKey());
            }
        }

        List<Integer> slots = centeredSlots(auto.size(), size, taken);
        for (int index = 0; index < auto.size() && index < slots.size(); index++) {
            slotToKey.put(slots.get(index), auto.get(index));
        }
        return slotToKey;
    }

    /**
     * Builds a centered block of slots for {@code count} items.
     */
    private static List<Integer> centeredSlots(int count, int size, Set<Integer> taken) {
        List<Integer> slots = new ArrayList<>();
        if (count <= 0) {
            return slots;
        }
        int rows = Math.max(1, size / 9);
        int rowsNeeded = Math.min(rows, (int) Math.ceil(count / 9.0));
        int startRow = (rows - rowsNeeded) / 2;
        int remaining = count;
        for (int row = 0; row < rowsNeeded && remaining > 0; row++) {
            int inRow = Math.min(9, (int) Math.ceil((double) remaining / (rowsNeeded - row)));
            int startColumn = (9 - inRow) / 2;
            for (int column = 0; column < inRow; column++) {
                int slot = (startRow + row) * 9 + startColumn + column;
                if (slot < size && !taken.contains(slot)) {
                    slots.add(slot);
                    taken.add(slot);
                }
            }
            remaining -= inRow;
        }
        // If explicit positions stole some of the centered slots, top up with whatever is free.
        for (int slot = 0; slot < size && slots.size() < count; slot++) {
            if (!taken.contains(slot)) {
                slots.add(slot);
                taken.add(slot);
            }
        }
        return slots;
    }
}
