/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.Math
 *  java.lang.Object
 *  java.lang.String
 *  java.util.ArrayList
 *  java.util.Arrays
 *  java.util.Collection
 *  java.util.List
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package net.havoc.crates.util;

import java.lang.Math;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.havoc.crates.util.CC;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            material = Material.matchMaterial((String)materialName.toUpperCase());
        }
        return new ItemBuilder(material == null ? fallback : material);
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(Math.max((int)1, (int)Math.min((int)64, (int)amount)));
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

    public ItemBuilder lore(String ... lines) {
        return this.lore((List<String>)new ArrayList((Collection)Arrays.asList((Object[])lines)));
    }

    public ItemBuilder appendLore(List<String> lines) {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            ArrayList lore = meta.getLore() == null ? new ArrayList() : new ArrayList((Collection)meta.getLore());
            lore.addAll(CC.translate(lines));
            meta.setLore((List)lore);
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder hideAttributes() {
        ItemMeta meta = this.itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
            this.itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return this.itemStack;
    }
}
