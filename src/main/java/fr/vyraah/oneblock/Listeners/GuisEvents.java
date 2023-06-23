package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.guis.guis;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class GuisEvents implements Listener {

    @EventHandler
    public void inventoryClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p) {
            //Liste des nom d'inv pour eviter l'interaction avec
            ArrayList<String> inventoryNames = new ArrayList<>();
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p.getName()));
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + "Menu des warps");
            inventoryNames.add("§4Menu de modération");
            inventoryNames.add("§6§lPhases");
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
                                    statement.execute("UPDATE t_island SET active_phase='" + newPhase + "' WHERE name='" + MySQL.getIslandNameByPlayer(p.getName()) + "';");
                                }catch(Exception ex){throw new RuntimeException(ex);}
                                p.sendMessage("§2Vous avez bien sélectionner la phase §e" + newPhase + " §2!");
                            }
                            default -> {}
                        }
                    }catch (Exception ex){}
                }
            }
        }
    }
}
