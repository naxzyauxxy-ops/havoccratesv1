package net.havoc.crates.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small fluent wrapper around ItemStack creation.
 */
public final class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public static ItemBuilder of(String materialName, Material fallback) {
        Material material = null;
        if (materialName != null) {
            material = Material.matchMaterial(materialName.toUpperCase());
        }
        return new ItemBuilder(material == null ? fallback : material);
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.translate(name));
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            meta.setLore(CC.translate(lore));
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(new ArrayList<>(Arrays.asList(lines)));
    }

    public ItemBuilder appendLore(List<String> lines) {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.addAll(CC.translate(lines));
            meta.setLore(lore);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder hideAttributes() {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return this.itemStack;
    }
}
