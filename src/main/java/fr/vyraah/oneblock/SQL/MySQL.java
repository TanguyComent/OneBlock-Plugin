package fr.vyraah.oneblock.SQL;

import fr.vyraah.oneblock.enums.IslandPermission;
import fr.vyraah.oneblock.enums.ShopType;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.commons.FloatingItemsNMS;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MySQL {
    private Connection conn;

    public static void createIsland(Player creator, String islandName, int x, int y, int z) throws SQLException {
        String islandQuery = "INSERT INTO ISLAND (islandName, islandCenterX, islandCenterY, islandCenterZ, islandOneBlockX, islandOneBlockY, islandOneBlockZ, islandSpawnX, islandSpawnY, islandSpawnZ) VALUES (?,?,?,?,?,?,?,?,?,?)";
        String banqueQuery = "INSERT INTO BANQUE (islandId) VALUES (?)";
        String gradeQuery  = """
                              INSERT INTO GRADE (islandId, gradeName, gradeColor)
                              VALUES
                              (?, 'Leader', 'r'), (?, 'Co-leader', 'o'), (?, 'Administrator', 'y'), (?, 'Member', 'g'), (?, 'Visitor', 'g')
                             """;
        String permissionQuery = "INSERT INTO GRADE_PERMISSION (gradeId, permissionName, permissionValue) VALUES (?,?,?)";
        String settingQuery = "INSERT INTO ISLAND_SETTING (islandId, settingTitle, settingValue) VALUES (?,?,?)";
        Connection con  = Main.INSTANCE.mysql.getConnection();
        try(PreparedStatement ps1 = con.prepareStatement(islandQuery, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement ps2 = con.prepareStatement(banqueQuery);
            PreparedStatement ps3 = con.prepareStatement(gradeQuery, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement ps4 = con.prepareStatement(permissionQuery);
            PreparedStatement ps5 = con.prepareStatement(settingQuery)){


            con.setAutoCommit(false);

            //Insersion de l'ile dans la bdd
            ps1.setString(1, islandName);
            ps1.setInt(2, x);
            ps1.setInt(3, y);
            ps1.setInt(4, z);
            ps1.setInt(5, x);
            ps1.setInt(6, y);
            ps1.setInt(7, z);
            ps1.setInt(8, x);
            ps1.setInt(9, y);
            ps1.setInt(10, z);

            ps1.executeUpdate();

            //Récupération de l'id de l'ile
            int islandId = 0;

            try(var generatedId = ps1.getGeneratedKeys()){
                if(generatedId.next()){
                    islandId = generatedId.getInt(1);
                }
            }

            //Insersion du joueurs dans l'ile avec le grade chef, suppression de l'ile si erreur
            if(!addPlayerToIsland(creator, islandId, 0)){
                con.rollback();
                throw new RuntimeException("Failed to add player to the island");
            }

            //Insersion de la banque d'ile
            ps2.setInt(1, islandId);

            ps2.executeUpdate();

            //Insersion des grade par default de l'ile
            for(int i = 1; i <= 13; i += 3){
                ps3.setInt(i, islandId);
            }

            ps3.executeUpdate();

            HashMap<String, Integer> map = new HashMap<>();
            var result = con.createStatement().executeQuery("SELECT permissionName AS pn, permissionLevel AS pl FROM PERMISSION");
            while(result.next()){
                map.put(result.getString("pn"), result.getInt("pl"));
            }

            //Ajout des map a chaque grade
            //boucle de tout les grades
            try(var generatedId = ps3.getGeneratedKeys()){
                for(int i = 1; i <= 5; i++){
                    int gradeId = generatedId.getInt(i);
                    //boucle de toutes les map
                    for(Map.Entry<String, Integer> entry : map.entrySet()){
                        ps4.setInt(1, gradeId);
                        ps4.setString(2, entry.getKey());
                        ps4.setByte(3, (byte) (i <= entry.getValue() ? 1 : 0));
                        ps4.addBatch(); // Ajout à un lot
                    }
                }
            }

            ps4.executeBatch();

            //Ajout des settings d'ile
            result = con.createStatement().executeQuery("SELECT settingTitle AS st, settingDefault AS sd FROM SETTING");
            while(result.next()){
                ps5.setInt(1, islandId);
                ps5.setString(2, result.getString("st"));
                ps5.setInt(3, result.getInt("sd") == 1 ? 1 : 0);
                ps5.addBatch();
            }

            ps5.executeBatch();

            con.commit();
        }catch (SQLException e){
            con.rollback();
            throw(e);
        }
    }

    public static boolean addPlayerToIsland(Player p, int islandId, int gradeId){
        String query = "UPDATE PLAYER SET islandId = ?, gradeId = ? WHERE playerUUID = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(query)){
            ps.setInt(1, islandId);
            ps.setInt(2, gradeId);
            ps.setString(3, p.getUniqueId().toString());

            ps.executeUpdate();
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public static boolean getPlayerHaveAnIsland(Player p){
        try{
            Connection con = Main.INSTANCE.mysql.getConnection();
            ResultSet result = con.createStatement().executeQuery("SELECT * FROM t_user;");
            ArrayList<String> existingUsers = new ArrayList<>();
            while(result.next()){
                existingUsers.add(result.getString("name"));
            }
            if(existingUsers.contains(p.getName())) return true;
        }catch (Exception e){}
        return false;
    }

    public static String getIslandNameByPlayer(String playerName){
        String islandName = "";
        try{
            Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM t_user WHERE name=\"" + playerName + "\";");
            int id = 0;
            while(result.next()){
                id = result.getInt("island_id");
            }
            result = statement.executeQuery("SELECT * FROM t_island WHERE id=" + id + ";");
            while(result.next()){
                islandName = result.getString("name");
            }
        }catch (Exception e){}
        return islandName;
    }

    public static int getInformationByNameInt(String name, String table, String information){
        int i = 0;
        try{
            Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT " + information + " FROM " + table + " WHERE name=\"" + name + "\";");
            while(result.next()){
                i = result.getInt(information);
            }
        }catch (Exception e){}
        return i;
    }

    public static boolean isPlayerOnHisIsland(Player p){
        return isLocationIsInPlayerIsland(p, p.getLocation());
    }

    public static boolean isLocationIsInPlayerIsland(Player p, Location loc){
        if(!getPlayerHaveAnIsland(p)) return false;
        if(!loc.getWorld().getName().equals("islands")) return false;
        int x = getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "center_x");
        int z = getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "center_z");
        int px = (int) loc.getX();
        int pz = (int) loc.getZ();
        int radius = Main.INSTANCE.radiusLevel.get(MySQL.getIslandPrestigeByPlayer(p));
        return px <= x + radius && px >= x - radius && pz <= z + radius && pz >= z - radius;
    }

    public static String getOnWhichIslandIsLocation(Location loc){
        if(!loc.getWorld().getName().equalsIgnoreCase("islands")){return "Error you are not in the good world";}
        int x = 0;
        int z = 0;
        while(true){
            if(x >= Main.INSTANCE.config.getInt("next_island_x") && z >= Main.INSTANCE.config.getInt("next_island_z")) return "ERROR";
            Location testLoc = new Location(Bukkit.getWorld("islands"), x, 0, z);
            if(new Location(Bukkit.getWorld("islands"), loc.getX(), 0, loc.getZ()).distance(testLoc) <= 300) break;
            if( x == z ){ x += 3000; } else { z += 3000; }
        }
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT name FROM t_island WHERE center_x=" + x + " AND center_z=" + z + ";");
            while(result.next()){
                return result.getString("name");
            }
        }catch(Exception e){}
        return "ERROR";
    }

    public static ArrayList<Player> getPlayersOnIsland(String islandName){
        ArrayList<Player> players = new ArrayList<>();
        for(Player p : Bukkit.getOnlinePlayers()){
            if(MySQL.getOnWhichIslandIsLocation(p.getLocation()).equalsIgnoreCase(islandName)){
                players.add(p);
            }
        }
        return players;
    }

    public static int getPlayerGrade(String playerName){
        return getInformationByNameInt(playerName, "t_user", "user_island_grade");
    }

    public static int getIslandLevelByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "level");
    }

    public static int getIslandPrestigeByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "prestige_level");
    }

    public static int getIslandPrestigeByIslandName(String islandName){
        return getInformationByNameInt(islandName, "t_island", "prestige_level");
    }

    public static int getIslandIdByIslandName(String islandName){
        return getInformationByNameInt(islandName, "t_island", "id");
    }

    public static int getIslandIdByPlayer(String player){
        return getInformationByNameInt(getIslandNameByPlayer(player), "t_island", "id");
    }

    public static ArrayList<String> getIslandPlayers(String islandName){
        ArrayList<String> players = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT * FROM t_user WHERE island_id=" + getIslandIdByIslandName(islandName) + ";");
            while(result.next()){
                players.add(result.getString("name"));
            }
        }catch (Exception e){}
        return players;
    }

    public static ArrayList<String> getIslandWarpsName(){
        ArrayList<String> warps = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT name FROM t_island_warp");
            while (result.next()){
                warps.add(result.getString("name"));
            }
        }catch (Exception e){}
        return warps;
    }

    public static Location getObLocationByIslandName(String isName){
        return new Location(Bukkit.getWorld("islands"), getInformationByNameInt(isName, "t_island", "oneblock_x"), getInformationByNameInt(isName, "t_island", "oneblock_y"), getInformationByNameInt(isName, "t_island", "oneblock_z"));
    }

    public static Location getSpawnLocationByIslandName(String isName){
        return new Location(Bukkit.getWorld("islands"), getInformationByNameInt(isName, "t_island", "spawn_x"), getInformationByNameInt(isName, "t_island", "spawn_y"), getInformationByNameInt(isName, "t_island", "spawn_z"));
    }

    public static Location getCenterLocationByIslandName(String isName){
        return new Location(Bukkit.getWorld("islands"), getInformationByNameInt(isName, "t_island", "center_x"), getInformationByNameInt(isName, "t_island", "center_y"), getInformationByNameInt(isName, "t_island", "center_z"));
    }

    public static int howManyPlayersHasIsland(String islandName){
        int nbr = 0;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT COUNT(*) AS nbr FROM t_user WHERE island_id=" + getInformationByNameInt(islandName, "t_island", "id") + ";");
            while(result.next()){
                nbr = result.getInt("nbr");
            }
        }catch(Exception e){}
        return nbr;
    }

    public static ArrayList<Location> getWellsList(String islandName){
        ArrayList<Location> wells = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT x, y, z FROM t_wells WHERE island_name='" + islandName + "';");
            while(result.next()){
                wells.add(new Location(Bukkit.getWorld("islands"), result.getInt("x"), result.getInt("y"), result.getInt("z")));
            }
        }catch (Exception e){}
        return wells;
    }

    public static int getWellNbr(Location wellLoc){
        int nbr = -1;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery(String.format("SELECT number AS nbr FROM t_wells WHERE x=%d AND y=%d AND z=%d;",
                    (int) wellLoc.getX(), (int) wellLoc.getY(), (int) wellLoc.getZ()));
            while(result.next()){
                nbr = result.getInt("nbr");
            }
        }catch (Exception e){}
        return nbr;
    }

    public static ArrayList<ArrayList<Object>> getWellsInformation(String islandName){
        ArrayList<Location> wellLoc = getWellsList(islandName);
        ArrayList<ArrayList<Object>> wellsInfo = new ArrayList<>();
        wellLoc.forEach((loc) -> {
            try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
                ResultSet result = statement.executeQuery(String.format("SELECT material AS mat, number AS nbr FROM t_wells WHERE x=%d AND y=%d AND z=%d;",
                        (int) loc.getX(), (int) loc.getY(), (int) loc.getZ()));
                ArrayList<Object> info = new ArrayList<>();
                while(result.next()){
                    info.add(result.getString("mat"));
                    info.add(result.getInt("nbr"));
                }
                wellsInfo.add(info);
            }catch(Exception e){throw new RuntimeException(e);}
        });
        return wellsInfo;
    }

    public static ArrayList<ArrayList<Object>> getIslandTop(){
        ArrayList<ArrayList<Object>> islandTop = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT name, level FROM t_island ORDER BY level;");
            while(result.next()){
                ArrayList<Object> island = new ArrayList<>();
                island.add(result.getString("name"));
                island.add(result.getInt("level"));
                islandTop.add(island);
            }
        }catch(Exception e){throw new RuntimeException(e);}
        return islandTop;
    }

    public static ArrayList<Location> getScoreboard(){
        ArrayList<Location> sbs = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT x, z, world, y FROM t_holo WHERE type=\"sb\";");
            while(result.next()){
                sbs.add(new Location(Bukkit.getWorld(result.getString("world")), result.getFloat("x"), result.getFloat("y"), result.getFloat("z")));
            }
        }catch(Exception e){}
        return sbs;
    }

    public static int getObTimesBreak(String islandName){
        int times = 0;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT ob_time_break AS br FROM t_island WHERE name=\"" + islandName + "\";");
            while(result.next()){
                times = result.getInt("br");
            }
        }catch(Exception e){}
        return times;
    }

    public static String getObPhase(String islandName){
        String obPhase = "";
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT active_phase AS ap FROM t_island WHERE name='" + islandName + "';");
            while(result.next()){
                obPhase = result.getString("ap");
            }
        }catch(Exception e){}
        return obPhase;
    }

    public static int getObQuestNbr(String islandName){
        return getInformationByNameInt(islandName, "t_island", "completed_daily_objective_number");
    }

    public static int getObBankMoney(String islandName){
        return getInformationByNameInt(islandName, "t_island", "bank");
    }

    public static int getActivePrestige(String islandName){
        return getInformationByNameInt(islandName, "t_island", "active_prestige");
    }

    public static boolean alterBankSold(String islandName, long value, int operation){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int bankSold = getObBankMoney(islandName);
            if(operation == -1){
                if(value > bankSold) return false;
            }
            value *= operation;
            long soldToSet = getObBankMoney(islandName) + value;
            statement.execute("UPDATE t_island SET bank=" + soldToSet + " WHERE name='" + islandName + "';");
            return true;
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static void updateQuest(String islandName){
        Random r = new Random();
        int realDay = LocalDateTime.now().getDayOfMonth();
        int realMonth = LocalDateTime.now().getMonthValue();
        int questDay = 0;
        int questMonth = 0;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT day, month FROM t_island_daily_quest WHERE island_id='" + getIslandIdByIslandName(islandName) + "';");
            while(result.next()){
                questDay = result.getInt("day");
                questMonth = result.getInt("month");
            }
            System.out.println(questDay + " -- " + questMonth);
            if(realDay == questDay && realMonth == questMonth) return;
            statement.execute("DELETE FROM t_island_daily_quest WHERE island_id='" + getIslandIdByIslandName(islandName) + "';");
            statement.execute(String.format("""
                       INSERT INTO t_island_daily_quest(day, month, quest_id, island_id)
                       VALUES (%d, %d, %d, %d);
                       """, realDay, realMonth, r.nextInt(8) + 1, getIslandIdByIslandName(islandName)));
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static boolean hasQuestX(String islandName, int questId){
        return getDailyQuestId(islandName) == questId;
    }

    public static int getDailyQuestId(String islandName){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT quest_id FROM t_island_daily_quest WHERE island_id='" + getIslandIdByIslandName(islandName) + "';");
            int questId = 0;
            while(result.next()){
                questId = result.getInt("quest_id");
            }
            return questId;
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static int getDailyQuestNumber(String islandName){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT number FROM t_island_daily_quest WHERE island_id='" + getIslandIdByIslandName(islandName) + "';");
            int number = 0;
            while (result.next()) {
                number = result.getInt("number");
            }
            return number;
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static void incrementQuest(String islandName, int toAdd){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            statement.execute("UPDATE t_island_daily_quest SET number=" + (getDailyQuestNumber(islandName) + toAdd) + " WHERE island_id='" + getIslandIdByIslandName(islandName) + "';");
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static boolean isQuestCompleted(String islandName){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT completed FROM t_island_daily_quest WHERE island_id=" + getIslandIdByIslandName(islandName) + ";");
            int isComplete = 0;
            while(result.next()){
                isComplete = result.getInt("completed");
            }
            return isComplete == 1;
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static void completeQuest(String islandName){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            statement.execute("UPDATE t_island_daily_quest SET completed=1 WHERE island_id=" + getIslandIdByIslandName(islandName) + ";");
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static boolean hasBeenRewarded(String islandName){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT rewarded FROM t_island_daily_quest WHERE island_id=" + getIslandIdByIslandName(islandName) + ";");
            int hasBeenRewarded = 0;
            while(result.next()){
                hasBeenRewarded = result.getInt("rewarded");
            }
            return hasBeenRewarded == 1;
        }catch(Exception e){throw new RuntimeException(e);}
    }

    private static ArrayList<ArrayList<Object>> getIslandShopItems(Player p, String islandName){
        ArrayList<ArrayList<Object>> items = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT item, chest_x AS x, chest_y AS y, chest_z AS z FROM t_island_shop WHERE island_name='" + islandName + "';");
            while(result.next()){
                ArrayList<Object> item = new ArrayList<>();
                item.add(Main.deserializedObject(result.getString("item")));
                item.add(new Location(p.getWorld(), result.getInt("x"), result.getInt("y"), result.getInt("z")));
                items.add(item);
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    public static void setIslandShopItems(Player p, String islandName){
        ArrayList<ArrayList<Object>> items = getIslandShopItems(p, islandName);
        for(ArrayList<Object> item : items){
            ItemStack it = (ItemStack) item.get(0);
            Location loc = (Location) item.get(1);
            FloatingItemsNMS floating = new FloatingItemsNMS(p, new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()).add(0.5, 1, 0.5), it);
            floating.setItem(getAsId(loc), getItemId(loc));
        }
    }

    public static int getAsId(Location loc){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT floating_armostand_id FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                return result.getInt("floating_armostand_id");
            }
        }catch(Exception e){throw new RuntimeException(e);}
        return 0;
    }

    public static int getItemId(Location loc){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT floating_item_id FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                return result.getInt("floating_item_id");
            }
        }catch(Exception e){}
        return 0;
    }

    public static boolean isLocationShop(Location loc){
        if(loc.getBlock().getType() != Material.CHEST) return false;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT island_name, sign_direction FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            String islandName = "ERROR";
            while(result.next()){
                islandName = result.getString("island_name");
            }
            return !islandName.equalsIgnoreCase("ERROR");
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static BlockFace getShopFacing(Location loc){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT sign_direction FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            String facing = null;
            while(result.next()){
                facing = result.getString("sign_direction");
            }
            return BlockFace.valueOf(facing);
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public static void removeFloatingShopItem(Player p, Location loc){
        int asId = -666;
        int itId = -666;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT floating_armostand_id, floating_item_id FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                asId = result.getInt("floating_armostand_id");
                itId = result.getInt("floating_item_id");
            }
            if(asId == -666 || itId == -666) return;
        }catch(Exception e){throw new RuntimeException(e);}
        ServerPlayer sp = ((CraftPlayer) p).getHandle();
        ServerGamePacketListenerImpl con = sp.connection;
        con.send(new ClientboundRemoveEntitiesPacket(asId, itId));
    }

    public static ItemStack getIslandShopItem(Location loc){
        ItemStack it = new ItemStack(Material.AIR);
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT item FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                it = (ItemStack) Main.deserializedObject(result.getString("item"));
            }
        }catch(Exception e){throw new RuntimeException(e);}
        return it;
    }

    public static int getIslandShopPrice(Location loc){
        int price = -1;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT price FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                price = result.getInt("price");
            }
        }catch(Exception e){throw new RuntimeException(e);}
        return price;
    }

    public static ShopType getIslandShopType(Location loc){
        int type = 0;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ResultSet result = statement.executeQuery(String.format("SELECT buy_or_sell FROM t_island_shop WHERE chest_x=%d AND chest_y=%d AND chest_z=%d;", x, y, z));
            while(result.next()){
                type = result.getInt("buy_or_sell");
            }
        }catch(Exception e){throw new RuntimeException(e);}
        return (type == 1) ? ShopType.buy : ShopType.sell;
    }

    public static boolean hasPermission(Player p, IslandPermission perm, int islandId){
        String[] permList = new String[0];
        // 0 : Co leader | 1 : Moderator | 2 : Member | 3 : Visitor
        String permName = "perm_" + perm;
        if(islandId == getIslandIdByIslandName(p.getName()) && getPlayerGrade(p.getName()) == 1) return true;
        int pPerm = (islandId == getIslandIdByPlayer(p.getName())) ? getPlayerGrade(p.getName()) - 2 : 3;
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery(String.format("SELECT %s AS perm FROM t_island WHERE island_id=%d;", permName, getIslandIdByPlayer(p.getName())));
            while(result.next()){
                permList = result.getString("perm").split("\\d");
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return permList[pPerm].equals("1");
    }

    // =========================================================================================
    //                             CONNECTION & DECONNEXION DE LA DB
    // =========================================================================================

    public static void initDatabase() {
        //creation du shema de la bdd automatique, utilisation de ALTER ADD pour prévoir des ajouts futurs de nouvelles tables ou champs de tables à la bdd
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            //création de la table des iles
            try{statement.execute("CREATE TABLE t_island (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD name VARCHAR(500) NOT NULL UNIQUE;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD prestige_level INT NOT NULL DEFAULT 1;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD active_prestige INT NOT NULL DEFAULT 1;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD completed_daily_objective_number INT NOT NULL DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD level FLOAT NOT NULL DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD ob_time_break INT NOT NULL DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD active_phase VARCHAR(100) NOT NULL DEFAULT \"Plaine\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD center_x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD center_y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD center_z INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD oneblock_x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD oneblock_y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD oneblock_z INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD spawn_x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD spawn_y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD spawn_z INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD allow_visitors INT NOT NULL DEFAULT TRUE;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD bank BIGINT DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_break_block VARCHAR(4) DEFAULT \"1110\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_place_block VARCHAR(4) DEFAULT \"1110\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_manage_permissions VARCHAR(4) DEFAULT \"1000\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_manage_settings VARCHAR(4) DEFAULT \"1000\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_invite_player VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_kick_player VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_place_shop VARCHAR(4) DEFAULT \"1110\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_bank_withdraw VARCHAR(4) DEFAULT \"1000\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_bank_deposit VARCHAR(4) DEFAULT \"1110\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_promote_player VARCHAR(4) DEFAULT \"1000\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_remote_player VARCHAR(4) DEFAULT \"1000\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_place_warps VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_remove_warps VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_upgrade_prestige VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_upgrade_phase VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_select_prestige VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island ADD perm_select_phase VARCHAR(4) DEFAULT \"1100\";");}catch(Exception e){}
            //création de la table des joueurs
            try{statement.execute("CREATE TABLE t_user (name VARCHAR(20));");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_user ADD island_id INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_user ADD user_island_grade INT DEFAULT 1;");}catch(Exception e){}
            //création de la table des invitations d'is
            try{statement.execute("CREATE TABLE t_pending_island_invite (name VARCHAR(20) NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_pending_island_invite ADD island_name VARCHAR(50) NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_pending_island_invite ADD island_invitation_sender VARCHAR(20) NOT NULL;");}catch(Exception e){}
            //création de la table des island warps
            try{statement.execute("CREATE TABLE t_island_warp (name VARCHAR(50) NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD island_id INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD warp_x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD warp_y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD warp_z INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD yaw INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_warp ADD pitch INT NOT NULL;");}catch(Exception e){}
            //création de la table des island ban
            try{statement.execute("CREATE TABLE t_island_ban (island_id INT NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_ban ADD banned_player VARCHAR(20);");}catch(Exception e){}
            //création de la table des puits
            try{statement.execute("CREATE TABLE t_wells (island_name VARCHAR(500) NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_wells ADD material VARCHAR(100) NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_wells ADD number INT DEFAULT 1;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_wells ADD x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_wells ADD y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_wells ADD z INT NOT NULL;");}catch(Exception e){}
            //création de la table des holograms
            try{statement.execute("CREATE TABLE t_holo (x FLOAT NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_holo ADD y FLOAT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_holo ADD z FLOAT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_holo ADD world VARCHAR(100) NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_holo ADD type VARCHAR(100) NOT NULL;");}catch(Exception e){}
            //création de la table des quêtes journalières
            try{statement.execute("CREATE TABLE t_island_daily_quest (island_id INT NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD day INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD month INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD number INT DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD quest_id INT DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD completed INT DEFAULT 0;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_daily_quest ADD rewarded INT DEFAULT 0;");}catch(Exception e){}
            //création de la tables des is shop
            try{statement.execute("CREATE TABLE t_island_shop (island_name varchar(500) NOT NULL);");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD chest_x INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD chest_y INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD chest_z INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD sign_direction VARCHAR(100) NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD price INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD item VARCHAR(15000) NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD buy_or_sell TINYINT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD floating_armostand_id INT NOT NULL;");}catch(Exception e){}
            try{statement.execute("ALTER TABLE t_island_shop ADD floating_item_id INT NOT NULL;");}catch(Exception e){}
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void connect(String host, int port, String database, String user, String password){
        if(!isConnected()){
            try{
                conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", user, password);
                Bukkit.getServer().getConsoleSender().sendMessage("[Vyraah-OneBlock] - Connexion de la base de donnée effectué !");
            }catch(SQLException e){
                throw new RuntimeException(e);
            }
        }
    }

    public void disconnect(){
        if(isConnected()){
            try {
                conn.close();
                Bukkit.getServer().getConsoleSender().sendMessage("[Vyraah-OneBlock] - Déconnexion de la base de donnée effectué !");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isConnected(){
        try {
            if((conn == null) || (conn.isClosed()) || conn.isValid(5)) {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public Connection getConnection(){
        return conn;
    }

}