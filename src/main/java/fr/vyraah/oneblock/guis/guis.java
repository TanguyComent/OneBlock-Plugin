package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
        for(int i = 0; i <= 9; i++){inventory.setItem(i, GuisItems.orangeGlass());}
        inventory.setItem(17, GuisItems.orangeGlass());
        inventory.setItem(18, GuisItems.orangeGlass());
        inventory.setItem(26, GuisItems.orangeGlass());
        inventory.setItem(27, GuisItems.orangeGlass());
        inventory.setItem(35, GuisItems.orangeGlass());
        for(int i = 36; i <= 44; i++){inventory.setItem(i, GuisItems.orangeGlass());}
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
        inv.setItem(0, GuisItems.orangeGlass());
        inv.setItem(1, GuisItems.orangeGlass());
        inv.setItem(7, GuisItems.orangeGlass());
        inv.setItem(8, GuisItems.orangeGlass());
        inv.setItem(9, GuisItems.orangeGlass());
        inv.setItem(17, GuisItems.orangeGlass());
        inv.setItem(53, GuisItems.orangeGlass());
        inv.setItem(52, GuisItems.orangeGlass());
        inv.setItem(46, GuisItems.orangeGlass());
        inv.setItem(45, GuisItems.orangeGlass());
        inv.setItem(44, GuisItems.orangeGlass());
        inv.setItem(36, GuisItems.orangeGlass());
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

    public static Inventory obPhases(Player p){
        Inventory inv = Bukkit.createInventory(null, 45, "§6§lPhases");
        inv.setItem(28, GuisItems.phaseBuilder(p, Material.GRASS_BLOCK, 0, "§2Plaine"));
        inv.setItem(19, GuisItems.phaseBuilder(p, Material.OAK_LOG, 1000, "§2Foret"));
        inv.setItem(10, GuisItems.phaseBuilder(p, Material.DARK_OAK_LOG, 2000, "§2Foret noir"));
        inv.setItem(11, GuisItems.phaseBuilder(p, Material.ACACIA_LOG, 3000, "§2Savane"));
        inv.setItem(12, GuisItems.phaseBuilder(p, Material.JUNGLE_LOG, 4000, "§2Jungle"));
        inv.setItem(21, GuisItems.phaseBuilder(p, Material.SNOW_BLOCK, 5000, "§2Neige"));
        inv.setItem(30, GuisItems.phaseBuilder(p, Material.PACKED_ICE, 6000, "§2Glace"));
        inv.setItem(31, GuisItems.phaseBuilder(p, Material.SAND, 7000, "§2Desert"));
        inv.setItem(32, GuisItems.phaseBuilder(p, Material.ORANGE_TERRACOTTA, 8000, "§2Canione"));
        inv.setItem(23, GuisItems.phaseBuilder(p, Material.WATER_BUCKET, 9000, "§2Ocean"));
        inv.setItem(14, GuisItems.phaseBuilder(p, Material.CRIMSON_STEM, 10000, "§2Nether rouge"));
        inv.setItem(15, GuisItems.phaseBuilder(p, Material.WARPED_STEM, 11000, "§2Nether bleu"));
        inv.setItem(16, GuisItems.phaseBuilder(p, Material.BASALT, 12000, "§2Basalt"));
        inv.setItem(25, GuisItems.phaseBuilder(p, Material.END_STONE, 13000, "§2Ender"));
        inv.setItem(34, GuisItems.phaseBuilder(p, Material.RED_MUSHROOM, 14000, "§2Champignon"));
        for(int i = 0; i <= 44; i++){
            if(inv.getItem(i) == null){
                inv.setItem(i, GuisItems.orangeGlass());
            }
        }
        return inv;
    }

    public static Inventory obPrestige(Player p){
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lPrestige");
        inv.setItem(0, GuisItems.orangeGlass());
        inv.setItem(1, GuisItems.orangeGlass());
        inv.setItem(2, GuisItems.yellowGlass());
        inv.setItem(3, GuisItems.yellowGlass());
        inv.setItem(4, GuisItems.yellowGlass());
        inv.setItem(5, GuisItems.yellowGlass());
        inv.setItem(6, GuisItems.yellowGlass());
        inv.setItem(7, GuisItems.orangeGlass());
        inv.setItem(8, GuisItems.orangeGlass());
        inv.setItem(9, GuisItems.orangeGlass());
        inv.setItem(17, GuisItems.orangeGlass());
        inv.setItem(18, GuisItems.yellowGlass());
        inv.setItem(20, GuisItems.prestigeBuilder(p, 1));
        inv.setItem(22, GuisItems.prestigeBuilder(p, 3));
        inv.setItem(24, GuisItems.prestigeBuilder(p, 5));
        inv.setItem(26, GuisItems.yellowGlass());
        inv.setItem(27, GuisItems.yellowGlass());
        inv.setItem(30, GuisItems.prestigeBuilder(p, 2));
        inv.setItem(32, GuisItems.prestigeBuilder(p, 4));
        inv.setItem(35, GuisItems.yellowGlass());
        inv.setItem(36, GuisItems.orangeGlass());
        inv.setItem(44, GuisItems.orangeGlass());
        inv.setItem(45, GuisItems.orangeGlass());
        inv.setItem(46, GuisItems.orangeGlass());
        inv.setItem(47, GuisItems.yellowGlass());
        inv.setItem(48, GuisItems.yellowGlass());
        inv.setItem(49, GuisItems.yellowGlass());
        inv.setItem(50, GuisItems.yellowGlass());
        inv.setItem(51, GuisItems.yellowGlass());
        inv.setItem(52, GuisItems.orangeGlass());
        inv.setItem(53, GuisItems.orangeGlass());
        return inv;
    }

    public static Inventory prestigeUpInv(int prestige){
        Inventory inv = Bukkit.createInventory(null, 54, "");
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