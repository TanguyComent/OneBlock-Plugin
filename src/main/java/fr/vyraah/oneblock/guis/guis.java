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
        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p.getName()));
        inventory.setItem(13, GuisItems.islandInfoItem(p));
        inventory.setItem(29, GuisItems.islandMembersAcces());
        inventory.setItem(33, GuisItems.settingsMenu());
        return inventory;
    }

    public static Inventory islandMembers(Player p){
        Inventory inventory = Bukkit.createInventory(null, 18, ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p.getName()));
        for(String playerName : MySQL.getIslandPlayers(MySQL.getIslandNameByPlayer(p.getName()))){
            ItemBuilder it = new ItemBuilder(getPlayerHead(playerName));
            it.addStringComponent("actions", "manageplayerpannel");
            it.addStringComponent("playername", playerName);
            inventory.addItem(it.toItemStack());
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

    public static Inventory managePlayer(String playerName){
        Inventory inv = Bukkit.createInventory(null, 54, "§4Menu de modération");
        inv.setItem(0, GuisItems.glass());
        inv.setItem(1, GuisItems.glass());
        inv.setItem(7, GuisItems.glass());
        inv.setItem(8, GuisItems.glass());
        inv.setItem(9, GuisItems.glass());
        inv.setItem(17, GuisItems.glass());
        inv.setItem(53, GuisItems.glass());
        inv.setItem(52, GuisItems.glass());
        inv.setItem(46, GuisItems.glass());
        inv.setItem(45, GuisItems.glass());
        inv.setItem(44, GuisItems.glass());
        inv.setItem(36, GuisItems.glass());
        inv.setItem(20, GuisItems.promote(playerName));
        inv.setItem(21, GuisItems.remote(playerName));
        inv.setItem(22, GuisItems.lead(playerName));
        inv.setItem(23, GuisItems.kick(playerName));
        inv.setItem(24, GuisItems.invite(playerName));
        inv.setItem(31, GuisItems.ban(playerName));
        inv.setItem(4, GuisItems.playerInformation(playerName));

        return inv;
    }

    public static Inventory isParams(){
        Inventory inv = Bukkit.createInventory(null, 54);
        return inv;
    }

    public static Inventory obPhases(){
        Inventory inv = Bukkit.createInventory(null, 45);
        inv.setItem(28, GuisItems.phaseOne());
        return inv;
    }

    public static Inventory obPrestige(){
        Inventory inv = Bukkit.createInventory(null, 54);
        return inv;
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