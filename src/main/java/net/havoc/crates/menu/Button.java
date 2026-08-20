package net.havoc.crates.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * A single clickable slot inside a {@link Menu}.
 */
public abstract class Button {

    public abstract ItemStack getButtonItem(Player player);

    public void clicked(Player player, int slot, ClickType clickType) {
        // no-op by default
    }

    /**
     * @return true if clicking this button should re-render the menu.
     */
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return false;
    }
}
