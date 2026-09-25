/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 */
package net.havoc.crates.listeners;

import java.lang.Object;
import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.ui.CrateViewMenu;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CrateInteractListener
implements Listener {
    private final CratesPlugin plugin;

    public CrateInteractListener(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != null && !event.getHand().name().equals("HAND")) {
            return;
        }
        Crate crate = this.plugin.getCrateManager().getCrate(event.getClickedBlock().getLocation());
        if (crate == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        new CrateViewMenu(this.plugin, crate).openMenu(player);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Crate crate = this.plugin.getCrateManager().getCrate(block.getLocation());
        if (crate == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("havoccrates.admin")) {
            event.setCancelled(true);
            this.plugin.message((CommandSender)event.getPlayer(), "CRATE_PROTECTED", "%crate%", crate.getDisplayName());
            return;
        }
        crate.removeLocation(block.getLocation());
        this.plugin.getCrateManager().save();
        this.plugin.message((CommandSender)event.getPlayer(), "CRATE_UNSET", "%crate%", crate.getDisplayName());
    }
}
