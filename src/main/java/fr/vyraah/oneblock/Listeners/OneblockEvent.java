package fr.vyraah.oneblock.Listeners;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Statement;
import java.util.Random;

public class OneblockEvent implements Listener {

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(!p.getWorld().getName().equals("islands")) return;
        if(!MySQL.isLocationIsInPlayerIsland(p, e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }
        Block bl = e.getBlock();
        Location obLocation = MySQL.getObLocationByIslandName(MySQL.getIslandNameByPlayer(p.getName()));
        if(bl.getLocation().distance(obLocation) == 0){
            e.setCancelled(true);
            String islandName = MySQL.getIslandNameByPlayer(p.getName());
            if(MySQL.hasQuestX(islandName, 1) && !MySQL.isQuestCompleted(islandName)){
                MySQL.incrementQuest(islandName, 1);
                if(MySQL.getDailyQuestNumber(islandName) == 3000){
                    MySQL.completeQuest(islandName);
                    p.sendMessage(Main.prefix + "§2Vous avez complété votre quête d'ile journalière ! §e/is quest §2pour récupérer votre récompense !");
                }
            }
            try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                int newTimeBreak = MySQL.getObTimesBreak(islandName) + 1;
                statement.execute("UPDATE t_island SET ob_time_break=" + newTimeBreak + " WHERE name='" + islandName + "';");
            }catch(Exception ex){throw new RuntimeException(ex);}
            for(ItemStack it : bl.getDrops(p.getInventory().getItemInMainHand())){
                if(it.getType() != Material.AIR)
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
            int luck = r.nextInt(10) + 1;
            if(luck <= 7) {
                luck = r.nextInt(100) + 1;
                int activePrestige = MySQL.getActivePrestige(MySQL.getIslandNameByPlayer(p.getName()));
                if(activePrestige == 1) {
                    if(luck <= 15) {
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                    }
                    else if(luck <= 55) obLocation.getBlock().setType(Material.STONE);

                    else if(luck <= 75) obLocation.getBlock().setType(Material.COPPER_ORE);

                    else if(luck <= 85) obLocation.getBlock().setType(Material.IRON_ORE);

                    else obLocation.getBlock().setType(Material.COAL_ORE);

                }else if(activePrestige == 2){
                    if(luck <= 10){
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                    }
                    else if(luck <= 50) obLocation.getBlock().setType(Material.STONE);

                    else if(luck <= 70) obLocation.getBlock().setType(Material.COPPER_ORE);

                    else if(luck <= 80) obLocation.getBlock().setType(Material.IRON_ORE);

                    else obLocation.getBlock().setType(Material.REDSTONE_ORE);

                }else if(activePrestige == 3){
                    if(luck <= 10){
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                    }
                    else if(luck <= 40) obLocation.getBlock().setType(Material.STONE);

                    else if(luck <= 60) obLocation.getBlock().setType(Material.COPPER_ORE);

                    else if(luck <= 70) obLocation.getBlock().setType(Material.IRON_ORE);

                    else if(luck <= 90) obLocation.getBlock().setType(Material.REDSTONE_ORE);

                    else obLocation.getBlock().setType(Material.LAPIS_ORE);

                }else if(activePrestige == 4){
                    if(luck <= 10){
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                    }
                    else if(luck <= 30) obLocation.getBlock().setType(Material.STONE);

                    else if(luck <= 50) obLocation.getBlock().setType(Material.COPPER_ORE);

                    else if(luck <= 60) obLocation.getBlock().setType(Material.IRON_ORE);

                    else if(luck <= 80) obLocation.getBlock().setType(Material.REDSTONE_ORE);

                    else if(luck <= 95) obLocation.getBlock().setType(Material.LAPIS_ORE);

                    else obLocation.getBlock().setType(Material.DIAMOND_ORE);

                }else if(activePrestige == 5){
                    if(luck <= 10){
                        obLocation.getBlock().setType(Material.CHEST);
                        Chest chest = (Chest) obLocation.getBlock().getState();
                        Inventory chestInv = chest.getInventory();
                    }
                    else if(luck <= 20) obLocation.getBlock().setType(Material.STONE);

                    else if(luck <= 40) obLocation.getBlock().setType(Material.COPPER_ORE);

                    else if(luck <= 50) obLocation.getBlock().setType(Material.IRON_ORE);

                    else if(luck <= 70) obLocation.getBlock().setType(Material.REDSTONE_ORE);

                    else if(luck <= 85) obLocation.getBlock().setType(Material.LAPIS_ORE);

                    else if(luck <= 95) obLocation.getBlock().setType(Material.DIAMOND_ORE);

                    else obLocation.getBlock().setType(Material.EMERALD_ORE);
                }
            }else{
                luck = r.nextInt(100) + 1;
                switch(MySQL.getObPhase(MySQL.getIslandNameByPlayer(p.getName())).toLowerCase()){
                    case "plaine" -> {
                        if(luck <= 20){
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.MOSS_BLOCK);
                        }else if(luck <= 60){
                            obLocation.getBlock().setType(Material.DIRT);
                        }else if(luck <= 70){
                            obLocation.getBlock().setType(Material.DIORITE);
                        }else if(luck <= 80){
                            obLocation.getBlock().setType(Material.ANDESITE);
                        }else if(luck <= 90){
                            obLocation.getBlock().setType(Material.GRANITE);
                        }else if(luck <= 95){
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.COW);
                        }else {
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.SHEEP);
                        }
                    }

                    case "foret" -> {
                        if(luck <= 23){
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                        }else if(luck <= 47){
                            obLocation.getBlock().setType(Material.OAK_LOG);
                        }else if(luck <= 71){
                            obLocation.getBlock().setType(Material.BIRCH_LOG);
                        }else if(luck <= 95){
                            obLocation.getBlock().setType(Material.BIRCH_LEAVES);
                        }else {
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.WOLF);
                        }
                    }

