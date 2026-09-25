/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.NumberFormatException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.Arrays
 *  java.util.Collection
 *  java.util.LinkedHashMap
 *  java.util.List
 *  java.util.Map
 *  java.util.Map$Entry
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.crate;

import java.lang.Integer;
import java.lang.NumberFormatException;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.Config;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class CrateManager {
    private final CratesPlugin plugin;
    private final Map<String, Crate> crates = new LinkedHashMap();

    public CrateManager(CratesPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    public Map<String, Crate> getCrates() {
        return this.crates;
    }

    public Crate getCrate(String name) {
        return name == null ? null : (Crate)this.crates.get(name.toLowerCase());
    }

    public Crate getCrate(Location location) {
        if (location == null) {
            return null;
        }
        String key = Crate.serialize(location);
        for (Crate crate : this.crates.values()) {
            if (!crate.getLocations().contains(key)) continue;
            return crate;
        }
        return null;
    }

    public Crate createCrate(String name) {
        Crate crate = new Crate(name);
        crate.setTitle("&8" + name);
        this.crates.put(crate.getKey(), crate);
        this.save();
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
        FileConfiguration root = config.getConfiguration();
        ConfigurationSection legacy = root.getConfigurationSection("CRATES");
        if (legacy != null) {
            for (String name : legacy.getKeys(false)) {
                this.readCrate(name, legacy.getConfigurationSection(name));
            }
        }
        for (String name : root.getKeys(false)) {
            if (name.equalsIgnoreCase("CRATES")) continue;
            this.readCrate(name, root.getConfigurationSection(name));
        }
        this.plugin.getLogger().info("Loaded " + this.crates.size() + " crate(s).");
    }

    private void readCrate(String name, ConfigurationSection section) {
        ConfigurationSection items;
        ConfigurationSection commands;
        ConfigurationSection positions;
        if (section == null) {
            return;
        }
        Crate crate = new Crate(name);
        crate.setTitle(section.getString("TITLE", section.getString("DISPLAY-NAME", "&8" + name)));
        crate.setRows(section.getInt("ROWS", 3));
        for (String raw : section.getStringList("LOCATIONS")) {
            String normalized = Crate.normalizeLocation(raw);
            if (normalized == null) continue;
            crate.getLocations().add(normalized);
        }
        ConfigurationSection rewards = section.getConfigurationSection("REWARDS");
        if (rewards != null) {
            for (Object rawKey : rewards.getKeys(false)) {
                ItemStack item;
                Integer key = this.parseInt((String)rawKey);
                if (key == null || (item = rewards.getItemStack((String)rawKey)) == null || item.getType().isAir()) continue;
                crate.getRewards().put(key, item);
            }
        }
        if ((positions = section.getConfigurationSection("POSITIONS")) != null) {
            for (Object rawKey : positions.getKeys(false)) {
                Integer key = this.parseInt((String)rawKey);
                if (key == null) continue;
                crate.getPositions().put(key, positions.getInt((String)rawKey, -1));
            }
        }
        ConfigurationSection chances = section.getConfigurationSection("CHANCES");
        if (chances != null) {
            for (String rawKey : chances.getKeys(false)) {
                Integer key = this.parseInt(rawKey);
                if (key == null) continue;
                crate.getChances().put(key, Double.valueOf(chances.getDouble(rawKey, 1.0)));
            }
        }
        if ((commands = section.getConfigurationSection("COMMANDS")) != null) {
            for (String rawKey : commands.getKeys(false)) {
                Integer key = this.parseInt(rawKey);
                if (key == null) continue;
                List list = commands.getStringList(rawKey);
                if (list == null || list.isEmpty()) {
                    String single = commands.getString(rawKey, "");
                    list = single == null || single.trim().isEmpty() ? new ArrayList() : new ArrayList((Collection)Arrays.asList(new String[]{single}));
                }
                crate.setCommands(key, (List<String>)list);
            }
        }
        if ((items = section.getConfigurationSection("ITEMS")) != null) {
            for (String rawKey : items.getKeys(false)) {
                Integer key = this.parseInt(rawKey);
                if (key == null) continue;
                ItemStack item = items.getItemStack(rawKey + ".ITEM");
                if (item != null) {
                    crate.getRewards().put(key, item);
                    crate.getPositions().put(key, key);
                }
                crate.setCommands(key, (List<String>)items.getStringList(rawKey + ".COMMANDS"));
            }
        }
        if (crate.getRewards().isEmpty() && crate.getLocations().isEmpty() && !section.contains("REWARDS") && !section.contains("TITLE")) {
            return;
        }
        this.normalizeCommands(crate);
        this.crates.put(crate.getKey(), crate);
    }

    private void normalizeCommands(Crate crate) {
        LinkedHashMap raw = new LinkedHashMap(crate.getCommands());
        crate.getCommands().clear();
        for (Map.Entry entry : crate.resolveSlots().entrySet()) {
            int slot = (Integer)entry.getKey();
            int rewardKey = (Integer)entry.getValue();
            List commands = (List)raw.get(rewardKey);
            if (commands == null || commands.isEmpty()) {
                commands = (List)raw.get(slot);
            }
            if (commands == null || commands.isEmpty()) continue;
            crate.setCommands(rewardKey, (List<String>)commands);
        }
    }

    public void save() {
        Config config = this.plugin.getCratesConfig();
        for (String key : new ArrayList<String>(config.getConfiguration().getKeys(false))) {
            config.getConfiguration().set(key, null);
        }
        for (Crate crate : this.crates.values()) {
            String base = crate.getName();
            config.getConfiguration().set(base + ".TITLE", crate.getTitle());
            config.getConfiguration().set(base + ".ROWS", crate.getRows());
            config.getConfiguration().set(base + ".LOCATIONS", new ArrayList(crate.getLocations()));
            for (Map.Entry entry : crate.getRewards().entrySet()) {
                String key = String.valueOf(entry.getKey());
                config.getConfiguration().set(base + ".REWARDS." + key, entry.getValue());
                Integer position = (Integer)crate.getPositions().get(entry.getKey());
                config.getConfiguration().set(base + ".POSITIONS." + key, (position == null ? -1 : position));
                List<String> commands = crate.getCommands((Integer)entry.getKey());
                config.getConfiguration().set(base + ".COMMANDS." + key, commands.isEmpty() ? "" : (commands.size() == 1 ? commands.get(0) : commands));
                Double chance = crate.getChances().get(entry.getKey());
                if (chance != null) {
                    config.getConfiguration().set(base + ".CHANCES." + key, chance);
                }
            }
        }
        config.save();
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt((String)raw.trim());
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    public List<String> getCrateNames() {
        ArrayList names = new ArrayList();
        for (Crate crate : this.crates.values()) {
            names.add(crate.getName());
        }
        return names;
    }
}
