package net.havoc.crates.ui;

import net.havoc.crates.CratesPlugin;
import net.havoc.crates.crate.Crate;
import net.havoc.crates.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * A plain 54 slot chest the admin can drag rewards into. Contents are saved when it closes.
 */
public class CrateEditMenu implements InventoryHolder {

    private final CratesPlugin plugin;
    private final Crate crate;
    private final Inventory inventory;

    public CrateEditMenu(CratesPlugin plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54,
                CC.translate("&8Editing: &f" + crate.getName()));
        this.inventory.setContents(crate.getContents());
    }

    public Crate getCrate() {
        return this.crate;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }

    public void save() {
        ItemStack[] contents = this.inventory.getContents();
        this.crate.setContents(contents);
        this.plugin.getCrateManager().save();
    }
}
