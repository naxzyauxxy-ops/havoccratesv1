/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Object
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.menu;

import java.lang.Object;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class Button {
    public abstract ItemStack getButtonItem(Player var1);

    public void clicked(Player player, int slot, ClickType clickType) {
    }

    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return false;
    }
}
