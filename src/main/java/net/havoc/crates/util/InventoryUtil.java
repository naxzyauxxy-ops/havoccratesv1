package net.havoc.crates.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helpers for splitting a bulk purchase into stacks and pushing it into a player's inventory.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Splits {@code amount} copies of {@code base} into properly sized stacks.
     * Items that do not stack (armour, tools, shulkers...) become {@code amount} separate items,
     * which is what lets an armour purchase fill the inventory instead of being capped at one.
     */
    public static List<ItemStack> split(ItemStack base, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        if (base == null || amount <= 0) {
            return stacks;
        }
        int maxStack = Math.max(1, base.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int size = Math.min(maxStack, remaining);
            ItemStack clone = base.clone();
            clone.setAmount(size);
            stacks.add(clone);
            remaining -= size;
        }
        return stacks;
    }

    /**
     * How many of {@code base} the player can still fit in their storage slots.
     */
    public static int countFreeSpace(Player player, ItemStack base) {
        int maxStack = Math.max(1, base.getMaxStackSize());
        int free = 0;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (ItemStack content : storage) {
            if (content == null || content.getType().isAir()) {
                free += maxStack;
            } else if (maxStack > 1 && content.isSimilar(base)) {
                free += Math.max(0, maxStack - content.getAmount());
            }
        }
        return free;
    }

    /**
     * A throwaway copy of the player's storage slots, so delivery can be tested without touching
     * the real inventory.
     */
    private static Inventory snapshot(Player player) {
        Inventory dummy = Bukkit.createInventory(null, 36);
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < Math.min(36, storage.length); slot++) {
            ItemStack content = storage[slot];
            dummy.setItem(slot, content == null ? null : content.clone());
        }
        return dummy;
    }

    /**
     * How many copies of {@code base} actually fit right now.
     *
     * <p>This is what makes a bulk buy fill the inventory instead of being refused: 64 chestplates
     * need 64 free slots and a player only has 36, so this returns what genuinely fits (topping up
     * matching stacks first, then using empty slots) and never counts a slot that already holds
     * something else.
     *
     * @return the number of items that can be delivered, 0 when there is no room at all.
     */
    public static int fitCount(Player player, ItemStack base, int amount) {
        if (base == null || amount <= 0) {
            return 0;
        }
        Inventory dummy = snapshot(player);
        int placed = 0;
        for (ItemStack stack : split(base, amount)) {
            Map<Integer, ItemStack> leftover = dummy.addItem(stack.clone());
            int notPlaced = 0;
            for (ItemStack remaining : leftover.values()) {
                notPlaced += remaining.getAmount();
            }
            placed += stack.getAmount() - notPlaced;
            if (notPlaced > 0) {
                break;
            }
        }
        return Math.max(0, Math.min(amount, placed));
    }

    /**
     * Tests whether every stack fits, without touching the real inventory.
     */
    public static boolean fits(Player player, Collection<ItemStack> stacks) {
        Inventory dummy = snapshot(player);
        for (ItemStack stack : stacks) {
            Map<Integer, ItemStack> leftover = dummy.addItem(stack.clone());
            if (!leftover.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gives the stacks to the player. Anything that does not fit is dropped at their feet
     * when {@code dropOverflow} is true, otherwise it is returned.
     *
     * @return the stacks that could not be delivered.
     */
    public static List<ItemStack> give(Player player, Collection<ItemStack> stacks, boolean dropOverflow) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack stack : stacks) {
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(stack.clone());
            leftovers.addAll(remaining.values());
        }
        if (dropOverflow && !leftovers.isEmpty()) {
            Location location = player.getLocation();
            for (ItemStack leftover : leftovers) {
                player.getWorld().dropItemNaturally(location, leftover);
            }
            return new ArrayList<>();
        }
        return leftovers;
    }
}
