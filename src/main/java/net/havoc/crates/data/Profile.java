/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.Object
 *  java.lang.String
 *  java.util.HashMap
 *  java.util.Iterator
 *  java.util.Map
 *  java.util.UUID
 */
package net.havoc.crates.data;

import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Profile {
    private final UUID uuid;
    private final Map<String, Integer> keys = new HashMap();
    private boolean alerts = true;
    private boolean dirty;

    public Profile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public boolean isAlerts() {
        return this.alerts;
    }

    public void setAlerts(boolean alerts) {
        this.alerts = alerts;
        this.dirty = true;
    }

    public Map<String, Integer> getKeys() {
        return this.keys;
    }

    public int getKeyAmount(String crate) {
        Integer value = (Integer)this.keys.get(crate.toLowerCase());
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
        this.setKeys(crate, this.getKeyAmount(crate) + amount);
    }

    public boolean takeKeys(String crate, int amount) {
        int current = this.getKeyAmount(crate);
        if (current < amount) {
            return false;
        }
        this.setKeys(crate, current - amount);
        return true;
    }

    public int getTotalKeys() {
        int total = 0;
        Iterator iterator = this.keys.values().iterator();
        while (iterator.hasNext()) {
            int value = (Integer)iterator.next();
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
