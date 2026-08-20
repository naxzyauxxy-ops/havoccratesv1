package net.havoc.crates.data;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.storage.FlatfileStorage;
import net.havoc.crates.data.storage.KeyStorage;
import net.havoc.crates.data.storage.SqlStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches {@link Profile}s of online players and persists them through the configured backend.
 */
public class ProfileManager {

    private final CratesPlugin plugin;
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final KeyStorage storage;

    public ProfileManager(CratesPlugin plugin) {
        this.plugin = plugin;
        this.storage = createStorage();
        this.storage.init();
    }

    private KeyStorage createStorage() {
        String type = this.plugin.getMainConfig().getString("DATABASE.TYPE", "FLATFILE").toUpperCase();
        try {
            if (type.equals("MYSQL")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return new SqlStorage(this.plugin, true);
            }
            if (type.equals("SQLITE")) {
                Class.forName("org.sqlite.JDBC");
                return new SqlStorage(this.plugin, false);
            }
        } catch (ClassNotFoundException | IllegalStateException exception) {
            this.plugin.getLogger().warning("Storage type " + type + " is unavailable ("
                    + exception.getMessage() + "), falling back to flatfile.");
        }
        return new FlatfileStorage(this.plugin);
    }

    public Profile getProfile(UUID uuid) {
        return this.profiles.computeIfAbsent(uuid, id -> {
            Profile profile = new Profile(id);
            this.storage.load(profile);
            return profile;
        });
    }

    public Profile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public void loadAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            Profile profile = new Profile(uuid);
            this.storage.load(profile);
            this.profiles.put(uuid, profile);
        });
    }

    public void saveAsync(Profile profile) {
        profile.setDirty(false);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.save(profile));
    }

    public void unload(UUID uuid) {
        Profile profile = this.profiles.remove(uuid);
        if (profile != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.storage.save(profile));
        }
    }

    public void saveAll() {
        for (Profile profile : this.profiles.values()) {
            this.storage.save(profile);
        }
    }

    public void shutdown() {
        saveAll();
        this.storage.close();
    }
}
