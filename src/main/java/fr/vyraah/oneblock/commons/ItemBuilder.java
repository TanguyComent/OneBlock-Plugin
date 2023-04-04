package fr.vyraah.oneblock.commons;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        item = new ItemStack(material, amount);
    }

    public ItemBuilder(Material material, int amount, short data) {
        item = new ItemStack(material, amount, data);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemBuilder(ItemStack item, int amount) {
        item.setAmount(amount);
        this.item = item;
    }

    public ItemBuilder setName(String name) {
        meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        meta = item.getItemMeta();
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addStringComponent(String key, String value){
        meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(NamespacedKey.fromString(key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return this;
    }
    public ItemBuilder addIntComponent(String key, int value){
        meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(NamespacedKey.fromString(key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLoreList(List<String> lore) {
        meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setDurability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        item.removeEnchantment(enchantment);
        return this;
    }

    public ItemBuilder hideEnchantment() {
        meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean active) {
        meta = item.getItemMeta();
        meta.setUnbreakable(active);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setSkullOwner(String pseudo) {
        try {
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            skullMeta.setOwner(pseudo);
            item.setItemMeta(skullMeta);
        } catch (ClassCastException expected) {
        }
        return this;
    }

    public ItemBuilder setWoolColor(DyeColor color) {
        if (item.getType().equals(Material.LEGACY_WOOL)) {
            this.item.setDurability(color.getDyeData());
        }
        return this;
    }

    public ItemBuilder setLeatherArmorColor(Color color) {
        try {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) item.getItemMeta();
            leatherArmorMeta.setColor(color);
            item.setItemMeta(leatherArmorMeta);
        } catch (ClassCastException expected) {
        }
        return this;
    }

    public ItemBuilder addFlag(ItemFlag... flag) {
        meta = item.getItemMeta();
        meta.addItemFlags(flag);
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack toItemStack() {
        return item;
    }

}
