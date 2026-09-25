/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 */
package net.havoc.crates.data.storage;

import java.lang.Object;
import net.havoc.crates.data.Profile;

public interface KeyStorage {
    public void init();

    public void load(Profile var1);

    public void save(Profile var1);

    public void close();
}
