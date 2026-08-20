package net.havoc.crates.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory events to the matching {@link Menu}.
 */
public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Menu menu = ((MenuHolder) holder).getMenu();

        boolean topInventory = event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize();
        if (!topInventory) {
            if (!menu.allowPlayerInventoryClicks()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        Button button = menu.getRenderedButtons().get(event.getRawSlot());
        if (button == null) {
            return;
        }
        button.clicked(player, event.getRawSlot(), event.getClick());
        if (button.shouldUpdate(player, event.getRawSlot(), event.getClick())) {
            menu.update(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        Menu menu = ((MenuHolder) holder).getMenu();
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
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
        ((MenuHolder) holder).getMenu().onClose((Player) event.getPlayer());
    }
}
