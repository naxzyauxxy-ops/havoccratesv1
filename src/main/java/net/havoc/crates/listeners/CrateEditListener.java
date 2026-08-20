package net.havoc.crates.listeners;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.ui.CrateEditMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Persists a crate's rewards when the admin closes the edit GUI.
 */
public class CrateEditListener implements Listener {

    private final CratesPlugin plugin;

    public CrateEditListener(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CrateEditMenu)) {
            return;
        }
        CrateEditMenu menu = (CrateEditMenu) holder;
        menu.save();
        if (event.getPlayer() instanceof Player) {
            this.plugin.message((Player) event.getPlayer(), "CRATE_SAVED",
                    "%crate%", menu.getCrate().getDisplayName());
        }
    }
}
