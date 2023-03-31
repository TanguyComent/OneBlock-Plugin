package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BreakBlock implements Listener {

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(isOnIsIsland() && !p.isOp() || p.hasPermission("is.break")) return;
        Block bl = e.getBlock();
        //cree une methode qui recup le middle d'une ile
        Location middle = new Location(Bukkit.getWorld("islands"), 0, 0, 0);
        if(bl.getLocation().equals(middle)){
            e.setCancelled(true);
            for(ItemStack it : bl.getDrops()){
                Bukkit.getWorld("islands").dropItem(middle, it);
            }
            middle.getBlock().setType(Material.AIR);
            Random r = new Random();
            int luck = r.nextInt(99);
            switch(getPrestigeLvl()) {
                case 1 -> {
                    if(luck <= 14) {
                        //coffre (15%)
                        return;
                    }
                    if(luck <= 54) {
                        //stone (40%)
                        middle.getBlock().setType(Material.STONE);
                        return;
                    }
                    if(luck <= 74){
                        //bois (20%)
                        middle.getBlock().setType(Material.OAK_WOOD);
                        return;
                    }
                    if(luck <= 84){
                        //iron (10%)
                        middle.getBlock().setType(Material.IRON_ORE);
                        return;
                    }
                    if(luck <= 89){
                        //bois (5%)
                        middle.getBlock().setType(Material.GOLD_ORE);
                        return;
                    }
                    //charbon (10%)
                    middle.getBlock().setType(Material.COAL_ORE);
                }
            }
        }
    }

    @EventHandler
    public void onChat(PlayerChatEvent e){
        Player p = e.getPlayer();

    }

    public boolean isOnIsIsland(){
        return false;
    }
    public int getPrestigeLvl(){
        return 0;
    }
}
