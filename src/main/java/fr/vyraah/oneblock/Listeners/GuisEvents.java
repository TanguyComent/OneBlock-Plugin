package fr.vyraah.oneblock.Listeners;

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
                            default -> {}
                        }
                    }catch (Exception ex){}
                }
            }
        }
    }
}
