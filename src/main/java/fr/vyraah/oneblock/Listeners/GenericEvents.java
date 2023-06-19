package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.Statement;
import java.util.Collection;

public class GenericEvents implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e){
        if(e.getDamager().getType() != EntityType.PLAYER) return;
        if(e.getEntity().getType() != EntityType.ARMOR_STAND) return;
        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
            Location eLoc = e.getEntity().getLocation();
            statement.execute("DELETE FROM t_holo WHERE x=" + eLoc.getX() + " AND z=" + eLoc.getZ() + ";");
        }catch(Exception ex){throw new RuntimeException(ex);}
        @NotNull Collection<Entity> entities =  e.getEntity().getLocation().getWorld().getNearbyEntities(e.getEntity().getLocation(), 0, 10, 0);
        entities.forEach(Entity::remove);
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
                        if(MySQL.getOnWhichIslandIsLocation(players.getLocation()).equals(MySQL.getIslandNameByPlayer(p.getName()))) {
                            players.teleport(Main.INSTANCE.spawn);
                            players.sendMessage("§2Vous avez été téleporter au spawn car l'ile sur laquelle vous étiez viens d'être supprimée.");
                        }
                    }
                }
                //delete island BDD
                int islandId = MySQL.getInformationByNameInt(MySQL.getIslandNameByPlayer(p.getName()), "t_island", "id");
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
}
