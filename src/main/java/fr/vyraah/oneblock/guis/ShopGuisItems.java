package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ShopGuisItems {

    public static ItemStack buyShop(){
        ItemBuilder it = new ItemBuilder(Material.DIAMOND);
        it.setName("§6§lAchat");
        it.setLore("§eClick pour que ton shop soit un shop d'achat");
        it.addStringComponent("actions", "toshopcreationsteptwo");
        it.addIntComponent("buyorsell", 1);
        return it.toItemStack();
    }

    public static ItemStack sellShop(){
        ItemBuilder it = new ItemBuilder(Material.GOLD_INGOT);
        it.setName("§6§lVente");
        it.setLore("§eClick pour que ton shop soit un shop de vente");
        it.addStringComponent("actions", "toshopcreationsteptwo");
        it.addIntComponent("buyorsell", 0);
        return it.toItemStack();
    }
}
