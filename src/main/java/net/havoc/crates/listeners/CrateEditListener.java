/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.InventoryHolder
 */
package net.havoc.crates.listeners;

import java.lang.Object;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.ui.CrateEditMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class CrateEditListener
implements Listener {
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
        CrateEditMenu menu = (CrateEditMenu)holder;
        menu.save();
        if (event.getPlayer() instanceof Player) {
            this.plugin.message((CommandSender)((Player)event.getPlayer()), "CRATE_SAVED", "%crate%", menu.getCrate().getDisplayName());
        }
    }
}
