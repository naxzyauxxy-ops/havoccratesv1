/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.Object
 *  java.lang.String
 *  java.util.HashMap
 *  java.util.Map
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.menu;

import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class Menu {
    private final Map<Integer, Button> buttons = new HashMap();
    private Inventory inventory;

    public abstract String getTitle(Player var1);

    public abstract int getSize(Player var1);

    public abstract Map<Integer, Button> getButtons(Player var1);

    public boolean allowPlayerInventoryClicks() {
        return false;
    }

    public void onClose(Player player) {
    }

    public Map<Integer, Button> getRenderedButtons() {
        return this.buttons;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void openMenu(Player player) {
        int size = this.getSize(player);
        MenuHolder holder = new MenuHolder(this);
        this.inventory = Bukkit.createInventory((InventoryHolder)holder, (int)size, (String)this.getTitle(player));
        holder.setInventory(this.inventory);
        this.render(player);
        player.openInventory(this.inventory);
    }

    public void update(Player player) {
        if (this.inventory == null) {
            this.openMenu(player);
            return;
        }
        this.render(player);
        player.updateInventory();
    }

    private void render(Player player) {
        this.buttons.clear();
        this.buttons.putAll(this.getButtons(player));
        int size = this.inventory.getSize();
        for (int slot = 0; slot < size; ++slot) {
            Button button = (Button)this.buttons.get(slot);
            ItemStack item = button == null ? null : button.getButtonItem(player);
            this.inventory.setItem(slot, item);
        }
    }
}
