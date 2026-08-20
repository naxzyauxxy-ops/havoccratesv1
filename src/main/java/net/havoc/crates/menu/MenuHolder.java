package net.havoc.crates.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Inventory holder used to identify our own menus.
 */
public class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private Inventory inventory;

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    public Menu getMenu() {
        return this.menu;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
