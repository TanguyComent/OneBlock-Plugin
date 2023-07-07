package fr.vyraah.oneblock.Listeners;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.io.IOException;
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
        ItemBuilder item = new ItemBuilder(Main.deserializedItem(it.getItemMeta().getPersistentDataContainer().get(NamespacedKey.fromString("item"), PersistentDataType.STRING)));
        int asId = 0;
        int itemId = 0;
        int i = 0;
        for(Player pl : MySQL.getPlayersOnIsland(MySQL.getIslandNameByPlayer(p.getName()))){
            FloatingItemsNMS floating = new FloatingItemsNMS(pl, signLoc.add(.5, 1, .5), item.toItemStack());
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
            """, islandName, chestX, chestY, chestZ, p.getFacing(), price, Main.serializedItem(item.toItemStack()), sellOrBuy, asId, itemId));
        }
    }

    @EventHandler
    public void onBreakChest(BlockBreakEvent e) throws SQLException{
        Block bl = e.getBlock();
        if(bl.getType() != Material.CHEST) return;
        Location loc = bl.getLocation();
        if(!MySQL.isLocationShop(loc)) return;
        BlockFace bf = MySQL.getShopFacing(loc);
        MySQL.removeFloatingShopItem(e.getPlayer(), loc);
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
        Block bl = e.getBlock();
        if(bl.getType() != Material.OAK_WALL_SIGN) return;
        Location chestLoc = getChestLocationBySign((WallSign) e.getBlock().getBlockData(), e.getBlock().getLocation());
        if(!MySQL.isLocationShop(chestLoc)) return;
        e.setCancelled(true);
        MySQL.removeFloatingShopItem(e.getPlayer(), chestLoc);
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
        Player p = e.getPlayer();
        if(MySQL.getOnWhichIslandIsLocation(p.getLocation()).equalsIgnoreCase("ERROR")) return;
        String island = MySQL.getOnWhichIslandIsLocation(p.getLocation());
        MySQL.setIslandShopItems(p, island);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
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
            p.sendMessage("§eItem vendu : §6§l" + item);
            p.sendMessage("§ePrix : §6§l" + price);
            p.sendMessage("§eNombre d'items disponible : §6§l" + dispo);
            p.sendMessage("");
            p.sendMessage("§6Faites un click gauche sur ce panneau si vous souhaitez acheter dans ce shop");
            p.sendMessage("§6§oPour rappel, les items sont vendu à l'unité");
        }else if(e.getAction() == Action.LEFT_CLICK_BLOCK){
            if(e.getClickedBlock().getType() != Material.OAK_WALL_SIGN) return;
            Location chestLoc = getChestLocationBySign((WallSign) e.getClickedBlock().getBlockData(), e.getClickedBlock().getLocation());
            if(!MySQL.isLocationShop(chestLoc)) return;
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e){
        if(MySQL.getOnWhichIslandIsLocation(e.getTo()).equalsIgnoreCase("ERROR")) return;
        String islandName = MySQL.getOnWhichIslandIsLocation(e.getTo());
        if(islandName.equalsIgnoreCase(MySQL.getOnWhichIslandIsLocation(e.getFrom()))) return;
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
