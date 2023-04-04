package fr.vyraah.oneblock.guis;

import fr.vyraah.oneblock.SQL.MySQL;
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
            ArrayList<String> inventoryNames = new ArrayList<>();
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + MySQL.getIslandNameByPlayer(p));
            inventoryNames.add(ChatColor.RED + "" + ChatColor.BOLD + "Menu des warps");
            if(inventoryNames.contains(e.getView().getTitle())){
                e.setCancelled(true);
                if(e.getCurrentItem() != null){
                    try{
                        String itName = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("actions"), PersistentDataType.STRING);
                        switch (itName) {
                            case "members" -> {
                                p.openInventory(guis.islandMembers(p));
                            }
                            case "warps" -> {
                                String warpName = e.getCurrentItem().getItemMeta().getDisplayName();
                                p.performCommand("is warp " + warpName);
                            }
                            default -> {
                                return;
                            }
                        }
                    }catch (Exception ex){return;}
                }
            }
        }
    }
}
