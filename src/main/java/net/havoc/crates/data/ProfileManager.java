/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Class
 *  java.lang.ClassNotFoundException
 *  java.lang.IllegalStateException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.Map
 *  java.util.UUID
 *  java.util.concurrent.ConcurrentHashMap
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package net.havoc.crates.data;

import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.data.Profile;
import net.havoc.crates.data.storage.FlatfileStorage;
import net.havoc.crates.data.storage.KeyStorage;
import net.havoc.crates.data.storage.SqlStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ProfileManager {
    private final CratesPlugin plugin;
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap();
    private final KeyStorage storage;

    public ProfileManager(CratesPlugin plugin) {
        this.plugin = plugin;
        this.storage = this.createStorage();
        this.storage.init();
    }

    private KeyStorage createStorage() {
        String type = this.plugin.getMainConfig().getString("DATABASE.TYPE", "FLATFILE").toUpperCase();
        try {
            if (type.equals("MYSQL")) {
                Class.forName((String)"com.mysql.cj.jdbc.Driver");
                return new SqlStorage(this.plugin, true);
            }
            if (type.equals("SQLITE")) {
                Class.forName((String)"org.sqlite.JDBC");
                return new SqlStorage(this.plugin, false);
            }
        }
        catch (ClassNotFoundException | IllegalStateException exception) {
            this.plugin.getLogger().warning("Storage type " + type + " is unavailable (" + exception.getMessage() + "), falling back to flatfile.");
        }
        return new FlatfileStorage(this.plugin);
    }

    public Profile getProfile(UUID uuid) {
        return (Profile)this.profiles.computeIfAbsent(uuid, id -> {
            Profile profile = new Profile((UUID)id);
            this.storage.load(profile);
            return profile;
        });
    }

    public Profile getProfile(Player player) {
        return this.getProfile(player.getUniqueId());
    }

    public void loadAsync(UUID uuid) {
        if (this.profiles.containsKey(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            Profile profile = new Profile(uuid);
            this.storage.load(profile);
            this.profiles.putIfAbsent(uuid, profile);
        });
    }

    public void saveAsync(Profile profile) {
        profile.setDirty(false);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.storage.save(profile));
    }

    public Profile readFromStorage(UUID uuid) {
        Profile profile = new Profile(uuid);
        this.storage.load(profile);
        return profile;
    }

    public String getStorageName() {
        return this.storage.getClass().getSimpleName();
    }

    public void unload(UUID uuid) {
        Profile profile = (Profile)this.profiles.remove(uuid);
        if (profile != null) {
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.storage.save(profile));
        }
    }

    public void saveAll() {
        for (Profile profile : this.profiles.values()) {
            this.storage.save(profile);
        }
    }

    public void shutdown() {
        this.saveAll();
        this.storage.close();
    }
}
