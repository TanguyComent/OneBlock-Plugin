package fr.vyraah.oneblock.Listeners;

import com.earth2me.essentials.api.Economy;
import fr.vyraah.oneblock.enums.ShopType;
import fr.vyraah.oneblock.commons.FloatingItemsNMS;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commons.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;

public class IslanShopEvent implements Listener {

    @EventHandler
    public void blockPlace(PlayerInteractEvent e) throws IOException, ClassNotFoundException, SQLException {
        if(!MySQL.isPlayerOnHisIsland(e.getPlayer())) return;
        if(!(e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        if(p.getInventory().getItemInMainHand().getType() == Material.AIR) return;
        if(!p.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().has(NamespacedKey.fromString("sellshop"), PersistentDataType.STRING)) return;
        ItemStack it = p.getInventory().getItemInMainHand();
        RayTraceResult result = p.rayTraceBlocks(1000);
        Location signLoc = result.getHitBlock().getLocation().add(0, 1, 0);
        Location chestLoc = result.getHitBlock().getLocation().add(0, 1, 0);
        ItemBuilder item = new ItemBuilder((ItemStack) Main.deserializedObject(it.getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("item"), PersistentDataType.STRING)));
        int asId = 0;
        int itemId = 0;
        int i = 0;
        signLoc.add(.5, 1, .5);
        //setting floating item for each player on the island
        for(Player pl : MySQL.getPlayersOnIsland(MySQL.getIslandNameByPlayer(p.getName()))){
            FloatingItemsNMS floating = new FloatingItemsNMS(pl, signLoc, item.toItemStack());
            if(i++ == 0) floating.setItem();
            else floating.setItem(asId, itemId);
            asId = floating.getAsId();
            itemId = floating.getItemId();
        }
        signLoc.add(-.5, -1, -.5);
        switch(p.getFacing()){
            case EAST -> signLoc.add(-1, 0, 0);
            case WEST -> signLoc.add(1, 0, 0);
            case SOUTH -> signLoc.add(0, 0, -1);
            case NORTH -> signLoc.add(0, 0, 1);
        }
        //Creating the wall sign
        signLoc.getBlock().setType(Material.OAK_WALL_SIGN);
        Sign sign = (Sign) signLoc.getBlock().getState();
        Directional facing = (Directional) sign.getBlockData();
        facing.setFacing(p.getFacing().getOppositeFace());
        sign.setBlockData(facing);
        int sellOrBuy = it.getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("buyorsell"), PersistentDataType.INTEGER);
        int price = it.getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("price"), PersistentDataType.INTEGER);
        sign.setLine(1, (sellOrBuy == 1) ? "§6Shop d'achat" : "§6Shop de vente");
        sign.setLine(2, "§eprix : §6" + price);
        sign.update();
        //Adding to db
        String islandName = MySQL.getIslandNameByPlayer(p.getName());
        int chestX = (int) chestLoc.getX();
        int chestY = (int) chestLoc.getY();
        int chestZ = (int) chestLoc.getZ();
        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
            statement.execute(String.format("""
            INSERT INTO t_island_shop (island_name, chest_x, chest_y, chest_z, sign_direction, price, item, buy_or_sell, floating_armostand_id, floating_item_id)
            VALUES ('%s', %d, %d, %d, '%s', %d, '%s', %d, %d, %d);
            """, islandName, chestX, chestY, chestZ, p.getFacing(), price, Main.serializedObject(item.clearComponents().toItemStack()), sellOrBuy, asId, itemId));
        }
    }

    @EventHandler
    public void onBreakChest(BlockBreakEvent e) throws SQLException{
        if(!MySQL.isPlayerOnHisIsland(e.getPlayer())) return;
        Block bl = e.getBlock();
        if(bl.getType() != Material.CHEST) return;
        Location loc = bl.getLocation();
        if(!MySQL.isLocationShop(loc)) return;
        BlockFace bf = MySQL.getShopFacing(loc);
        //removing the floating item each player on the island
        for(Player p : MySQL.getPlayersOnIsland(MySQL.getIslandNameByPlayer(e.getPlayer().getName()))) {
            MySQL.removeFloatingShopItem(p, loc);
        }
        //removing the shop from the database
        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
            statement.execute(String.format("DELETE FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d", (int) loc.getX(), (int) loc.getY(), (int) loc.getZ()));
        }
        switch(bf){
            case EAST -> loc.add(-1, 0, 0);
            case WEST -> loc.add(1, 0, 0);
            case SOUTH -> loc.add(0, 0, -1);
            case NORTH -> loc.add(0, 0, 1);
        }
        loc.getBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onBreakSign(BlockBreakEvent e){
        if(!MySQL.isPlayerOnHisIsland(e.getPlayer())) return;
        Block bl = e.getBlock();
        if(bl.getType() != Material.OAK_WALL_SIGN) return;
        Location chestLoc = getChestLocationBySign((WallSign) e.getBlock().getBlockData(), e.getBlock().getLocation());
        if(!MySQL.isLocationShop(chestLoc)) return;
        e.setCancelled(true);
        //removing the floating item each player on the island
        for(Player p : MySQL.getPlayersOnIsland(MySQL.getIslandNameByPlayer(e.getPlayer().getName()))) {
            MySQL.removeFloatingShopItem(p, chestLoc);
        }
        //removing the shop from the database
        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
            statement.execute(String.format("DELETE FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d", (int) chestLoc.getX(), (int) chestLoc.getY(), (int) chestLoc.getZ()));
        }catch (SQLException ex){
            throw new RuntimeException(ex);
        }
        bl.setType(Material.AIR);
        chestLoc.getBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        //setting the floatings item when a player log on an island which has shops
        Player p = e.getPlayer();
        if(MySQL.getOnWhichIslandIsLocation(p.getLocation()).equalsIgnoreCase("ERROR")) return;
        String island = MySQL.getOnWhichIslandIsLocation(p.getLocation());
        MySQL.setIslandShopItems(p, island);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) throws IOException {
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
            //sending shop information to the player
            if(e.getClickedBlock().getType() != Material.OAK_WALL_SIGN) return;
            Location chestLoc = getChestLocationBySign((WallSign) e.getClickedBlock().getBlockData(), e.getClickedBlock().getLocation());
            if(!MySQL.isLocationShop(chestLoc)) return;
            Player p = e.getPlayer();
            ItemStack it = MySQL.getIslandShopItem(chestLoc);
            String item = it.getItemMeta().getDisplayName() + " (" + it.getType() + ")";
            int price = MySQL.getIslandShopPrice(chestLoc);
            int dispo = howManyItemIntoShop(chestLoc);
            p.sendMessage("§6§lInformation du shop :");
            p.sendMessage("");
            p.sendMessage("§eType : §6§l" + MySQL.getIslandShopType(chestLoc));
            p.sendMessage("§eItem : §6§l" + item);
            p.sendMessage("§ePrix : §6§l" + price);
            if(MySQL.getIslandShopType(chestLoc) == ShopType.sell) p.sendMessage("§eNombre d'items disponible : §6§l" + dispo);
            p.sendMessage("");
            p.sendMessage("§6Faites un click gauche sur ce panneau si vous souhaitez acheter dans ce shop");
            p.sendMessage("§6§oPour rappel, les items sont vendu à l'unité");
        }else if(e.getAction() == Action.LEFT_CLICK_BLOCK){
            //the player want to buy / sell
            if(e.getClickedBlock().getType() != Material.OAK_WALL_SIGN) return;
            Location chestLoc = getChestLocationBySign((WallSign) e.getClickedBlock().getBlockData(), e.getClickedBlock().getLocation());
            if(!MySQL.isLocationShop(chestLoc)) return;
            Player p = e.getPlayer();
            String buyOrSell = (MySQL.getIslandShopType(chestLoc) == ShopType.buy) ? "acheter" : "vendre";
            p.sendMessage(Main.prefix + "§2Veuillez indiquer le nombre de " + MySQL.getIslandShopItem(chestLoc).getType() + " que vous souhaitez " + buyOrSell);
            p.sendMessage((MySQL.getIslandShopType(chestLoc) == ShopType.sell) ? Main.prefix + "§2Nombre d'items disponible : §6§l" + howManyItemIntoShop(chestLoc)
                    : Main.prefix + "§2Vous avez §6§l" + howManyItemIntoInv(p, MySQL.getIslandShopItem(chestLoc)) + " §2fois cet item dans votre inventaire");
            p.getPersistentDataContainer().set(NamespacedKey.fromString("shop-loc"), PersistentDataType.STRING, Main.serializedObject(chestLoc));
        }
    }


    @EventHandler
    public void onMessage(PlayerChatEvent e) throws IOException, ClassNotFoundException {
        //verifying if the player want to buy and other stuff, if so, initialising variables
        Player sender = e.getPlayer();
        if(!sender.getPersistentDataContainer().has(NamespacedKey.fromString("shop-loc"), PersistentDataType.STRING)) return;
        e.setCancelled(true);
        Location chestLoc = (Location) Main.deserializedObject(sender.getPersistentDataContainer().get(NamespacedKey.fromString("shop-loc"), PersistentDataType.STRING));
        if(MySQL.getOnWhichIslandIsLocation(chestLoc).equalsIgnoreCase(MySQL.getIslandNameByPlayer(sender.getName()))) return;
        ShopType type = MySQL.getIslandShopType(chestLoc);
        int unitPrice = MySQL.getIslandShopPrice(chestLoc);
        ItemStack item = MySQL.getIslandShopItem(chestLoc);
        try{
            int amount = Integer.parseInt(e.getMessage());
            int finalPrice = unitPrice * amount;
            int neededSpace = (amount / item.getType().getMaxStackSize()) + 1;
            //verifying if the amount isn't negative (or equals to 0 at the same time). Money duplication glitch patch
            if(amount < 1){
                sender.sendMessage(Main.prefix + "§4Wtf comment ça tu veut acheter un nombre negatif d'items ??? §o(Achat annulée)");
                sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
                return;
            }
            switch(type){
                case sell -> {
                    //the shop is a sell shop
                    //verifying some stuff
                    if(Economy.hasEnough(sender.getUniqueId(), BigDecimal.valueOf(finalPrice))){
                        if(howManyItemIntoShop(chestLoc) < amount){
                            sender.sendMessage(Main.prefix + "§4Ce shop n'as pas asser de stock ! §o(achat annulé)");
                            sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
                            return;
                        }
                        if(Main.getInventorySpace(sender) < neededSpace){
                            sender.sendMessage(Main.prefix + "§4Il vous faut plus de place pour pouvoir acheter en telle quantité ! §o(achat annulé)");
                            sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
                            return;
                        }
                        //removing the items from the chest
                        Chest chest = (Chest) chestLoc.getBlock().getState();
                        int toRemove = amount;
                        for(int i = 0; i <= 26; i++){
                            ItemStack chestItem = chest.getBlockInventory().getItem(i);
                            if(chestItem == null) continue;
                            item.setAmount(chestItem.getAmount());
                            if(chestItem.equals(item)){
                                if(toRemove > chestItem.getAmount()) {
                                    toRemove -= chestItem.getAmount();
                                    chest.getBlockInventory().setItem(i, null);
                                }else{
                                    item.setAmount(chestItem.getAmount() - toRemove);
                                    chest.getBlockInventory().setItem(i, item);
                                    break;
                                }
                            }
                        }
                        //adding the items to the player inv
                        for(int i = 1; i <= neededSpace; i++){
                            item.setAmount(Math.min(amount, item.getType().getMaxStackSize()));
                            sender.getInventory().addItem(item);
                            amount -= item.getType().getMaxStackSize();
                        }
                        //economy stuff
                        Economy.add(sender.getUniqueId(), BigDecimal.valueOf(-1 * finalPrice));
                        MySQL.alterBankSold(MySQL.getOnWhichIslandIsLocation(chestLoc), finalPrice, 1);
                        sender.sendMessage(Main.prefix + "§2Achat effectué avec succes");
                    }else{
                        //the player don't have enough money
                        sender.sendMessage(Main.prefix + "§4Vous n'avez pas asser d'argent pour effectuer cet achat ! §o(achat annulée)");
                    }
                }

                case buy -> {
                    //the shop is a buy shop
                    //verifying some stuff
                    if(MySQL.getObBankMoney(MySQL.getOnWhichIslandIsLocation(chestLoc)) >= finalPrice){
                        if(howManyItemIntoInv(sender, item) < amount){
                            sender.sendMessage(Main.prefix + "§4Vous n'avez pas asser de fois cette item dans votre inventaire ! §o(vente annulée)");
                            sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
                            return;
                        }
                        int shopSpace = 0;
                        Chest chest = (Chest) chestLoc.getBlock().getState();
                        for(int i = 0; i <= 26; i++){
                            if(chest.getBlockInventory().getItem(i) == null) shopSpace++;
                        }
                        if(shopSpace < neededSpace){
                            sender.sendMessage(Main.prefix + "§4Ce shop n'as pas asser de place ! §o(vente annulée)");
                            sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
                            return;
                        }
                        //removing items from the player
                        int toRemove = amount;
                        for(int i = 0; i <= 26; i++){
                            ItemStack invItem = sender.getInventory().getItem(i);
                            if(invItem == null) continue;
                            item.setAmount(invItem.getAmount());
                            if(invItem.equals(item)){
                                if(toRemove > invItem.getAmount()) {
                                    toRemove -= invItem.getAmount();
                                    sender.getInventory().setItem(i, null);
                                }else{
                                    item.setAmount(invItem.getAmount() - toRemove);
                                    sender.getInventory().setItem(i, item);
                                    break;
                                }
                            }
                        }
                        //adding items to chest
                        for(int i = 1; i <= neededSpace; i++){
                            item.setAmount(Math.min(amount, item.getType().getMaxStackSize()));
                            chest.getBlockInventory().addItem(item);
                            amount -= item.getType().getMaxStackSize();
                        }
                        //economy stuffs
                        Economy.add(sender.getUniqueId(), BigDecimal.valueOf(finalPrice));
                        MySQL.alterBankSold(MySQL.getOnWhichIslandIsLocation(chestLoc), finalPrice, -1);
                        sender.sendMessage(Main.prefix + "§2Vente effectuée avec succes");
                    }else{
                        //the island don't have enough money
                        sender.sendMessage(Main.prefix + "§4L'ile à qui vous essayez de vendre n'as pas asser d'argent ! §o(vente annulé)");
                    }
                }
            }
        }catch(Exception ex){
            sender.sendMessage(Main.prefix + "§4Achat annulé");
        }
        sender.getPersistentDataContainer().remove(NamespacedKey.fromString("shop-loc"));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (MySQL.getOnWhichIslandIsLocation(e.getTo()).equalsIgnoreCase("ERROR")) return;
        String islandName = MySQL.getOnWhichIslandIsLocation(e.getTo());
        if (islandName.equalsIgnoreCase(MySQL.getOnWhichIslandIsLocation(e.getFrom()))) return;
        Player p = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                MySQL.setIslandShopItems(p, islandName);
            }
        }.runTaskLater(Main.INSTANCE, 0);
    }

    private int howManyItemIntoShop(Location loc){
        int nbr = 0;
        Chest chest = (Chest) loc.getBlock().getState();
        ItemStack toSell = MySQL.getIslandShopItem(loc);
        for(int i = 0; i <= 26; i++){
            ItemStack chestItem = chest.getBlockInventory().getItem(i);
            if(chestItem == null) continue;
            toSell.setAmount(chestItem.getAmount());
            if(chestItem.equals(toSell)) nbr += chestItem.getAmount();
        }
        return nbr;
    }

    private int howManyItemIntoInv(Player p, ItemStack toCheck){
        int nbr = 0;
        for(int i = 0; i <= 35; i++){
            ItemStack it = p.getInventory().getItem(i);
            if(it == null) continue;
            toCheck.setAmount(it.getAmount());
            if(it.equals(toCheck)) nbr += it.getAmount();
        }
        return nbr;
    }

    private Location getChestLocationBySign(WallSign sign, Location signloc){
        BlockFace bf = sign.getFacing();
        switch(bf){
            case WEST -> signloc.add(1, 0, 0);
            case EAST -> signloc.add(-1, 0, 0);
            case SOUTH -> signloc.add(0, 0, -1);
            case NORTH -> signloc.add(0, 0, 1);
        }
        return signloc;
    }
}
