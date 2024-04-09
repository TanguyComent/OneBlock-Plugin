package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class QuestEvent implements Listener {

    @EventHandler
    public static void onConnect(PlayerLoginEvent e){
        Player p = e.getPlayer();
        p.sendMessage("OKOK");
        if(MySQL.playerFirstConnexion(p)) {
            MySQL.registerPlayer(p);
        }
        if(MySQL.getPlayerHaveAnIsland(p))
            MySQL.updateQuest(MySQL.getIslandNameByPlayer(p.getName()));

    }

    @EventHandler
    public static void onCraft(CraftItemEvent e){
        if(e.getWhoClicked() instanceof Player p){
            String islandName = MySQL.getIslandNameByPlayer(p.getName());
            if(!MySQL.hasQuestX(islandName, 6) && MySQL.isQuestCompleted(islandName)) return;
            if(e.getClick() != ClickType.LEFT){
                e.setCancelled(true);
                p.sendMessage(Main.prefix + "§4Pour éviter les bugs, veuillez effectuer des click gauche lorsque vous réalisez ce craft alors que vous avez la quête d'ile §e\"§lLe forgeron en colère\"");
                return;
            }
            if(e.getRecipe().getResult().getType() == Material.IRON_CHESTPLATE){
                e.setCancelled(true);
                MySQL.incrementQuest(islandName, 1);
                e.getInventory().setItem(0, null);
                for(int i = 1; i<=9; i++){
                    if(e.getInventory().getItem(i) == null) continue;
                    if(e.getInventory().getItem(i).getAmount() == 1){
                        e.getInventory().setItem(i, null);
                        continue;
                    }
                    int amount = e.getInventory().getItem(i).getAmount();
                    e.getInventory().setItem(i, new ItemStack(Material.IRON_INGOT, amount - 1));
                }
                if(MySQL.getDailyQuestNumber(islandName) == 100){
                    MySQL.completeQuest(islandName);
                    p.sendMessage(Main.prefix + "§2Votre ile a complétée votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                }
            }
        }
    }

    @EventHandler
    public static void onFurnace(FurnaceSmeltEvent e){
        String islandName = MySQL.getOnWhichIslandIsLocation(e.getBlock().getLocation());
        if(islandName.equalsIgnoreCase("ERREUR")) return;
        if(MySQL.hasQuestX(islandName, 4) && !MySQL.isQuestCompleted(islandName)){
            if(e.getResult().getType() != Material.IRON_INGOT) return;
            MySQL.incrementQuest(islandName, 1);
            if(MySQL.getDailyQuestNumber(islandName) == 750){
                MySQL.completeQuest(islandName);
                for(String playerName : MySQL.getIslandPlayers(islandName)){
                    try{
                        Player p = Bukkit.getPlayer(playerName);
                        p.sendMessage(Main.prefix + "§2Votre ile a complétée votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                    }catch(Exception ex){continue;}
                }
            }
        }
    }

    @EventHandler
    public static void onBreak(BlockBreakEvent e){
        if((MySQL.hasQuestX(MySQL.getIslandNameByPlayer(e.getPlayer().getName()), 2) || MySQL.hasQuestX(MySQL.getIslandNameByPlayer(e.getPlayer().getName()), 5)) && !MySQL.isQuestCompleted(MySQL.getIslandNameByPlayer(e.getPlayer().getName()))){
            Player p = e.getPlayer();
            Block bl = e.getBlock();
            if (bl.getBlockData() instanceof Ageable age) {
                if (bl.getType() != Material.WHEAT || age.getAge() < age.getMaximumAge()) return;
                MySQL.incrementQuest(MySQL.getIslandNameByPlayer(p.getName()), 1);
                if (MySQL.getDailyQuestNumber(MySQL.getIslandNameByPlayer(p.getName())) == 2000) {
                    MySQL.completeQuest(MySQL.getIslandNameByPlayer(p.getName()));
                    p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                }
            }else{
                if (bl.getType() == Material.OAK_LOG) {
                    MySQL.incrementQuest(MySQL.getIslandNameByPlayer(p.getName()), 1);
                    if (MySQL.getDailyQuestNumber(MySQL.getIslandNameByPlayer(p.getName())) == 3000) {
                        MySQL.completeQuest(MySQL.getIslandNameByPlayer(p.getName()));
                        p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                    }
                }
            }
        }
    }

    @EventHandler
    public static void onFish(PlayerFishEvent e){
        if(!MySQL.hasQuestX(MySQL.getIslandNameByPlayer(e.getPlayer().getName()), 3) || MySQL.isQuestCompleted(MySQL.getIslandNameByPlayer(e.getPlayer().getName()))) return;
        if(e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player p = e.getPlayer();
        MySQL.incrementQuest(MySQL.getIslandNameByPlayer(p.getName()), 1);
        if(MySQL.getDailyQuestNumber(MySQL.getIslandNameByPlayer(p.getName())) == 500){
            MySQL.completeQuest(MySQL.getIslandNameByPlayer(p.getName()));
            p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
        }
    }

    @EventHandler
    public static void onZombieKill(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Zombie && e.getDamager() instanceof Player p){
            String islandName = MySQL.getIslandNameByPlayer(p.getName());
            if(e.getEntity() instanceof LivingEntity le) if(le.getHealth() - e.getDamage() > 0) return;
            if(!MySQL.hasQuestX(islandName, 7)) return;
            if(e.getEntity() instanceof Zombie && MySQL.hasQuestX(islandName, 7)){
                MySQL.incrementQuest(islandName, 1);
                if(MySQL.getDailyQuestNumber(islandName) == 500){
                    p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                    MySQL.completeQuest(islandName);
                }
            }
        }
    }

    @EventHandler
    public static void onSkeletonKill(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Skeleton && e.getDamager() instanceof Arrow arrow){
            if(!arrow.getPersistentDataContainer().has(NamespacedKey.fromString("player"), PersistentDataType.STRING)) return;
            String playerName = arrow.getPersistentDataContainer().get(NamespacedKey.fromString("player"), PersistentDataType.STRING);
            try {
                Player p = Bukkit.getPlayer(playerName);
                String islandName = MySQL.getIslandNameByPlayer(p.getName());
                if (e.getEntity() instanceof LivingEntity le) if (le.getHealth() - e.getDamage() > 0) return;
                if (!MySQL.hasQuestX(islandName, 8)) return;
                if (e.getEntity() instanceof Skeleton && MySQL.hasQuestX(islandName, 8)) {
                    MySQL.incrementQuest(islandName, 1);
                    if(MySQL.getDailyQuestNumber(islandName) == 200) {
                        p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                        MySQL.completeQuest(islandName);
                    }
                }
            }catch(Exception ex){}
        }
    }

    @EventHandler
    public static void onFireArrow(EntityShootBowEvent e){
        if(e.getEntity() instanceof Player p && MySQL.hasQuestX(MySQL.getIslandNameByPlayer(p.getName()), 8)){
            if(!(e.getProjectile() instanceof Arrow arrow)) return;
            arrow.getPersistentDataContainer().set(NamespacedKey.fromString("player"), PersistentDataType.STRING, p.getName());
        }
    }
}
