package net.havoc.crates.data.storage;

import net.havoc.crates.data.Profile;

/**
 * Backend used to persist key balances.
 */
public interface KeyStorage {

    void init();

    void load(Profile profile);

    void save(Profile profile);

    void close();
}
