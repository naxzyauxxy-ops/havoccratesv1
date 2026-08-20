package net.havoc.crates.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for every GUI in the plugin.
 */
public abstract class Menu {

    private final Map<Integer, Button> buttons = new HashMap<>();
    private Inventory inventory;

    public abstract String getTitle(Player player);

    public abstract int getSize(Player player);

    public abstract Map<Integer, Button> getButtons(Player player);

    /**
     * @return true when the player is allowed to click their own inventory while this menu is open.
     */
    public boolean allowPlayerInventoryClicks() {
        return false;
    }

    public void onClose(Player player) {
        // no-op by default
    }

    public Map<Integer, Button> getRenderedButtons() {
        return this.buttons;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void openMenu(Player player) {
        int size = getSize(player);
        MenuHolder holder = new MenuHolder(this);
        this.inventory = Bukkit.createInventory(holder, size, getTitle(player));
        holder.setInventory(this.inventory);
        render(player);
        player.openInventory(this.inventory);
    }

    /**
     * Re-draws the contents of an already opened menu.
     */
    public void update(Player player) {
        if (this.inventory == null) {
            openMenu(player);
            return;
        }
        render(player);
        player.updateInventory();
    }

    private void render(Player player) {
        this.buttons.clear();
        this.buttons.putAll(getButtons(player));
        int size = this.inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            Button button = this.buttons.get(slot);
            ItemStack item = button == null ? null : button.getButtonItem(player);
            this.inventory.setItem(slot, item);
        }
    }
}
