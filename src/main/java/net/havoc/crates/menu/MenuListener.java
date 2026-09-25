/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Integer
 *  java.lang.Object
 *  java.util.Iterator
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.InventoryHolder
 */
package net.havoc.crates.menu;

import java.lang.Integer;
import java.lang.Object;
import java.util.Iterator;
import net.havoc.crates.menu.Button;
import net.havoc.crates.menu.Menu;
import net.havoc.crates.menu.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener
implements Listener {
    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        boolean topInventory;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        Menu menu = ((MenuHolder)holder).getMenu();
        boolean bl = topInventory = event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize();
        if (!topInventory) {
            if (!menu.allowPlayerInventoryClicks()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        Button button = (Button)menu.getRenderedButtons().get(event.getRawSlot());
        if (button == null) {
            return;
        }
        button.clicked(player, event.getRawSlot(), event.getClick());
        if (button.shouldUpdate(player, event.getRawSlot(), event.getClick())) {
            menu.update(player);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        Menu menu = ((MenuHolder)holder).getMenu();
        Iterator iterator = event.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int slot = (Integer)iterator.next();
            if (slot >= event.getInventory().getSize()) continue;
            event.setCancelled(true);
            return;
        }
        if (!menu.allowPlayerInventoryClicks()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        ((MenuHolder)holder).getMenu().onClose((Player)event.getPlayer());
    }
}
