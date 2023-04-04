package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class GuisItems {

    public static ItemStack islandInfoItem(Player p){
        ItemBuilder it = new ItemBuilder(Material.PAPER);
        it.setName(ChatColor.RED + "Island Information (" + MySQL.getIslandNameByPlayer(p) + ")");
        it.setLore(
                   ChatColor.GOLD + "Player : " + ChatColor.RED + p.getName()
                 , ChatColor.GOLD + "Grade : " + ChatColor.RED + MySQL.getPlayerGrade(p)
                 , ChatColor.GOLD + "Level : " + ChatColor.RED + MySQL.getIslandLevelByPlayer(p)
                 , ChatColor.GOLD + "Prestige : " + ChatColor.RED + MySQL.getIslandPrestigeByPlayer(p));
        return it.toItemStack();
    }

    public static ItemStack glass(){
        ItemBuilder it = new ItemBuilder(Material.BLACK_STAINED_GLASS);
        it.setName(ChatColor.BLACK + "rien");
        return it.toItemStack();
    }

    public static ItemStack nextWarpPage(int actualPage){
        ItemBuilder it = new ItemBuilder(Material.PAPER);
        it.setName(ChatColor.RED + "-->");
        it.addIntComponent("warppage", actualPage);
        it.addStringComponent("actions", "nextwarppage");
        return it.toItemStack();
    }

    public static ItemStack previousWarpPage(int actualPage){
        ItemBuilder it = new ItemBuilder(Material.PAPER);
        it.setName(ChatColor.RED + "<--");
        it.addIntComponent("warppage", actualPage);
        it.addStringComponent("actions", "previouswarppage");
        return it.toItemStack();
    }

    public static ItemStack settingsMenu(){
        ItemBuilder it = new ItemBuilder(Material.PAPER);
        it.setName(ChatColor.RED + "Configuration de l'is");
        it.setLore(ChatColor.RED + "Click pour acceder a ce menu");

        return it.toItemStack();
    }

    public static ItemStack islandMembersAcces(){
        ItemBuilder it = new ItemBuilder(Material.PAPER);
        it.setName(ChatColor.RED + "Liste des membres");
        it.setLore(ChatColor.RED + "Click pour acceder a ce menu");
        it.addStringComponent("actions", "members");

        return it.toItemStack();
    }
}
