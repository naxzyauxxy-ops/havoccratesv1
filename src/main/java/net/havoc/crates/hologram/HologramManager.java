package net.havoc.crates.hologram;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Floating text above every crate block, built from invisible marker armour stands so it works on
 * any server without a hologram plugin.
 *
 * <p>Lines come from HOLOGRAMS.LINES, or HOLOGRAMS.CRATES.&lt;crate&gt;.LINES for one crate.
 * The stands are non-persistent, so they never end up saved in the world, and a repeating task
 * puts them back after a chunk reload.
 */
public class HologramManager {

    private final CratesPlugin plugin;
    private final List<ArmorStand> spawned = new ArrayList<ArmorStand>();

    public HologramManager(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return this.plugin.getMainConfig().getBoolean("HOLOGRAMS.ENABLED", true);
    }

    /**
     * Starts the keep-alive task. Holograms are drawn a second after startup so every world is
     * loaded first.
     */
    public void start() {
        Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {

            @Override
            public void run() {
                HologramManager.this.refresh();
            }
        }, 20L);

        long seconds = Math.max(5L, this.plugin.getMainConfig().getInt("HOLOGRAMS.REFRESH-SECONDS", 30));
        Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable() {

            @Override
            public void run() {
                HologramManager.this.healMissing();
            }
        }, seconds * 20L, seconds * 20L);
    }

    /**
     * Removes every hologram and draws them again - used on enable, /crates reload and whenever a
     * crate block is bound or unbound.
     */
    public void refresh() {
        this.despawnAll();
        if (!this.enabled()) {
            return;
        }
        for (Crate crate : this.plugin.getCrateManager().getCrates().values()) {
            for (String raw : crate.getLocations()) {
                Location location = this.parse(raw);
                if (location != null) {
                    this.spawn(crate, location);
                }
            }
        }
    }

    /**
     * Redraws everything when any stand has gone missing (chunk unload, another plugin, /kill).
     */
    private void healMissing() {
        if (!this.enabled()) {
            return;
        }
        for (ArmorStand stand : this.spawned) {
            if (stand == null || stand.isDead() || !stand.isValid()) {
                this.refresh();
                return;
            }
        }
        if (this.spawned.isEmpty() && !this.plugin.getCrateManager().getCrates().isEmpty()) {
            this.refresh();
        }
    }

    public void despawnAll() {
        for (ArmorStand stand : this.spawned) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        this.spawned.clear();
    }

    /**
     * "world,x,y,z" -> a Location, or null when that world is not loaded.
     */
    private Location parse(String raw) {
        String[] parts = raw.split("[,;]");
        if (parts.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * The lines for one crate: its own override, else the shared list.
     */
    private List<String> linesFor(Crate crate) {
        List<String> lines = this.plugin.getMainConfig().getConfiguration()
                .getStringList("HOLOGRAMS.CRATES." + crate.getName() + ".LINES");
        if (lines == null || lines.isEmpty()) {
            lines = this.plugin.getMainConfig().getConfiguration().getStringList("HOLOGRAMS.LINES");
        }
        if (lines == null || lines.isEmpty()) {
            lines = Arrays.asList(new String[]{"&c&l%crate% CRATE", "&8► &7Right click to &aOpen &8◄"});
        }
        return lines;
    }

    private void spawn(Crate crate, Location block) {
        List<String> lines = this.linesFor(crate);
        double height = this.plugin.getMainConfig().getConfiguration().getDouble("HOLOGRAMS.HEIGHT", 2.6);
        double spacing = this.plugin.getMainConfig().getConfiguration().getDouble("HOLOGRAMS.LINE-SPACING", 0.28);
        World world = block.getWorld();
        if (world == null) {
            return;
        }

        // Clear anything left behind by an earlier run before drawing again.
        this.cleanupNear(block);

        for (int index = 0; index < lines.size(); ++index) {
            String text = CC.translate(lines.get(index)
                    .replace("%crate%", crate.getName())
                    .replace("%crate_upper%", crate.getName().toUpperCase(java.util.Locale.ROOT))
                    .replace("%rewards%", String.valueOf(crate.getRewards().size())));
            Location at = new Location(world, block.getBlockX() + 0.5,
                    block.getBlockY() + height - (index * spacing), block.getBlockZ() + 0.5);
            ArmorStand stand = world.spawn(at, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.setCustomName(text);
            stand.setCustomNameVisible(!text.trim().isEmpty());
            this.spawned.add(stand);
        }
    }

    /**
     * Removes stray hologram stands around a crate, so a reload never stacks two sets of text.
     */
    private void cleanupNear(Location block) {
        World world = block.getWorld();
        if (world == null) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(block.clone().add(0.5, 2.0, 0.5), 2.0, 4.0, 2.0)) {
            if (!(entity instanceof ArmorStand)) {
                continue;
            }
            ArmorStand stand = (ArmorStand) entity;
            if (stand.isMarker() && !stand.isVisible() && stand.getCustomName() != null) {
                stand.remove();
            }
        }
    }
}
