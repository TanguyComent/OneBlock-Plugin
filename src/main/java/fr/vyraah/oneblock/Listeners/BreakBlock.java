package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BreakBlock implements Listener {

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(!MySQL.isPlayerOnHisIsland(p) && !p.isOp()) return;
        Block bl = e.getBlock();
        Location obLocation = MySQL.getObLocationByIslandName(MySQL.getIslandNameByPlayer(p));
        if(bl.getLocation().distance(obLocation) == 0){
            e.setCancelled(true);
            for(ItemStack it : bl.getDrops()){
                Bukkit.getWorld("islands").dropItem(obLocation.add(0, 2, 0), it);
            }
            obLocation.add(0, -2, 0);
            obLocation.getBlock().setType(Material.AIR);
            Random r = new Random();
            int luck = r.nextInt(99);
            switch(MySQL.getIslandPrestigeByPlayer(p)) {
                case 1 -> {
                    if(luck <= 14) {
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                        return;
                    }
                    if(luck <= 54) {
                        //stone (40%)
                        obLocation.getBlock().setType(Material.STONE);
                        return;
                    }
                    if(luck <= 74){
                        //bois (20%)
                        obLocation.getBlock().setType(Material.OAK_WOOD);
                        return;
                    }
                    if(luck <= 84){
                        //iron (10%)
                        obLocation.getBlock().setType(Material.IRON_ORE);
                        return;
                    }
                    if(luck <= 89){
                        //bois (5%)
                        obLocation.getBlock().setType(Material.GOLD_ORE);
                        return;
                    }
                    //charbon (10%)
                    obLocation.getBlock().setType(Material.COAL_ORE);
                }
            }
        }
    }

    @EventHandler
    public void onChat(PlayerChatEvent e){
        Player p = e.getPlayer();
        if(p.getPersistentDataContainer().has(NamespacedKey.fromString("is-delete"))){
            if(e.getMessage().equals("CONFIRMER")){

            }
            p.getPersistentDataContainer().remove(NamespacedKey.fromString("is-delete"));
        }
    }
}
