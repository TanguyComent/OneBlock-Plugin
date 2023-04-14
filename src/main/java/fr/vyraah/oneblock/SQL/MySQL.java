package fr.vyraah.oneblock.SQL;

import fr.vyraah.oneblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;

public class MySQL {
    private Connection conn;


    // =========================================================================================
    //                             GET INFORMATION PLAYER ISLAND
    // =========================================================================================

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

    public static String getIslandNameByPlayer(Player p){
        String islandName = "";
        try{
            Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM t_user WHERE name=\"" + p.getName() + "\";");
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
            ResultSet result = statement.executeQuery("SELECT * FROM " + table + " WHERE name=\"" + name + "\";");
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
        if(!loc.getWorld().getName().equals("islands")) return false;
        int x = getInformationByNameInt(getIslandNameByPlayer(p), "t_island", "center_x");
        int z = getInformationByNameInt(getIslandNameByPlayer(p), "t_island", "center_z");
        int px = (int) loc.getX();
        int pz = (int) loc.getZ();
        int radius = switch(getInformationByNameInt(getIslandNameByPlayer(p), "t_island", "prestige_level")){
            case 2 -> 50;
            case 3 -> 75;
            case 4 -> 125;
            case 5 -> 200;
            default -> 25;
        };
        return px <= x + radius && px >= x - radius && pz <= z + radius && pz >= z - radius;
    }

    public static String getOnWhichIslandIsLocation(Location loc){
        if(!loc.getWorld().getName().equalsIgnoreCase("islands")){return "Error you are not in the good world";}
        int x = 0;
        int z = 0;
        while(true){
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
        return "";
    }

    public static int getPlayerGrade(Player p){
        return getInformationByNameInt(p.getName(), "t_user", "user_island_grade");
    }

    public static int getIslandLevelByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p), "t_island", "level");
    }

    public static int getIslandPrestigeByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p), "t_island", "prestige_level");
    }

    public static ArrayList<String> getIslandPlayers(String islandName){
        ArrayList<String> players = new ArrayList<>();
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            ResultSet result = statement.executeQuery("SELECT * FROM t_user WHERE island_id=" + getInformationByNameInt(islandName, "t_island", "id") + ";");
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

    // =========================================================================================
    //                             CONNECTION & DECONNEXION DE LA DB
    // =========================================================================================

    public static void initDatabase(){
        try(Statement statement = Main.INSTANCE.mysql.conn.createStatement()){
            statement.execute("CREATE TABLE t_island (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(500) NOT NULL UNIQUE, prestige_level INT NOT NULL DEFAULT 1, level FLOAT NOT NULL DEFAULT 0, center_x INT NOT NULL, center_z INT NOT NULL, center_y INT NOT NULL, oneblock_x INT NOT NULL, oneblock_y INT NOT NULL, oneblock_z INT NOT NULL, spawn_x INT NOT NULL, spawn_y INT NOT NULL, spawn_z INT NOT NULL, allow_visitors INT NOT NULL DEFAULT TRUE);");
            statement.execute("""
                    CREATE TABLE t_user
                    (
                    name VARCHAR(20),
                    island_id INT NOT NULL,
                    user_island_grade INT DEFAULT 1
                    );
                    """);
            statement.execute("""
                    CREATE TABLE t_pending_island_invite
                    (
                    name VARCHAR(20) NOT NULL,
                    island_name VARCHAR(50) NOT NULL,
                    island_invitation_sender VARCHAR(20) NOT NULL
                    );
                    """);
            statement.execute("""
                    CREATE TABLE t_island_warp
                    (
                    name VARCHAR(50) NOT NULL,
                    island_id INT NOT NULL,
                    warp_x INT NOT NULL,
                    warp_y INT NOT NULL,
                    warp_z INT NOT NULL,
                    yaw INT NOT NULL,
                    pitch INT NOT NULL
                    );
                    """);
        }catch(Exception e){}
    }
    public void connect(String host, int port, String database, String user, String password){
        if(!isConnected()){
            try{
                conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", user, password);
                Bukkit.getServer().getConsoleSender().sendMessage("[Vyraah-OneBlock] - Connexion de la base de donnée effectué !");
            } catch (SQLException e){
                e.printStackTrace();
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