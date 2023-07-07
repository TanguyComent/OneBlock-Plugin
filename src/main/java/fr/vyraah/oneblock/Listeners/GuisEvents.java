package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.guis.guis;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class GuisEvents implements Listener {
    public static HashMap<UUID, ArrayList<Object>> shopInCreation = new HashMap<>();

    @EventHandler
    public void inventoryClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p) {
            //Liste des nom d'inv pour eviter l'interaction avec
            String islandName = MySQL.getIslandNameByPlayer(p.getName());
            ArrayList<String> inventoryNames = new ArrayList<>();
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + islandName);
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + "Menu des warps");
            inventoryNames.add("§4Menu de modération");
            inventoryNames.add("§6§lPhases");
            inventoryNames.add("§6§lPrestige");
            inventoryNames.add("§6§lPassage au prestige");
            inventoryNames.add("§6§lPassage au prestige " + (MySQL.getIslandPrestigeByPlayer(p) + 1));
            inventoryNames.add("§6§lQuêtes journalières");
            inventoryNames.add("§6§lShop action");
            inventoryNames.add("§6§lItem selection");
            if(inventoryNames.contains(e.getView().getTitle())){
                e.setCancelled(true);
                if(e.getCurrentItem() != null){
                    try{
                        String itName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("actions"), PersistentDataType.STRING);
                        switch(itName) {
                            case "members" -> {
                                p.openInventory(guis.islandMembers(p));
                            }
                            case "warps" -> {
                                String warpName = e.getCurrentItem().getItemMeta().getDisplayName();
                                p.performCommand("is warp " + warpName);
                            }
                            case "kickplayer" -> {
                                String playerName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("playername"), PersistentDataType.STRING);
                                p.performCommand("is kick " + playerName);
                            }
                            case "manageplayerpannel" -> {
                                String playerName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("playername"), PersistentDataType.STRING);
                                p.openInventory(guis.managePlayer(playerName));
                            }
                            case "promoteplayer" -> {
                                String playerName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("playername"), PersistentDataType.STRING);
                                p.performCommand("is promote " + playerName);
                                p.openInventory(guis.managePlayer(playerName));
                            }
                            case "remoteplayer" -> {
                                String playerName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("playername"), PersistentDataType.STRING);
                                p.performCommand("is remote " + playerName);
                                p.openInventory(guis.managePlayer(playerName));
                            }
                            case "inviteplayer" -> {
                                String playerName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("playername"), PersistentDataType.STRING);
                                p.performCommand("is invite " + playerName);
                            }
                            case "selectphase" -> {
                                String newPhase = e.getCurrentItem().getItemMeta().getDisplayName().replace("§", "").replace("2", "");
                                try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                                    statement.execute("UPDATE t_island SET active_phase='" + newPhase + "' WHERE name='" + islandName + "';");
                                }
                                p.sendMessage("§2Vous avez bien sélectionner la phase §e" + newPhase + " §2!");
                            }
                            case "selectprestige" -> {
                                int newPrestige = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("prestige"), PersistentDataType.INTEGER);
                                try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                                    statement.execute("UPDATE t_island SET active_prestige=" + newPrestige + " WHERE name='" + islandName + "';");
                                }
                                p.sendMessage("§2Vous avez bien sélectionner le prestige §e" + newPrestige + " §2!");
                            }
                            case "unlockprestigegui" -> {
                                int prestige = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("prestige"), PersistentDataType.INTEGER);
                                p.openInventory(guis.prestigeUpInv(prestige, p));
                            }
                            case "unlockprestige" -> {
                                boolean firstCond = (e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("firstcond"), PersistentDataType.INTEGER) == 1) ? true : false;
                                boolean secondCond = (e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("secondcond"), PersistentDataType.INTEGER) == 1) ? true : false;
                                boolean thirdCond = (e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("thirdcond"), PersistentDataType.INTEGER) == 1) ? true : false;
                                if(!firstCond || !secondCond || !thirdCond){
                                    p.sendMessage(Main.prefix + "§4Vous ne remplissez pas toutes les conditions");
                                    return;
                                }

                                int price = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("price"), PersistentDataType.INTEGER);
                                int balance = MySQL.getObBankMoney(MySQL.getIslandNameByPlayer(p.getName()));
                                int newBalance = balance - price;

                                int prestige = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("prestige"), PersistentDataType.INTEGER);
                                try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                                    statement.execute("UPDATE t_island SET bank=" + newBalance + " WHERE name='" + islandName + "';");
                                    statement.execute("UPDATE t_island SET prestige_level=" + prestige + " WHERE name='" + islandName + "';");
                                    statement.execute("UPDATE t_island SET active_prestige=" + prestige + " WHERE name='" + islandName + "';");
                                }
                                p.sendMessage(Main.prefix + "Vous êtes désormais au prestige " + prestige);
                                p.openInventory(guis.obPrestige(p));
                            }
                            case "valider" -> {
                                switch(MySQL.getDailyQuestId(islandName)){
                                    case 1, 2, 3, 4, 5, 6, 7, 8 -> {
                                        MySQL.alterBankSold(islandName, 75000, 1);
                                        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                                            statement.execute("UPDATE t_island_daily_quest SET rewarded=1 WHERE island_name='" + islandName + "';");
                                            statement.execute("UPDATE t_island SET completed_daily_objective_number=" + (MySQL.getObQuestNbr(islandName) + 1) + " WHERE name='" + islandName + "';");
                                        }
                                        p.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));
                                        p.openInventory(guis.dailyQuestInv(p));
                                        p.sendMessage(Main.prefix + "§6§lGG! §r§2Vous avez complété une quête d'ile journalière, §oà demain");
                                        p.sendMessage(Main.prefix + "§e75K §2ont bien été ajouté à votre bank d'ile");
                                        p.sendMessage(Main.prefix + "§2Vous avez reçu §e64 §2diamant");
                                    }
                                }
                            }
                            case "toshopcreationsteptwo" -> {
                                int buyOrSell = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("buyorsell"), PersistentDataType.INTEGER);
                                ArrayList<Object> shopInfos = new ArrayList<>();
                                shopInfos.add(buyOrSell);
                                shopInCreation.put(p.getUniqueId(), shopInfos);
                                p.openInventory(guis.chooseItem(p));
                            }
                            case "toshopcreationstepthree" -> {
                                ArrayList<Object> shopInfos = shopInCreation.get(p.getUniqueId());
                                ItemStack toSell = e.getCurrentItem();
                                toSell.setAmount(1);
                                shopInfos.add(toSell);
                                if(!e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(NamespacedKey.fromString("sellshop"), PersistentDataType.STRING)) {
                                    p.closeInventory();
                                    p.sendMessage(Main.prefix + "Veuillez écrire le prix de vente/achat dans le chat");
                                    p.sendMessage(Main.prefix + "Pour annuler la création du shop, veuillez écrir 'cancel' dans le chat");
                                    p.getPersistentDataContainer().set(NamespacedKey.fromString("shop-price"), PersistentDataType.STRING, "");
                                }else{
                                    p.sendMessage(Main.prefix + "§4Item invalide");
                                }
                            }
                            default -> {}
                        }
                    }catch(Exception ex){throw new RuntimeException(ex);}
                }
            }
        }
    }
}
