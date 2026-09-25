/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Math
 *  java.lang.Object
 *  java.util.ArrayList
 *  java.util.Collection
 *  java.util.HashMap
 *  java.util.List
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package net.havoc.crates.util;

import java.lang.Math;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static List<ItemStack> split(ItemStack base, int amount) {
        int size;
        ArrayList stacks = new ArrayList();
        if (base == null || amount <= 0) {
            return stacks;
        }
        int maxStack = Math.max((int)1, (int)base.getMaxStackSize());
        for (int remaining = amount; remaining > 0; remaining -= size) {
            size = Math.min((int)maxStack, (int)remaining);
            ItemStack clone = base.clone();
            clone.setAmount(size);
            stacks.add(clone);
        }
        return stacks;
    }

    public static int countFreeSpace(Player player, ItemStack base) {
        ItemStack[] storage;
        int maxStack = Math.max((int)1, (int)base.getMaxStackSize());
        int free = 0;
        for (ItemStack content : storage = player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                free += maxStack;
                continue;
            }
            if (maxStack <= 1 || !content.isSimilar(base)) continue;
            free += Math.max((int)0, (int)(maxStack - content.getAmount()));
        }
        return free;
    }

    private static Inventory snapshot(Player player) {
        Inventory dummy = Bukkit.createInventory(null, (int)36);
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < Math.min((int)36, (int)storage.length); ++slot) {
            ItemStack content = storage[slot];
            dummy.setItem(slot, content == null ? null : content.clone());
        }
        return dummy;
    }

    public static int fitCount(Player player, ItemStack base, int amount) {
        if (base == null || amount <= 0) {
            return 0;
        }
        Inventory dummy = InventoryUtil.snapshot(player);
        int placed = 0;
        for (ItemStack stack : InventoryUtil.split(base, amount)) {
            HashMap<Integer, ItemStack> leftover = dummy.addItem(new ItemStack[]{stack.clone()});
            int notPlaced = 0;
            for (ItemStack remaining : leftover.values()) {
                notPlaced += remaining.getAmount();
            }
            placed += stack.getAmount() - notPlaced;
            if (notPlaced <= 0) continue;
            break;
        }
        return Math.max((int)0, (int)Math.min((int)amount, (int)placed));
    }

    public static boolean fits(Player player, Collection<ItemStack> stacks) {
        Inventory dummy = InventoryUtil.snapshot(player);
        for (ItemStack stack : stacks) {
            HashMap leftover = dummy.addItem(new ItemStack[]{stack.clone()});
            if (leftover.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public static List<ItemStack> give(Player player, Collection<ItemStack> stacks, boolean dropOverflow) {
        ArrayList<ItemStack> leftovers = new ArrayList<ItemStack>();
        for (ItemStack stack : stacks) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(new ItemStack[]{stack.clone()});
            leftovers.addAll(remaining.values());
        }
        if (dropOverflow && !leftovers.isEmpty()) {
            Location location = player.getLocation();
            for (ItemStack leftover : leftovers) {
                player.getWorld().dropItemNaturally(location, leftover);
            }
            return new ArrayList<ItemStack>();
        }
        return leftovers;
    }
}