                    case "foret noir" -> {
                        if(luck <= 20){
                            obLocation.getBlock().setType(Material.DARK_OAK_LOG);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.SPRUCE_LOG);
                        }else if(luck <= 60){
                            obLocation.getBlock().setType(Material.SPRUCE_LEAVES);
                        }else if(luck <= 80){
                            obLocation.getBlock().setType(Material.COARSE_DIRT);
                        }else {
                            obLocation.getBlock().setType(Material.PODZOL);
                        }
                    }

                    case "savane" -> {
                        if(luck <= 25){
                            obLocation.getBlock().setType(Material.ACACIA_LOG);
                        }else if(luck <= 50){
                            obLocation.getBlock().setType(Material.RED_SAND);
                        }else if(luck <= 75){
                            obLocation.getBlock().setType(Material.RED_SANDSTONE);
                        }else {
                            obLocation.getBlock().setType(Material.ACACIA_LEAVES);
                        }
                    }

                    case "jungle" -> {
                        if(luck <= 25){
                            obLocation.getBlock().setType(Material.JUNGLE_LOG);
                        }else if(luck <= 50){
                            obLocation.getBlock().setType(Material.OAK_LOG);
                        }else if(luck <= 75){
                            obLocation.getBlock().setType(Material.JUNGLE_LEAVES);
                        }else {
                            obLocation.getBlock().setType(Material.MELON);
                        }
                    }

                    case "neige" -> {
                        if(luck <= 45) {
                            obLocation.getBlock().setType(Material.GRASS_BLOCK);
                            if (obLocation.add(0, 1, 0).getBlock().getType() == Material.AIR)
                                obLocation.getBlock().setType(Material.SNOW);
                        }else if(luck <= 90){
                            obLocation.getBlock().setType(Material.SNOW_BLOCK);
                        }else {
                            obLocation.getBlock().setType(Material.SNOW_BLOCK);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.SNOWMAN);
                        }
                    }

                    case "glace" -> {
                        if(luck <= 100 / 3){
                            obLocation.getBlock().setType(Material.ICE);
                        }else if(luck <= (100 / 3) * 2){
                            obLocation.getBlock().setType(Material.BLUE_ICE);
                        }else {
                            obLocation.getBlock().setType(Material.PACKED_ICE);
                        }
                    }

                    case "desert" -> {
                        if(luck <= 20){
                            boolean hasPlace = false;
                            if(obLocation.add(0, -1, 0).getBlock().getType() == Material.AIR) {
                                obLocation.getBlock().setType(Material.STONE);
                                hasPlace = true;
                            }
                            obLocation.add(0, 1, 0).getBlock().setType(Material.SAND);
                            if(obLocation.add(0, -1, 0).getBlock().getType() == Material.AIR || hasPlace)
                                obLocation.getBlock().setType(Material.AIR);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.SANDSTONE);
                        }else if(luck <= 60){
                            boolean hasPlace = false;
                            if(obLocation.add(0, -1, 0).getBlock().getType() == Material.AIR) {
                                obLocation.getBlock().setType(Material.STONE);
                                hasPlace = true;
                            }
                            obLocation.add(0, 1, 0).getBlock().setType(Material.RED_SAND);
                            if(obLocation.add(0, -1, 0).getBlock().getType() == Material.AIR || hasPlace)
                                obLocation.getBlock().setType(Material.AIR);
                        }else if(luck <= 80){
                            obLocation.getBlock().setType(Material.RED_SANDSTONE);
                        }else {
                            obLocation.getBlock().setType(Material.STRIPPED_BIRCH_LOG);
                        }
                    }

                    case "canione" -> {
                        if(luck <= 100/6){
                            obLocation.getBlock().setType(Material.BROWN_CONCRETE);
                        }else if(luck <= (100/6) * 2){
                            obLocation.getBlock().setType(Material.BROWN_CONCRETE_POWDER);
                        }else if(luck <= (100/6) * 3){
                            obLocation.getBlock().setType(Material.ORANGE_CONCRETE);
                        }else if(luck <= (100/6) * 4){
                            obLocation.getBlock().setType(Material.ORANGE_CONCRETE_POWDER);
                        }else if(luck <= (100/6) * 5){
                            obLocation.getBlock().setType(Material.RED_CONCRETE);
                        }else {
                            obLocation.getBlock().setType(Material.RED_CONCRETE_POWDER);
                        }
                    }

                    case "ocean" -> {
                        if(luck <= 10){
                            obLocation.getBlock().setType(Material.DEAD_TUBE_CORAL_BLOCK);
                        }else if(luck <= 20){
                            obLocation.getBlock().setType(Material.DEAD_HORN_CORAL_BLOCK);
                        }else if(luck <= 30){
                            obLocation.getBlock().setType(Material.FIRE_CORAL_BLOCK);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.BUBBLE_CORAL_BLOCK);
                        }else if(luck <= 50){
                            obLocation.getBlock().setType(Material.DARK_PRISMARINE);
                        }else if(luck <= 60){
                            obLocation.getBlock().setType(Material.PRISMARINE_BRICKS);
                        }else if(luck <= 70){
                            obLocation.getBlock().setType(Material.PRISMARINE);
                        }else if(luck <= 80){
                            obLocation.getBlock().setType(Material.SEA_LANTERN);
                        }else if(luck <= 90){
                            obLocation.getBlock().setType(Material.HORN_CORAL_BLOCK);
                        }else {
                            obLocation.getBlock().setType(Material.PRISMARINE);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.TURTLE);
                        }
                    }

                    case "nether rouge" -> {
                        if(luck <= 100/8){
                            obLocation.getBlock().setType(Material.CRIMSON_STEM);
                        }else if(luck <= (100/8) * 2){
                            obLocation.getBlock().setType(Material.NETHERRACK);
                        }else if(luck <= (100/8) * 3){
                            obLocation.getBlock().setType(Material.SOUL_SAND);
                        }else if(luck <= (100/8) * 4){
                            obLocation.getBlock().setType(Material.SOUL_SOIL);
                        }else if(luck <= (100/8) * 5){
                            obLocation.getBlock().setType(Material.MAGMA_BLOCK);
                        }else if(luck <= (100/8) * 6){
                            obLocation.getBlock().setType(Material.NETHER_WART_BLOCK);
                        }else if(luck <= (100/8) * 7){
                            obLocation.getBlock().setType(Material.NETHER_BRICK);
                        }else{
                            obLocation.getBlock().setType(Material.CRIMSON_NYLIUM);
                        }
                    }

                    case "nether bleu" -> {
                        if(luck <= 100/6){
                            obLocation.getBlock().setType(Material.WARPED_NYLIUM);
                        }else if(luck <= (100/6) * 2){
                            obLocation.getBlock().setType(Material.WARPED_STEM);
                        }else if(luck <= (100/6) * 3){
                            obLocation.getBlock().setType(Material.WARPED_WART_BLOCK);
                        }else if(luck <= (100/6) * 4){
                            obLocation.getBlock().setType(Material.BLUE_GLAZED_TERRACOTTA);
                        }else if(luck <= (100/6) * 5){
                            obLocation.getBlock().setType(Material.BLUE_CONCRETE);
                        }else {
                            obLocation.getBlock().setType(Material.NETHER_QUARTZ_ORE);
                        }
                    }

                    case "basalt" -> {
                        if(luck <= 20) {
                            obLocation.getBlock().setType(Material.BASALT);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.POLISHED_BASALT);
                        }else if(luck <= 60){
                            obLocation.getBlock().setType(Material.SMOOTH_BASALT);
                        }else{
                            obLocation.getBlock().setType(Material.DEEPSLATE_BRICKS);
                        }
                    }

                    case "ender" -> {
                        if(luck <= 20){
                            obLocation.getBlock().setType(Material.END_STONE);
                        }else if(luck <= 40){
                            obLocation.getBlock().setType(Material.END_STONE_BRICKS);
                        }else if(luck <= 60){
                            obLocation.getBlock().setType(Material.OBSIDIAN);
                        }else if(luck <= 80){
                            obLocation.getBlock().setType(Material.PURPUR_PILLAR);
                        }else {
                            obLocation.getBlock().setType(Material.PURPUR_BLOCK);
                        }
                    }

                    case "champignon" -> {
                        int champ = r.nextInt(1000);
                        if(champ == 1){
                            obLocation.getBlock().setType(Material.DIAMOND_BLOCK);
                            obLocation.getWorld().spawnEntity(obLocation.add(0, 1, 0), EntityType.MUSHROOM_COW);
                            return;
                        }
                        if(luck <= 25){
                            obLocation.getBlock().setType(Material.BROWN_MUSHROOM_BLOCK);
                        }else if(luck <= 50) {
                            obLocation.getBlock().setType(Material.RED_MUSHROOM_BLOCK);
                        }else if(luck <= 75) {
                            obLocation.getBlock().setType(Material.MUSHROOM_STEM);
                        }else{
                            obLocation.getBlock().setType(Material.PODZOL);
                        }
                    }
                }
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
    public void onIceBreak(BlockFadeEvent e){
        Location blLoc = e.getBlock().getLocation();
        if(!blLoc.getWorld().getName().equalsIgnoreCase("islands")) return;
        String isName = MySQL.getOnWhichIslandIsLocation(blLoc);
        if(isName.equalsIgnoreCase("ERROR")) return;
        if(blLoc.distance(MySQL.getObLocationByIslandName(isName)) == 0){
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onSandFall(EntityChangeBlockEvent e){
        Location blLoc = e.getBlock().getLocation();
        if(!blLoc.getWorld().getName().equalsIgnoreCase("islands")) return;
        String isName = MySQL.getOnWhichIslandIsLocation(blLoc);
        if(isName.equalsIgnoreCase("ERROR")) return;
        if(e.getEntityType()==EntityType.FALLING_BLOCK && e.getTo()==Material.AIR){
            if(blLoc.distance(MySQL.getObLocationByIslandName(isName)) == 0){
                e.setCancelled(true);
                e.getBlock().getState().update(false, false);
            }
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
        if(islandName.equalsIgnoreCase("ERROR")) {
            p.teleport(Main.spawn);
            p.sendMessage(Main.prefix + "§4L'ile sur laquelle vous étiez lors de votre deconnection a été supprimée !\n" + Main.prefix + "§2Vous avez été téleporté au spawn");
            return;
        }
        int prestigeLvl = MySQL.getIslandPrestigeByPlayer(p);
        try{
            Location loc = MySQL.getCenterLocationByIslandName(islandName);
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
        }catch(RuntimeException ex){
            p.sendMessage("§4" + ex);
            p.teleport(Main.spawn);
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
    public void onInteract(PlayerInteractEvent e){
        if(MySQL.isPlayerOnHisIsland(e.getPlayer())) return;
        if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if(e.getClickedBlock().getState() instanceof InventoryHolder) e.setCancelled(true);
    }
}
