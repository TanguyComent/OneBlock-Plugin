package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.net.http.WebSocket;
import java.util.Random;

public class OneblockEvent implements Listener {

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(!p.getWorld().getName().equals("islands")) return;
        if(!MySQL.isLocationIsInPlayerIsland(p, e.getBlock().getLocation()) && !p.isOp()) {
            e.setCancelled(true);
            return;
        }
        Block bl = e.getBlock();
        Location obLocation = MySQL.getObLocationByIslandName(MySQL.getIslandNameByPlayer(p.getName()));
        if(bl.getLocation().distance(obLocation) == 0){
            e.setCancelled(true);
            for(ItemStack it : bl.getDrops(p.getInventory().getItemInMainHand())){
                Bukkit.getWorld("islands").dropItem(new Location(bl.getWorld(), bl.getX(), bl.getY() + 1, bl.getZ()), it);
            }
            ItemMeta meta = p.getInventory().getItemInMainHand().getItemMeta();
            if(meta instanceof Damageable d && !p.getGameMode().equals(GameMode.CREATIVE)){
                Random r = new Random();
                if(!meta.hasEnchant(Enchantment.DURABILITY)) {
                    d.setDamage(((Damageable) meta).getDamage() + 1);
                    p.getInventory().getItemInMainHand().setItemMeta(d);
                }else{
                    if(r.nextInt(meta.getEnchantLevel(Enchantment.DURABILITY)+1) == 0){
                        d.setDamage(((Damageable) meta).getDamage() + 1);
                        p.getInventory().getItemInMainHand().setItemMeta(d);
                    }
                }
            }
            obLocation.getBlock().setType(Material.AIR);
            Random r = new Random();
            int luck = r.nextInt(99);
            if (MySQL.getIslandPrestigeByPlayer(p) == 1) {
                if (luck <= 14) {
                    obLocation.getBlock().setType(Material.CHEST);
                    Chest chest = (Chest) obLocation.getBlock().getState();
                    Inventory chestInv = chest.getInventory();
                    return;
                }
                if (luck <= 54) {
                    //stone (40%)
                    obLocation.getBlock().setType(Material.STONE);
                    return;
                }
                if (luck <= 74) {
                    //bois (20%)
                    obLocation.getBlock().setType(Material.OAK_WOOD);
                    return;
                }
                if (luck <= 84) {
                    //iron (10%)
                    obLocation.getBlock().setType(Material.IRON_ORE);
                    return;
                }
                if (luck <= 89) {
                    //bois (5%)
                    obLocation.getBlock().setType(Material.GOLD_ORE);
                    return;
                }
                //charbon (10%)
                obLocation.getBlock().setType(Material.COAL_ORE);
            }
        }
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e){
        Player p = e.getPlayer();
        Block bl = e.getBlock();
        if(!MySQL.isLocationIsInPlayerIsland(p, bl.getLocation()) && !p.isOp()){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void fallDamages(EntityDamageEvent e){
        if(e.getEntity() instanceof Player p){
            if(!p.getWorld().equals(Bukkit.getWorld("islands"))) return;
            if(e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onConnect(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if(!p.getWorld().equals(Bukkit.getWorld("islands"))) return;
        String islandName = MySQL.getOnWhichIslandIsLocation(p.getLocation());
        int prestigeLvl = MySQL.getInformationByNameInt(islandName, "t_island", "prestige_level");
        int x = MySQL.getInformationByNameInt(islandName, "t_island", "center_x");
        int y = MySQL.getInformationByNameInt(islandName, "t_island", "center_y");
        int z = MySQL.getInformationByNameInt(islandName, "t_island", "center_z");
        Location loc = new Location(Bukkit.getWorld("islands"), x, y, z);
        int borderSize = switch(prestigeLvl){
            case 2 -> 100;
            case 3 -> 150;
            case 4 -> 250;
            case 5 -> 400;
            default -> 50;
        };
        new BukkitRunnable() {
            @Override
            public void run() {
                Main.INSTANCE.worldBorderApi.setBorder(p, borderSize, loc);
            }
        }.runTaskLater(Main.INSTANCE, 0);
    }

    @EventHandler
    public void teleportToIsSpawn(PlayerMoveEvent e){
        Player p = e.getPlayer();
        if(!p.getWorld().equals(Bukkit.getWorld("islands"))) return;
        if(p.getLocation().getY() <= -80){
            p.teleport(MySQL.getSpawnLocationByIslandName(MySQL.getOnWhichIslandIsLocation(p.getLocation())).add(0, 2, 0));
        }
    }
}