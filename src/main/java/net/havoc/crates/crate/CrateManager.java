package net.havoc.crates.crate;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.util.Config;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads, stores and saves every crate defined in crates.yml.
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
        if (name == null) {
            return null;
        }
        return this.crates.get(name.toLowerCase());
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
        Crate crate = new Crate(name.toLowerCase());
        this.crates.put(crate.getName(), crate);
        save();
        return crate;
    }

    public void deleteCrate(Crate crate) {
        this.crates.remove(crate.getName());
        this.plugin.getCratesConfig().getConfiguration().set("CRATES." + crate.getName(), null);
        this.plugin.getCratesConfig().save();
    }

    public void load() {
        this.crates.clear();
        Config config = this.plugin.getCratesConfig();
        config.reload();
        ConfigurationSection root = config.getConfiguration().getConfigurationSection("CRATES");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            Crate crate = new Crate(name.toLowerCase());
            crate.setDisplayName(section.getString("DISPLAY-NAME", "&b" + name));
            for (String raw : section.getStringList("LOCATIONS")) {
                crate.getLocations().add(raw);
            }
            ConfigurationSection items = section.getConfigurationSection("ITEMS");
            if (items != null) {
                for (String rawSlot : items.getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(rawSlot);
                    } catch (NumberFormatException exception) {
                        continue;
                    }
                    ItemStack item = items.getItemStack(rawSlot + ".ITEM");
                    if (item != null) {
                        crate.setItem(slot, item);
                    }
                    List<String> commands = items.getStringList(rawSlot + ".COMMANDS");
                    if (!commands.isEmpty()) {
                        crate.setCommands(slot, commands);
                    }
                }
            }
            this.crates.put(crate.getName(), crate);
        }
    }

    public void save() {
        Config config = this.plugin.getCratesConfig();
        config.getConfiguration().set("CRATES", null);
        for (Crate crate : this.crates.values()) {
            String base = "CRATES." + crate.getName();
            config.getConfiguration().set(base + ".DISPLAY-NAME", crate.getDisplayName());
            config.getConfiguration().set(base + ".LOCATIONS", new ArrayList<>(crate.getLocations()));
            for (int slot = 0; slot < crate.getContents().length; slot++) {
                ItemStack item = crate.getItem(slot);
                List<String> commands = crate.getCommands(slot);
                if (item == null && commands.isEmpty()) {
                    continue;
                }
                if (item != null) {
                    config.getConfiguration().set(base + ".ITEMS." + slot + ".ITEM", item);
                }
                if (!commands.isEmpty()) {
                    config.getConfiguration().set(base + ".ITEMS." + slot + ".COMMANDS", commands);
                }
            }
        }
        config.save();
    }

    public List<String> getCrateNames() {
        return new ArrayList<>(this.crates.keySet());
    }
}
