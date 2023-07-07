package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class QuestGuisItems {
    public static ItemStack completeQuestButton(String islandName, boolean canValid, boolean hasGotRewarded){
        ItemBuilder it = new ItemBuilder(Material.DIAMOND_BLOCK);
        it.setName((canValid) ? "§2§lValider" : "§4§lValider");
        if(hasGotRewarded){
            it.setLore("§2§oVous avez déja récupérer la récompense de la quête d'aujourd'hui !", "§2§oRevenez demain pour une nouvelle quête !");
            return it.toItemStack();
        }
        if(canValid) it.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
        it.addFlag(ItemFlag.HIDE_ENCHANTS);
        it.setLore((canValid) ? "§2§oClick ici pour valider cette quête !" : "§4§oVous devez finir cette quête avant de la valider !");
        it.addStringComponent("island", islandName);
        if(canValid)
            it.addStringComponent("actions", "valider");
        return it.toItemStack();
    }

    public static ItemStack rewards(int questId){
        ItemBuilder it = new ItemBuilder(Material.GOLD_INGOT);
        it.setName("§e§lRécompenses");
        it.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
        it.addFlag(ItemFlag.HIDE_ENCHANTS);
        switch(questId){
            case 1, 2, 3, 4, 5, 6, 7, 8 -> it.setLore("§f§l--------------------------------"
                    , "§f• §e75K$ dans votre bank d'ile"
                    , "§f• §e64 diamant"
                    , "§f• §e+1 quête journalière terminée ! (condition des prestiges)"
                    , "§f§l--------------------------------");
        }
        return it.toItemStack();
    }

    public static ItemStack firstQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("60144"));
        it.setName("§e§lEtrange découverte");
        it.setLore("§f§l--------------------------------"
                , "§eQuel est cet étrange block ???"
                , "§eVisiblement il a la capacité de se régénérer en changeant de forme !"
                , "§eUtilisons cette particularitée à notre avantage"
                , "§f§l---------------------------------"
                , "§e§oCassez 3000 fois votre OneBlock §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/3000)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack secondQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("23757"));
        it.setName("§e§lPauvre fermier");
        it.setLore("§f§l--------------------------------"
                , "§eLe fermier de votre ile a mal au dos"
                , "§ePourriez vous l'aider a récolter ses champs ?"
                , "§f§l---------------------------------"
                , "§e§oFarmez 2000 de blé §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/2000)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack thirdQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("60105"));
        it.setName("§e§lStage avec Bernard");
        it.setLore("§f§l--------------------------------"
                , "§eBonne nouvelle capitaine !"
                , "§eBernard est ok pour vous prendre en stage de pêche !"
                , "§f§l---------------------------------"
                , "§e§oPêchez 500 fois §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/500)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack fourthQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("24179"));
        it.setName("§e§lFait chauffer le four !");
        it.setLore("§f§l--------------------------------"
                , "§eLe forgeron n'as presque plus de minerais !"
                , "§eAide le faisant fondre du fer"
                , "§e§oLe fer fondu ne te seras pas pris"
                , "§f§l---------------------------------"
                , "§e§oFaire fondre 750 minerais de fer §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/750)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack fifthQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Main.head.getItemHead("51562"));
        it.setName("§e§lL'arrivée de l'hiver");
        it.setLore("§f§l--------------------------------"
                , "§eL'hivers approche à grand pas"
                , "§ePour se préparer il nous faut du bois"
                , "§f§l---------------------------------"
                , "§e§oCasser 3000 buches de chêne §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/3000)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack sixthQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Material.ANVIL);
        it.setName("§e§lLe forgeron en colère");
        it.setLore("§f§l--------------------------------"
                , "§eVyraah est bien embêté, le forgeron fait grêve"
                , "§eNos soldats se sont tous fait volées leurs armures"
                , "§ePourrait tu leurs en faire de nouvelles ?"
                , "§e§oLes 100 prochains plastron en fer que tu feras te serront pris"
                , "§f§l---------------------------------"
                , "§e§oCrafter 100 plastrons en fer §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/100)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack seventhQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Material.ZOMBIE_HEAD);
        it.setName("§e§lService millitaire");
        it.setLore("§f§l--------------------------------"
                , "§eVyraah a besoin de toi soldat !"
                , "§eQue ton épée sois ton arme et ta bravoure ta motivation !"
                , "§f§l---------------------------------"
                , "§e§oTuer 500 zombies §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/500)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }

    public static ItemStack eighthQuestObjective(String islandName){
        ItemBuilder it = new ItemBuilder(Material.BOW);
        it.setName("§e§lContre son camp majestueux");
        it.setLore("§f§l--------------------------------"
                , "§eY'en a marre de ces foutus squelettes !"
                , "§eConcentre ta rage et immagine leurs seum"
                , "§esi tu les tues avec leurs propre arme !"
                , "§f§l---------------------------------"
                , "§e§oTuer 200 squelettes avec un arc §r§e§l(" + MySQL.getDailyQuestNumber(islandName) + "/200)"
                , "§f§o[Rappel] : Cette quête est faisable avec l'aide des membres de votre ile !");
        return it.toItemStack();
    }
}
