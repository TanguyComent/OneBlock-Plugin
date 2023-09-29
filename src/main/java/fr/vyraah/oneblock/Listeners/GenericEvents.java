package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import joptsimple.ValueConversionException;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

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
                p.sendMessage(Main.prefix + "§2Votre ile a bien été supprimée !");
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
                p.sendMessage(Main.prefix + "§2Suppression de l'ile annulée");
            }
            p.getPersistentDataContainer().remove(NamespacedKey.fromString("is-delete"));
        }
    }

    @EventHandler
    public void shopCreationChat(PlayerChatEvent e){
        Player p = e.getPlayer();
        if(!p.getPersistentDataContainer().has(NamespacedKey.fromString("shop-price"), PersistentDataType.STRING)) return;
        e.setCancelled(true);
        if(e.getMessage().replace(" ", "").equalsIgnoreCase("cancel")){
            p.sendMessage(Main.prefix + "§2La création de shop a bien été annulée");
            p.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-price"));
            return;
        }
        if(Main.isFull(p)){
            p.sendMessage(Main.prefix + "Votre inventaire est plein ! Veuillez y laisser au moin 1 place et retaper le prix");
            return;
        }
        try{
            int price = Integer.parseInt(e.getMessage());
            ArrayList<Object> shopInfo = GuisEvents.shopInCreation.get(p.getUniqueId());
            String buyOrSell = ((int) shopInfo.get(0) == 1) ? "buy" : "sell";
            ItemStack toSell = (ItemStack) shopInfo.get(1);
            ItemBuilder sellChest = new ItemBuilder(Material.CHEST);
            sellChest.setName("§6§lSell chest");
            sellChest.setLore("§eItem : (X" + toSell.getAmount() + ") " + toSell.getType().toString().toLowerCase(), "§eType : " + buyOrSell, "§ePrice : " + price);
            sellChest.addStringComponent("sellshop", "");
            sellChest.addIntComponent("buyorsell", (int) shopInfo.get(0));
            sellChest.addIntComponent("price", price);
            sellChest.addStringComponent("item", Main.serializedObject(toSell));
            p.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-price"));
            p.sendMessage(Main.prefix + "§2Création de shop terminée");
            p.getInventory().addItem(sellChest.toItemStack());
        }catch(Exception ex){
            p.sendMessage(Main.prefix + "§4La valeur que vous avez entrer ne correspond pas ! Si vous souhaitez annuler la création du shop, veuillez écrir 'annuler' dans le cahat");
        }
    }
}
