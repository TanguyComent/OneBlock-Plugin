package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;

public class guis {
    public static Inventory islandInformations(Player p){
        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p));
        inventory.setItem(13, GuisItems.islandInfoItem(p));
        inventory.setItem(29, GuisItems.islandMembersAcces());
        inventory.setItem(33, GuisItems.settingsMenu());
        return inventory;
    }

    public static Inventory islandMembers(Player p){
        Inventory inventory = Bukkit.createInventory(null, 18, ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p));
        for(String playerName : MySQL.getIslandPlayers(MySQL.getIslandNameByPlayer(p))){
            inventory.addItem(getPlayerHead(playerName));
        }
        return inventory;
    }

    public static Inventory warpsMenu(int page){
        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.RED + "" + ChatColor.BOLD + "Menu des warps");
        for(int i = 0; i <= 9; i++){inventory.setItem(i, GuisItems.glass());}
        inventory.setItem(17, GuisItems.glass());
        inventory.setItem(18, GuisItems.glass());
        inventory.setItem(26, GuisItems.glass());
        inventory.setItem(27, GuisItems.glass());
        inventory.setItem(35, GuisItems.glass());
        for(int i = 36; i <= 44; i++){inventory.setItem(i, GuisItems.glass());}
        inventory.setItem(42, GuisItems.nextWarpPage(0));
        inventory.setItem(38, GuisItems.previousWarpPage(0));
        for(String warps : MySQL.getIslandWarpsName()){
            for(int i = page*48; i<=1; i++) continue;
            ItemBuilder it = new ItemBuilder(Material.PAPER);
            it.addStringComponent("actions", "warps");
            it.setName(warps);
            it.setLore(ChatColor.GREEN + "Click pour aller a ce warp !");
            inventory.addItem(it.toItemStack());
        }
        return inventory;
    }

    public static ItemStack getPlayerHead(String playerName){
        ItemStack it = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwner(playerName);
        it.setItemMeta(meta);
        ItemMeta info = it.getItemMeta();
        info.setDisplayName(ChatColor.GOLD + playerName);
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GREEN + "Click pour modifier les infos de ce joueur");
        info.setLore(lore);
        it.setItemMeta(info);
        return it;
    }
}