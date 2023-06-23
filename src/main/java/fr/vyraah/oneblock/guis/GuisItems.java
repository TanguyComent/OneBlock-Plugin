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
        it.setName(ChatColor.RED + "Island Information (" + MySQL.getIslandNameByPlayer(p.getName()) + ")");
        it.setLore(
                   ChatColor.GOLD + "Player : " + ChatColor.RED + p.getName()
                 , ChatColor.GOLD + "Grade : " + ChatColor.RED + MySQL.getPlayerGrade(p.getName())
                 , ChatColor.GOLD + "Level : " + ChatColor.RED + MySQL.getIslandLevelByPlayer(p)
                 , ChatColor.GOLD + "Prestige : " + ChatColor.RED + MySQL.getIslandPrestigeByPlayer(p));
        return it.toItemStack();
    }

    public static ItemStack orangeGlass(){
        ItemBuilder it = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE);
        it.setName(" ");
        return it.toItemStack();
    }

    public static ItemStack yellowGlass(){
        ItemBuilder it = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE);
        it.setName(" ");
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

    public static ItemStack playerInformation(String playerName){
        ItemBuilder it = new ItemBuilder(guis.getPlayerHead(playerName));
        it.setName("§4" + playerName);
        String grade = switch(MySQL.getPlayerGrade(playerName)){
           case 1 -> "Fondateur";
           case 2 -> "Co-Fondateur";
           case 3 -> "Administrateur";
           case 4 -> "Membre";
           default -> "Visiteur";
        };
        it.setLore("§4Grade : " + grade);
        it.addStringComponent("playername", playerName);

        return it.toItemStack();
    }

    public static ItemStack promote(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("9669"));
        it.setName("§4Promote");
        it.setLore("§2Clickez pour promote " + playerName);
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "promoteplayer");

        return it.toItemStack();
    }

    public static ItemStack remote(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("9675"));
        it.setName("§4Remote");
        it.setLore("§2Clickez pour remote " + playerName);
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "remoteplayer");

        return it.toItemStack();
    }

    public static ItemStack lead(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("61402"));
        it.setName("§4Lead");
        it.setLore("§2Clickez pour transférer le grade de chef à " + playerName);
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "leadplayer");

        return it.toItemStack();
    }

    public static ItemStack kick(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("9671"));
        it.setName("§4Kick");
        it.setLore("§2Clickez pour kick " + playerName + " de votre ile");
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "kickplayer");

        return it.toItemStack();
    }

    public static ItemStack invite(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("21774"));
        it.setName("§4Inviter");
        it.setLore("§2Clickez pour inviter " + playerName + " sur votre ile");
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "inviteplayer");

        return it.toItemStack();
    }

    public static ItemStack ban(String playerName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("29800"));
        it.setName("§4Ban");
        it.setLore("§2Clickez pour bannir " + playerName + " de votre ile");
        it.addStringComponent("playername", playerName);
        it.addStringComponent("actions", "banplayer");

        return it.toItemStack();
    }

    public static ItemStack phaseBuilder(Player p, Material phaseMat, int blockRequired, String name){
        ItemBuilder it;
        if(MySQL.getObTimesBreak(MySQL.getIslandNameByPlayer(p.getName())) >= blockRequired) {
            it = new ItemBuilder(phaseMat);
            it.setName(name);
            it.setLore("§eClick pour sélectionner");
            it.addStringComponent("actions", "selectphase");
        }else{
            it = new ItemBuilder(Material.BARRIER);
            it.setName("§4Lock");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("§4Biome débloquer au §e" + blockRequired + " §4blocs cassés !");
            lore.add("§4Vous devez encore casser §e" + (blockRequired - MySQL.getObTimesBreak(MySQL.getIslandNameByPlayer(p.getName()))) + " §4fois votre");
            lore.add("§4oneblock pour débloquer cette phase !");
            it.setLoreList(lore);
        }
        return it.toItemStack();
    }

    public static ItemStack prestigeBuilder(Player p, int prestige){
        ItemBuilder it = switch(prestige){
            default -> new ItemBuilder(Material.COAL_BLOCK);
            case 2 -> new ItemBuilder(Material.IRON_BLOCK);
            case 3 -> new ItemBuilder(Material.GOLD_BLOCK);
            case 4 -> new ItemBuilder(Material.DIAMOND_BLOCK);
            case 5 -> new ItemBuilder(Material.NETHERITE_BLOCK);
        };
        it.addIntComponent("prestige", prestige);
        it.setName("§ePrestige " + prestige);
        boolean prestigeUnlocked = MySQL.getIslandPrestigeByPlayer(p) >= prestige;
        it.setLore((prestigeUnlocked) ? "§2Click pour sélectionner" : "§4Vous n'avez pas débloquer ce prestige", "§4Click pour le débloquer !");
        return it.toItemStack();
    }

}
