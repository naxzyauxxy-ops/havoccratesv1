package net.havoc.crates.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A player's virtual key balances, keyed by crate name.
 */
public class Profile {

    private final UUID uuid;
    private final Map<String, Integer> keys = new HashMap<>();
    private boolean dirty;

    public Profile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Map<String, Integer> getKeys() {
        return this.keys;
    }

    public int getKeyAmount(String crate) {
        Integer value = this.keys.get(crate.toLowerCase());
        return value == null ? 0 : value;
    }

    public void setKeys(String crate, int amount) {
        String key = crate.toLowerCase();
        if (amount <= 0) {
            this.keys.remove(key);
        } else {
            this.keys.put(key, amount);
        }
        this.dirty = true;
    }

    public void addKeys(String crate, int amount) {
        setKeys(crate, getKeyAmount(crate) + amount);
    }

    /**
     * @return true when the keys were available and have been taken.
     */
    public boolean takeKeys(String crate, int amount) {
        int current = getKeyAmount(crate);
        if (current < amount) {
            return false;
        }
        setKeys(crate, current - amount);
        return true;
    }

    public int getTotalKeys() {
        int total = 0;
        for (int value : this.keys.values()) {
            total += value;
        }
        return total;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
