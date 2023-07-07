package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class PrestigeGuisItems {
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
        boolean nextPrestige = MySQL.getIslandPrestigeByPlayer(p) + 1 == prestige;
        if(prestigeUnlocked){
            it.setLore("§2Click pour sélectionner !");
            it.addStringComponent("actions", "selectprestige");
        }else if(nextPrestige){
            it.setLore("§4Vous n'avez pas débloquer ce prestige", "§4click pour le débloquer");
            it.addStringComponent("actions", "unlockprestigegui");
        }else{
            it.setLore("§4Vous n'avez pas débloquer ce prestige", "§4Veuillez débloquer le précédent avant de pouvoir avoir celui la");
        }
        return it.toItemStack();
    }

    public static ItemStack prestigeUpBuilder(Player p, int prestige){
        ItemBuilder it;
        int obTimesBreak = MySQL.getObTimesBreak(MySQL.getIslandNameByPlayer(p.getName()));
        int dailyQuestAchieve = MySQL.getObQuestNbr(MySQL.getIslandNameByPlayer(p.getName()));
        int islandMoney = MySQL.getObBankMoney(MySQL.getIslandNameByPlayer(p.getName()));
        int obTimesBreakNeeded;
        int dailyQuestAchieveNeeded;
        int islandMoneyNeeded;
        String simplified;
        switch(prestige){
            default -> {
                obTimesBreakNeeded = 4000;
                dailyQuestAchieveNeeded = 2;
                islandMoneyNeeded = 100000;
                simplified = "100K";
                it = new ItemBuilder(Material.IRON_BLOCK);
                it.setName("§6§lPassage au prestige 2");
            }
            case 3 -> {
                obTimesBreakNeeded = 8000;
                dailyQuestAchieveNeeded = 7;
                islandMoneyNeeded = 500000;
                simplified = "500K";
                it = new ItemBuilder(Material.GOLD_BLOCK);
                it.setName("§6§lPassage au prestige 3");
            }
            case 4 -> {
                obTimesBreakNeeded = 15000;
                dailyQuestAchieveNeeded = 12;
                islandMoneyNeeded = 1000000;
                simplified = "1M";
                it = new ItemBuilder(Material.DIAMOND_BLOCK);
                it.setName("§6§lPassage au prestige 4");
            }
            case 5 -> {
                obTimesBreakNeeded = 25000;
                dailyQuestAchieveNeeded = 20;
                islandMoneyNeeded = 2000000;
                simplified = "2M";
                it = new ItemBuilder(Material.NETHERITE_BLOCK);
                it.setName("§6§lPassage au prestige 5");
            }
        }
        it.setLore("§f§l-----------------------------------"
                , "§6§lConditions :"
                , ""
                , ((obTimesBreak >= obTimesBreakNeeded) ? "§2Casser §e§l" + obTimesBreakNeeded + " §r§2fois son OneBlock ✔" : "§4Casser §e§l" + obTimesBreakNeeded + " §r§4fois son OneBlock ❌ §l(" + obTimesBreak + "/" + obTimesBreakNeeded + ")")
                , ((dailyQuestAchieve >= dailyQuestAchieveNeeded) ? "§2Accomplir §e§l" + dailyQuestAchieveNeeded + " §r§2quêtes d'ile journalière ✔" : "§4Accomplir §e§l" + dailyQuestAchieveNeeded + " §r§4quêtes d'ile journalière ❌ §l(" + dailyQuestAchieve + "/" + dailyQuestAchieveNeeded + ")")
                , (islandMoney >= islandMoneyNeeded) ? "§2Prix : §e§l" + simplified + "$ §r§2✔" : "§4Prix : §e§l" + simplified + "$ §r§4❌ §l(" + islandMoney + "/" + islandMoneyNeeded + ")"
                , "§f§l-----------------------------------"
                , "§6§oClickez pour débloquer ce prestige");
        it.addIntComponent("firstcond", (obTimesBreak >= obTimesBreakNeeded) ? 1 : 0);
        it.addIntComponent("secondcond", (dailyQuestAchieve >= dailyQuestAchieveNeeded) ? 1 : 0);
        it.addIntComponent("thirdcond", (islandMoney >= islandMoneyNeeded) ? 1 : 0);
        it.addIntComponent("price", islandMoneyNeeded);
        it.addStringComponent("actions", "unlockprestige");
        it.addIntComponent("prestige", prestige);
        return it.toItemStack();
    }
}
