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

import javax.xml.transform.Result;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MySQL {
    private Connection conn;

    public static void createIsland(Player creator, String islandName, int x, int y, int z) throws SQLException {
        String islandQuery = "INSERT INTO ISLAND (islandName, islandCenterX, islandCenterY, islandCenterZ, islandOneBlockX, islandOneBlockY, islandOneBlockZ, islandSpawnX, islandSpawnY, islandSpawnZ, islandQuestDay, islandQuestMonth) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
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
            ps1.setInt(11, LocalDateTime.now().getDayOfMonth());
            ps1.setInt(12, LocalDateTime.now().getMonthValue());

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
        String sql = "SELECT islandId FROM PLAYER WHERE playerUUID = ? AND islandId IS NOT NULL";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, p.getUniqueId().toString());

            ResultSet result = ps.executeQuery();

            if(result.next()) return true;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static String getIslandNameByPlayer(String playerName){
        String sql = """
                SELECT islandName
                FROM ISLAND i
                INNER JOIN PLAYER p ON i.islandId = p.islandId
                WHERE playerName = ?
                """;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, playerName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getString("islandName");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return "";
    }

    public static Location getCenterLocationByIslandName(String islandName){
        String sql = "SELECT islandCenterX AS X, islandCenterY AS Y, islandCenterZ AS Z FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet res = ps.executeQuery();
            if(res.next()){
                return new Location(Bukkit.getWorld("islands"), res.getInt("X"), res.getInt("Y"), res.getInt("Z"));
            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException("erreur lors de l'execution du SQL");
        }
        throw new RuntimeException("L'ile ciblée semble introuvble");
    }

    public static boolean inSameIsland(String player1, String player2){
        String sql = """
                SELECT p1.islandId
                FROM PLAYER p1
                INNER JOIN PLAYER p2 ON p1.islandId = p2.islandId
                WHERE p1.playerName = ?
                AND p2.playerName = ?
                """;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, player1);
            ps.setString(2, player2);
            ResultSet res = ps.executeQuery();
            if(res.next()) return true;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isPlayerOnHisIsland(Player p){
        return isLocationIsInPlayerIsland(p, p.getLocation());
    }

    public static boolean isLocationIsInPlayerIsland(Player p, Location loc){
        String islandName = getIslandNameByPlayer(p.getName());
        Location center = getCenterLocationByIslandName(islandName);
        int radius = Main.INSTANCE.radiusLevel.get(getIslandPrestigeByIslandName(islandName));
        //Vérification sur un plan 2D de la distance des coordonnés
        return loc.add(0, -loc.getY(), 0).distance(center.add(0, -center.getY(), 0)) <= radius;
    }

    public static String getOnWhichIslandIsLocation(Location loc){
        if(!loc.getWorld().getName().equalsIgnoreCase("islands")){return "Error you are not in the good world";}
        int x = (int) loc.getX();
        int z = (int) loc.getZ();
        int remainsX = x % 3000;
        int remainsZ = z % 3000;
        //Les iles sont espacés de 3000 chacunes : Ex : loc.x = 4600 = loc.x % 3000 > 1500 donc x = 4600 + 3000 - 1600 = 6000
        x += remainsX < 1500 ? - remainsX : 3000 - remainsX;
        z += remainsZ < 1500 ? - remainsZ : 3000 - remainsZ;
        String sql = "SELECT islandName FROM ISLAND WHERE islandCenterX = ? AND islandCenterZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, x);
            ps.setInt(2, z);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getString("islandName");
        }catch (SQLException e){
            e.printStackTrace();
        }
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
        String sql = "SELECT gradeHierarchy FROM PLAYER p INNER JOIN GRADE g ON p.gradeId = g.gradeId WHERE playerName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, playerName);
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIslandLevelByPlayer(Player p){
        String sql = "SELECT islandLevel FROM ISLAND i INNER JOIN PLAYER p ON i.islandId = p.islandId WHERE playerUUID = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, p.getUniqueId().toString());
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIslandPrestigeByPlayer(Player p){
        String sql = "SELECT islandPrestige FROM ISLAND i INNER JOIN PLAYER p ON i.islandId = p.islandId WHERE playerUUID = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, p.getUniqueId().toString());
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIslandPrestigeByIslandName(String islandName){
        String sql = "SELECT islandPrestige FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIslandIdByIslandName(String islandName){
        String sql = "SELECT islandId FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIslandIdByPlayer(String player){
        String sql = "SELECT islandId FROM PLAYER WHERE playerName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, player);
            ResultSet res = ps.executeQuery();
            if(res.next()) return res.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static ArrayList<String> getIslandPlayers(String islandName){
        int islandId = getIslandIdByIslandName(islandName);
        ArrayList<String> players = new ArrayList<>();
        String sql = "SELECT playerName FROM PLAYER WHERE islandId = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, islandId);
            ResultSet result = ps.executeQuery();
            while(result.next()) players.add(result.getString(1));
        }catch(SQLException e){
            e.printStackTrace();
        }
        return players;
    }

    public static ArrayList<String> getIslandWarpsName(){
        ArrayList<String> warps = new ArrayList<>();
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement("SELECT warpName FROM WARP")){
            ResultSet result = ps.executeQuery();
            while(result.next()) warps.add(result.getString(1));
        }catch (SQLException e){
            e.printStackTrace();
        }
        return warps;
    }

    public static Location getObLocationByIslandName(String isName){
        String sql = "SELECT islandOneBlockX as x, islandOneBlockY as y, islandOneBlockZ as z FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, isName);
            ResultSet result = ps.executeQuery();
            if(result.next()){
                return new Location(Bukkit.getWorld("islands"), result.getInt("x"), result.getInt("y"), result.getInt("z"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static Location getSpawnLocationByIslandName(String isName){
        String sql = "SELECT islandSpawnX as x, islandSpawnY as y, islandSpawnZ as z FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, isName);
            ResultSet result = ps.executeQuery();
            if(result.next()){
                return new Location(Bukkit.getWorld("islands"), result.getInt("x"), result.getInt("y"), result.getInt("z"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static int howManyPlayersHasIsland(String islandName){
        String sql = "SELECT COUNT(*) AS nbP FROM PLAYER p INNER JOIN ISLAND i ON p.islandId = i.islandId WHERE islandName = ? ";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt("nbP");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
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
        return 0;
    }

    public static int getObBankMoney(String islandName){
        return 0;
    }

    public static int getActivePrestige(String islandName){
        return 0;
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