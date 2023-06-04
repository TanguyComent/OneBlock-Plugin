package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.RayTraceResult;

import java.sql.Statement;
import java.util.ArrayList;

public class Wells implements Listener {

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e){
        Player p = e.getPlayer();
        if(!Main.INSTANCE.wells.containsKey(e.getBlock().getType())) return;
        Block bl = e.getBlock();
        Block near = e.getBlockAgainst();
        ArrayList<Location> wells = MySQL.getWellsList(MySQL.getIslandNameByPlayer(p.getName()));
        if(Main.INSTANCE.wells.containsKey(near.getType()) && bl.getType() == near.getType()){
            //ajouter au chaudron dja existant
            try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                int oldNbr = MySQL.getWellNbr(near.getLocation());
                if(p.isSneaking()){

                }else{
                    statement.execute(String.format("UPDATE t_wells SET number=%d WHERE x=%d AND y=%d AND z=%d;",
                            ++oldNbr, (int) near.getLocation().getX(), (int) near.getLocation().getY(), (int) near.getLocation().getZ()));
                }
                Location entityLoc = near.getLocation().add(.5, -.7, .5);
                entityLoc.getWorld().getEntities().forEach((entity) -> {if(entity.getType().equals(EntityType.ARMOR_STAND)) entity.remove();});
                ArmorStand armorStand = (ArmorStand) near.getLocation().getWorld().spawnEntity(near.getLocation().add(.5, -.7, .5), EntityType.ARMOR_STAND);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setCustomName("§6" + oldNbr);
                armorStand.setCustomNameVisible(true);
                armorStand.setCanPickupItems(false);
                e.setCancelled(true);
            }catch(Exception ex){}

        }else if(Main.INSTANCE.wells.containsKey(e.getBlock().getType())){
            //crée un nouveau chaudron
            try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                statement.execute(String.format("""
                        INSERT INTO t_wells (island_name, material, x, y, z)
                        VALUES ('%s', '%s', %d, %d, %d);
                        """,MySQL.getIslandNameByPlayer(p.getName()) , bl.getType(), (int) bl.getLocation().getX(), (int) bl.getLocation().getY(), (int) bl.getLocation().getZ()));
            }catch(Exception ex){
                throw new RuntimeException(ex);
            }
        }
    }
}
