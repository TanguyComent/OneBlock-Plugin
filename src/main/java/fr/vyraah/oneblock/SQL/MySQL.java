package fr.vyraah.oneblock.SQL;

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
import java.util.*;

public class MySQL {
    private Connection conn;

    public static boolean playerFirstConnexion(Player p){
        String UUID = p.getUniqueId().toString(),
               sql = "SELECT COUNT(*) AS nb FROM PLAYER WHERE playerUUID = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, UUID);
            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1) == 0;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return true;
    }

    public static void registerPlayer(Player p){
        String sql = "INSERT INTO PLAYER (playerUUID, playerName) VALUES (?, ?)";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, p.getUniqueId().toString());
            ps.setString(2, p.getName());

            ps.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void createIsland(Player creator, String islandName, int x, int y, int z) throws SQLException {
        String islandQuery = "INSERT INTO ISLAND (islandName, islandCenterX, islandCenterY, islandCenterZ, islandOneBlockX, islandOneBlockY, islandOneBlockZ, islandSpawnX, islandSpawnY, islandSpawnZ, islandQuestDay, islandQuestMonth) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        String banqueQuery = "INSERT INTO BANQUE (islandId) VALUES (?)";
        String gradeQuery  = """
                              INSERT INTO GRADE (islandId, gradeName, gradeColor, gradeHierarchy)
                              VALUES
                              (?, 'Visitor', 'g', 1), (?, 'Member', 'g', 2), (?, 'Administrator', 'y', 3),  (?, 'Co-leader', 'o', 4), (?, 'Leader', 'r', 5)
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

            //Insersion de la banque d'ile
            ps2.setInt(1, islandId);

            ps2.executeUpdate();

            //Insersion des grade par default de l'ile
            for(int i = 1; i <= 5; i++){
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
            //Plus la hierarchy du grade est haute, plus le grade a de permission
            int chefGradeId = 0;
            try(var generatedId = ps3.getGeneratedKeys()){
                for(int i = 1; i <= 5; i++){
                    generatedId.next();
                    int gradeId = generatedId.getInt(1);
                    //Recuperation de l'id du grade "leader"
                    if(i == 5) chefGradeId = gradeId;
                    //boucle de toutes les map
                    for(Map.Entry<String, Integer> entry : map.entrySet()){
                        ps4.setInt(1, gradeId);
                        ps4.setString(2, entry.getKey());
                        ps4.setByte(3, (byte) (i >= entry.getValue() ? 1 : 0));
                        ps4.addBatch(); // Ajout à un lot
                    }
                }
            }

            ps4.executeBatch();

            //Insersion du joueurs dans l'ile avec le grade chef, suppression de l'ile si erreur
            if(!addPlayerToIsland(creator, islandId, chefGradeId)){
                con.rollback();
                throw new RuntimeException("Failed to add player to the island");
            }

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
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean updateObTimesBreak(int newValue, String islandName){
        String sql = "update island set islandObTimesBreak = ? where islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, newValue);
            ps.setString(2, islandName);

            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean hasInvitation(UUID uuid, int islandId){
        String sql = "SELECT COUNT(*) FROM ISLAND_INVITE WHERE playerUUID = ? AND islandId = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, uuid.toString());
            ps.setInt(2, islandId);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1) == 1;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static ArrayList<String> getIslandInvite(Player p){
        String sql = "SELECT islandName FROM ISLAND_INVITE NATURAL JOIN ISLAND WHERE playerUUID = ?";
        ArrayList<String> invitations = new ArrayList<>();
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, p.getUniqueId().toString());
            ResultSet result = ps.executeQuery();
            while(result.next()) invitations.add(result.getString(1));
        }catch (SQLException e){
            e.printStackTrace();
        }
        return invitations;
    }

    public static void invitePlayerToIsland(String islandName, Player p){
        String sql = "INSERT INTO ISLAND_INVITE (islandId, playerUUID) VALUES (?,?)";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, getIslandIdByIslandName(islandName));
            ps.setString(2, p.getUniqueId().toString());

            ps.executeUpdate();
        }catch(SQLException e){
            if(e.getErrorCode() == 1062)
                throw new RuntimeException("AlreadyInvited");
            throw new RuntimeException();
        }
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

    public static ArrayList<Location> getWellsList(int islandId){
        ArrayList<Location> wells = new ArrayList<>();
        String sql = "SELECT wellX AS X, wellY AS Y, wellZ AS Z FROM WELL WHERE islandId = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, islandId);
            ResultSet result = ps.executeQuery();
            while(result.next())
                wells.add(new Location(Bukkit.getWorld("islands"), result.getInt("x"), result.getInt("y"), result.getInt("z")));
        }catch (SQLException e){
            e.printStackTrace();
        }
        return wells;
    }

    public static int getWellNbr(Location wellLoc){
        String sql = "SELECT wellNbr FROM WELL WHERE wellX = ? AND wellY = ? AND wellZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, (int) wellLoc.getX());
            ps.setInt(2, (int) wellLoc.getY());
            ps.setInt(3, (int) wellLoc.getZ());

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static ArrayList<ArrayList<Object>> getWellsInformation(int islandId){
        ArrayList<ArrayList<Object>> wellsInfo = new ArrayList<>();
        String sql = "SELECT wellMaterial AS mat, wellNbr FROM WELL WHERE islandId = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, islandId);

            ResultSet result = ps.executeQuery();
            while(result.next()){
                ArrayList<Object> list = new ArrayList<>();
                list.add(Material.getMaterial(result.getString("mat")));
                list.add(result.getInt("wellNbr"));
                wellsInfo.add(list);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return wellsInfo;
    }

    public static ArrayList<ArrayList<Object>> getIslandTop(int maxTop){
        ArrayList<ArrayList<Object>> islandTop = new ArrayList<>();
        String sql = "SELECT islandName, islandLevel FROM ISLAND ORDER BY islandLevel LIMIT ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, maxTop);
            ResultSet result = ps.executeQuery();
            while(result.next()){
                ArrayList<Object> island = new ArrayList<>();
                island.add(result.getString("islandName"));
                island.add(result.getInt("islandLevel"));
                islandTop.add(island);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return islandTop;
    }

    public static ArrayList<Location> getScoreboard(){
        ArrayList<Location> sbs = new ArrayList<>();
        String sql = "SELECT sbWorld, sbX, sbY, sbZ FROM SCOREBOARD";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ResultSet result = ps.executeQuery();
            while(result.next()){
                String world = result.getString("sbWorld");
                int x = result.getInt("sbX");
                int y = result.getInt("sbY");
                int z = result.getInt("sbZ");
                sbs.add(new Location(Bukkit.getWorld(world), x, y, z));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return sbs;
    }

    public static int getObTimesBreak(String islandName){
        String sql = "SELECT islandObTimesBreak FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static String getObPhase(String islandName){
        String sql = "SELECT islandActivePhase AS ap FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getString("ap");
        }catch(SQLException e){
            e.printStackTrace();
        }
        return "";
    }

    public static int getObQuestNbr(String islandName){
        String sql = "SELECT islandQuestId FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int getObBankMoney(String islandName){
        String sql = """
                SELECT banqueMoney FROM BANQUE B
                INNER JOIN ISLAND I ON B.islandId = I.islandId
                WHERE islandName = ?
                """;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int getActivePrestige(String islandName){
        String sql = "SELECT islandActivePrestige FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean alterBankSold(String islandName, long value, int operation){
        if(value <= 0) return false;
        int actualMoney = getObBankMoney(islandName);
        String sql = """
                UPDATE BANQUE B
                INNER JOIN ISLAND I ON B.islandId = I.islandId
                SET banqueMoney = ?
                WHERE islandName = ?
                """;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            long newSold = ((operation == 1) ? actualMoney + value : actualMoney - value);
            ps.setLong(1, newSold);
            ps.setString(2, islandName);
            ps.executeUpdate();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return true;
    }

    public static void updateQuest(String islandName){
        Random r = new Random();
        int realDay = LocalDateTime.now().getDayOfMonth();
        int realMonth = LocalDateTime.now().getMonthValue();
        int questDay = 0, questMonth = 0;
        String getDate = "SELECT islandQuestDay AS day, islandQuestMonth AS month FROM ISLAND WHERE islandName = ?",
               update = "UPDATE ISLAND SET islandQuestDay = ?, islandQuestMonth = ?, islandQuestId = ? WHERE islandName = ?";
        try(PreparedStatement dateStatement = Main.INSTANCE.mysql.getConnection().prepareStatement(getDate);
            PreparedStatement updateStatement = Main.INSTANCE.mysql.getConnection().prepareStatement(update)){
            dateStatement.setString(1, islandName);
            ResultSet result = dateStatement.executeQuery();
            if(result.next()){
                questDay = result.getInt("day");
                questMonth = result.getInt("month");
            }
            if(realDay == questDay && realMonth == questMonth) return;
            int questNbr = r.nextInt(8) + 1;
            updateStatement.setInt(1, realDay);
            updateStatement.setInt(2, realMonth);
            updateStatement.setInt(3, questNbr);
            updateStatement.setString(4, islandName);

            updateStatement.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static boolean hasQuestX(String islandName, int questId){
        return getDailyQuestId(islandName) == questId;
    }

    public static int getDailyQuestId(String islandName){
        String sql = "SELECT islandQuestId FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int getDailyQuestNumber(String islandName){
        String sql = "SELECT islandQuestNbr FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static void incrementQuest(String islandName, int toAdd){
        String sql = "UPDATE ISLAND SET islandQuestNbr = ? WHERE islandName = ?";
        int newNbr = getObQuestNbr(islandName) + toAdd;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setInt(1, newNbr);
            ps.setString(2, islandName);
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static boolean isQuestCompleted(String islandName){
        String sql = "SELECT islandCompletedDailyQuest FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1) == 1;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void completeQuest(String islandName){
        String sql = "UPDATE ISLAND SET islandCompletedDailyQuest = 1 WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static boolean hasBeenRewarded(String islandName){
        String sql = "SELECT islandQuestRewarded FROM ISLAND WHERE islandName = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);

            ResultSet result = ps.executeQuery();
            if(result.next()) return result.getInt(1) == 1;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    private static ArrayList<ArrayList<Object>> getIslandShopItems(Player p, String islandName){
        ArrayList<ArrayList<Object>> items = new ArrayList<>();
        String sql = """
                SELECT shopX AS x, shopY AS y, shopZ AS z, shopItem AS item
                FROM SHOP S
                INNER JOIN ISLAND_SHOP ISH ON S.shopId = ISH.shopId
                INNER JOIN ISLAND I ON ISH.islandId = I.islandId
                WHERE islandName = ?
                """;
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            ps.setString(1, islandName);
            ResultSet result = ps.executeQuery();
            while(result.next()){
                ArrayList<Object> item = new ArrayList<>();
                item.add(Main.deserializedObject(result.getString("item")));
                item.add(new Location(p.getWorld(), result.getInt("x"), result.getInt("y"), result.getInt("z")));
                items.add(item);
            }
        }catch(Exception e) {
            e.printStackTrace();
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
        String sql = "SELECT armorstandId FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);
            ResultSet result = ps.executeQuery();
            if(result.next()){
                return result.getInt("armorstandId");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int getItemId(Location loc){
        String sql = "SELECT shopId FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);
            ResultSet result = ps.executeQuery();
            if(result.next()){
                return result.getInt("shopId");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isLocationShop(Location loc){
        if(loc.getBlock().getType() != Material.CHEST) return false;
        String sql = "SELECT islandName FROM SHOP NATURAL JOIN ISLAND_SHOP NATURAL JOIN ISLAND WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();
            String islandName = "ERROR";
            while(result.next()){
                islandName = result.getString("islandName");
            }
            return !islandName.equalsIgnoreCase("ERROR");
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static BlockFace getShopFacing(Location loc){
        String sql = "SELECT shopDirection FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();
            String facing = null;
            while(result.next()){
                facing = result.getString("shopDirection");
            }
            return BlockFace.valueOf(facing);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static void removeFloatingShopItem(Player p, Location loc){
        int asId = -666;
        int itId = -666;
        String sql = "SELECT shopId AS itId, armorstandId FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();

            while(result.next()){
                asId = result.getInt("armorstandId");
                itId = result.getInt("shopId");
            }
            if(asId == -666 || itId == -666) return;
        }catch(Exception e){
            e.printStackTrace();
        }
        ServerPlayer sp = ((CraftPlayer) p).getHandle();
        ServerGamePacketListenerImpl con = sp.connection;
        con.send(new ClientboundRemoveEntitiesPacket(asId, itId));
    }

    public static ItemStack getIslandShopItem(Location loc){
        String sql = "SELECT shopItem FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();
            if(result.next()) return (ItemStack) Main.deserializedObject(result.getString("shopItem"));
        }catch(Exception e){
            e.printStackTrace();
        }
        return new ItemStack(Material.AIR);
    }

    public static int getIslandShopPrice(Location loc){
        String sql = "SELECT shopPrice FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();
            if(result.next()) result.getInt(1);
        }catch(Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    public static ShopType getIslandShopType(Location loc){
        int type = 0;
        String sql = "SELECT shopType FROM SHOP WHERE shopX = ? AND shopY = ? AND shopZ = ?";
        try(PreparedStatement ps = Main.INSTANCE.mysql.getConnection().prepareStatement(sql)){
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ResultSet result = ps.executeQuery();
            if(result.next()) type = result.getInt(1);

        }catch(Exception e){throw new RuntimeException(e);}
        return (type == 1) ? ShopType.buy : ShopType.sell;
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