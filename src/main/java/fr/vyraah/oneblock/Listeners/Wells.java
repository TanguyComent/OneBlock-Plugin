package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class Wells implements Listener {

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e){
        Player p = e.getPlayer();
        if(!MySQL.isLocationIsInPlayerIsland(p, p.getLocation())) return;
        if(!Main.INSTANCE.wells.containsKey(e.getBlock().getType())) return;
        Block bl = e.getBlock();
        Block near = e.getBlockAgainst();
        if(Main.INSTANCE.wells.containsKey(near.getType()) && bl.getType() == near.getType()){
            //ajouter au chaudron dja existant
            try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                int oldNbr = MySQL.getWellNbr(near.getLocation());
                int max = Main.INSTANCE.wells.get(bl.getType());
                int amountInMainHand = p.getInventory().getItemInMainHand().getAmount();
                if(oldNbr == max) {
                    e.setCancelled(true);
                    return;
                }
                if(p.isSneaking()) {
                    if(oldNbr + p.getInventory().getItemInMainHand().getAmount() <= max){
                        oldNbr += p.getInventory().getItemInMainHand().getAmount();
                        if(p.getGameMode() != GameMode.CREATIVE)
                            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    }else{
                        if(p.getGameMode() != GameMode.CREATIVE)
                            p.getInventory().setItemInMainHand(new ItemStack(p.getInventory().getItemInMainHand().getType(), amountInMainHand - max + oldNbr));
                        oldNbr = max;
                    }
                }else{
                    oldNbr++;
                    if(p.getGameMode() != GameMode.CREATIVE){
                        p.getInventory().setItemInMainHand(new ItemStack(p.getInventory().getItemInMainHand().getType(), amountInMainHand - 1));
                    }
                }
                statement.execute(String.format("UPDATE t_wells SET number=%d WHERE x=%d AND y=%d AND z=%d;",
                        oldNbr, (int) near.getLocation().getX(), (int) near.getLocation().getY(), (int) near.getLocation().getZ()));
                Location entityLoc = near.getLocation().add(.5, -.7, .5);
                entityLoc.getWorld().getNearbyEntities(entityLoc, .1, .1, .1).forEach((entity) -> {if(entity.getType().equals(EntityType.ARMOR_STAND)) entity.remove();});
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

    @EventHandler
    public void playerInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        Action action = e.getAction();
        if(!MySQL.isLocationIsInPlayerIsland(p, p.getLocation())) return;
        if(action == Action.LEFT_CLICK_BLOCK){
            RayTraceResult rayResult = p.rayTraceBlocks(1000);
            if(rayResult == null) return;
            Block bl = rayResult.getHitBlock();
            if (!Main.INSTANCE.wells.containsKey(bl.getType())) return;
            try (Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()) {
                int x = (int) bl.getLocation().getX();
                int y = (int) bl.getLocation().getY();
                int z = (int) bl.getLocation().getZ();
                int nbr = MySQL.getWellNbr(bl.getLocation());
                Material memory = bl.getType();
                if (nbr == -1) return;
                Location armorStandLoc = bl.getLocation().add(.5, -.7, .5);
                armorStandLoc.getWorld().getNearbyEntities(armorStandLoc, .1, .1, .1).forEach((entity) -> {
                    if (entity.getType().equals(EntityType.ARMOR_STAND)) entity.remove();
                });
                e.setCancelled(true);
                if (nbr == 1) {
                    statement.execute("DELETE FROM t_wells WHERE x=" + x + " AND  y=" + y + " AND z=" + z + ";");
                    bl.getLocation().getBlock().setType(Material.AIR);
                }else{
                    if(nbr != 2){
                        ArmorStand armorStand = (ArmorStand) bl.getLocation().getWorld().spawnEntity(bl.getLocation().add(.5, -.7, .5), EntityType.ARMOR_STAND);
                        armorStand.setVisible(false);
                        armorStand.setGravity(false);
                        armorStand.setCustomName("§6" + --nbr);
                        armorStand.setCustomNameVisible(true);
                        armorStand.setCanPickupItems(false);
                    }else{nbr--;}
                    statement.execute("UPDATE t_wells SET number=" + nbr + " WHERE x=" + x + " AND  y=" + y + " AND z=" + z + ";");
                }
                bl.getLocation().getWorld().dropItemNaturally(bl.getLocation().add(0, 1, 0), new ItemStack(memory));
            } catch (Exception ex) {throw new RuntimeException(ex);}
        }
    }
}
