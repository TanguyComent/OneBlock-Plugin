package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commands.oneblock;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Statement;
import java.util.Objects;
import java.util.Random;

public class BreakBlock implements Listener {

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(!MySQL.isLocationIsInPlayerIsland(p, e.getBlock().getLocation()) && !p.isOp()) {
            e.setCancelled(true);
            return;
        }
        Block bl = e.getBlock();
        Location obLocation = MySQL.getObLocationByIslandName(MySQL.getIslandNameByPlayer(p));
        if(bl.getLocation().distance(obLocation) == 0){
            e.setCancelled(true);
            for(ItemStack it : bl.getDrops()){
                Bukkit.getWorld("islands").dropItem(obLocation.add(0.5, 1, 0.5), it);
            }
            obLocation.add(-0, -1, -0);
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
    public void onChat(PlayerChatEvent e){
        Player p = e.getPlayer();
        if(p.getPersistentDataContainer().has(NamespacedKey.fromString("is-delete"), PersistentDataType.STRING)){
            if(e.getMessage().equals("CONFIRMER")){
                e.setCancelled(true);
                p.sendMessage("§2Votre ile a bien été supprimée !");
                //Tp des joueurs present sur l'is au spawn
                for(Player players : Bukkit.getOnlinePlayers()){
                    if(players.getLocation().getWorld().equals(Bukkit.getWorld("islands"))){
                        if(MySQL.getOnWhichIslandIsLocation(players.getLocation()).equals(MySQL.getIslandNameByPlayer(p))) {
                            players.teleport(new Location(Bukkit.getWorld("world"), 0, 0, 0));
                            players.sendMessage("§2Vous avez été téleporter au spawn car l'ile sur laquelle vous étiez viens d'être supprimée.");
                        }
                    }
                }
                //delete island BDD
                int islandId = MySQL.getInformationByNameInt(MySQL.getIslandNameByPlayer(p), "t_island", "id");
                try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                    statement.execute(String.format("DELETE FROM t_island WHERE id=%d;", islandId));
                    statement.execute(String.format("DELETE FROM t_user WHERE island_id=%d;", islandId));
                }catch(Exception exception){}
            }else{
                p.sendMessage("§4Suppression de l'ile annulée");
            }
            p.getPersistentDataContainer().remove(NamespacedKey.fromString("is-delete"));
        }
    }

    @EventHandler
    public void teleportToIsSpawn(PlayerMoveEvent e){
        Player p = e.getPlayer();
        if(!p.getWorld().equals(Bukkit.getWorld("islands"))) return;
        if(p.getLocation().getY() <= -80){
            p.teleport(MySQL.getSpawnLocationByIslandName(MySQL.getOnWhichIslandIsLocation(p.getLocation())).add(0, 2, 0));
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
}
