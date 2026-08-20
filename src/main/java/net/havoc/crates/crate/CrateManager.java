package net.havoc.crates.crate;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.util.Config;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes crates.yml.
 *
 * <p>The native format is the one produced by the previous crates plugin: every crate is a
 * top level section holding TITLE, ROWS, LOCATIONS, COMMANDS, POSITIONS and REWARDS. A legacy
 * "CRATES:" root section is still understood.
 */
public class CrateManager {

    private final CratesPlugin plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public CrateManager(CratesPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public Map<String, Crate> getCrates() {
        return this.crates;
    }

    public Crate getCrate(String name) {
        return name == null ? null : this.crates.get(name.toLowerCase());
    }

    public Crate getCrate(Location location) {
        if (location == null) {
            return null;
        }
        String key = Crate.serialize(location);
        for (Crate crate : this.crates.values()) {
            if (crate.getLocations().contains(key)) {
                return crate;
            }
        }
        return null;
    }

    public Crate createCrate(String name) {
        Crate crate = new Crate(name);
        crate.setTitle("&8" + name);
        this.crates.put(crate.getKey(), crate);
        save();
        return crate;
    }

    public void deleteCrate(Crate crate) {
        this.crates.remove(crate.getKey());
        this.plugin.getCratesConfig().getConfiguration().set(crate.getName(), null);
        this.plugin.getCratesConfig().save();
    }

    public void load() {
        this.crates.clear();
        Config config = this.plugin.getCratesConfig();
        config.reload();

        ConfigurationSection root = config.getConfiguration();
        // Legacy layout: everything nested under CRATES.
        ConfigurationSection legacy = root.getConfigurationSection("CRATES");
        if (legacy != null) {
            for (String name : legacy.getKeys(false)) {
                readCrate(name, legacy.getConfigurationSection(name));
            }
        }
        for (String name : root.getKeys(false)) {
            if (name.equalsIgnoreCase("CRATES")) {
                continue;
            }
            readCrate(name, root.getConfigurationSection(name));
        }
        this.plugin.getLogger().info("Loaded " + this.crates.size() + " crate(s).");
    }

    private void readCrate(String name, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        Crate crate = new Crate(name);
        crate.setTitle(section.getString("TITLE", section.getString("DISPLAY-NAME", "&8" + name)));
        crate.setRows(section.getInt("ROWS", 3));

        for (String raw : section.getStringList("LOCATIONS")) {
            String normalized = Crate.normalizeLocation(raw);
            if (normalized != null) {
                crate.getLocations().add(normalized);
            }
        }

        ConfigurationSection rewards = section.getConfigurationSection("REWARDS");
        if (rewards != null) {
            for (String rawKey : rewards.getKeys(false)) {
                Integer key = parseInt(rawKey);
                if (key == null) {
                    continue;
                }
                ItemStack item = rewards.getItemStack(rawKey);
                if (item != null && !item.getType().isAir()) {
                    crate.getRewards().put(key, item);
                }
            }
        }

        ConfigurationSection positions = section.getConfigurationSection("POSITIONS");
        if (positions != null) {
            for (String rawKey : positions.getKeys(false)) {
                Integer key = parseInt(rawKey);
                if (key != null) {
                    crate.getPositions().put(key, positions.getInt(rawKey, -1));
                }
            }
        }

        ConfigurationSection commands = section.getConfigurationSection("COMMANDS");
        if (commands != null) {
            for (String rawKey : commands.getKeys(false)) {
                Integer key = parseInt(rawKey);
                if (key == null) {
                    continue;
                }
                // A slot can hold a single command string or a list of them.
                List<String> list = commands.getStringList(rawKey);
                if (list == null || list.isEmpty()) {
                    String single = commands.getString(rawKey, "");
                    list = single == null || single.trim().isEmpty()
                            ? new ArrayList<>() : new ArrayList<>(Arrays.asList(single));
                }
                crate.setCommands(key, list);
            }
        }

        // Legacy layout: ITEMS.<slot>.ITEM / .COMMANDS
        ConfigurationSection items = section.getConfigurationSection("ITEMS");
        if (items != null) {
            for (String rawKey : items.getKeys(false)) {
                Integer key = parseInt(rawKey);
                if (key == null) {
                    continue;
                }
                ItemStack item = items.getItemStack(rawKey + ".ITEM");
                if (item != null) {
                    crate.getRewards().put(key, item);
                    crate.getPositions().put(key, key);
                }
                crate.setCommands(key, items.getStringList(rawKey + ".COMMANDS"));
            }
        }

        if (crate.getRewards().isEmpty() && crate.getLocations().isEmpty()
                && !section.contains("REWARDS") && !section.contains("TITLE")) {
            // Not a crate section (some unrelated key), ignore it.
            return;
        }

        normalizeCommands(crate);
        this.crates.put(crate.getKey(), crate);
    }

    /**
     * COMMANDS can be keyed either by reward key or by the slot the reward is drawn on - the old
     * plugin wrote both (Ruby stores its rewards as 1-5 but its commands as 11-15). Anything keyed
     * by slot is moved onto the reward that occupies that slot, and stale keys are dropped.
     */
    private void normalizeCommands(Crate crate) {
        Map<Integer, List<String>> raw = new LinkedHashMap<>(crate.getCommands());
        crate.getCommands().clear();
        for (Map.Entry<Integer, Integer> entry : crate.resolveSlots().entrySet()) {
            int slot = entry.getKey();
            int rewardKey = entry.getValue();
            List<String> commands = raw.get(rewardKey);
            if (commands == null || commands.isEmpty()) {
                commands = raw.get(slot);
            }
            if (commands != null && !commands.isEmpty()) {
                crate.setCommands(rewardKey, commands);
            }
        }
    }

    public void save() {
        Config config = this.plugin.getCratesConfig();
        // Wipe every known crate section, then write them back in the original layout.
        for (String key : new ArrayList<>(config.getConfiguration().getKeys(false))) {
            config.getConfiguration().set(key, null);
        }
        for (Crate crate : this.crates.values()) {
            String base = crate.getName();
            config.getConfiguration().set(base + ".TITLE", crate.getTitle());
            config.getConfiguration().set(base + ".ROWS", crate.getRows());
            config.getConfiguration().set(base + ".LOCATIONS", new ArrayList<>(crate.getLocations()));

            for (Map.Entry<Integer, ItemStack> entry : crate.getRewards().entrySet()) {
                String key = String.valueOf(entry.getKey());
                config.getConfiguration().set(base + ".REWARDS." + key, entry.getValue());
                Integer position = crate.getPositions().get(entry.getKey());
                config.getConfiguration().set(base + ".POSITIONS." + key, position == null ? -1 : position);
                List<String> commands = crate.getCommands(entry.getKey());
                config.getConfiguration().set(base + ".COMMANDS." + key,
                        commands.isEmpty() ? "" : (commands.size() == 1 ? commands.get(0) : commands));
            }
        }
        config.save();
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public List<String> getCrateNames() {
        List<String> names = new ArrayList<>();
        for (Crate crate : this.crates.values()) {
            names.add(crate.getName());
        }
        return names;
    }
}
