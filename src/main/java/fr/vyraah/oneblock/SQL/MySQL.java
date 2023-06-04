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

    public static int getPlayerGrade(String playerName){
        return getInformationByNameInt(playerName, "t_user", "user_island_grade");
    }

    public static int getIslandLevelByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "level");
    }

    public static int getIslandPrestigeByPlayer(Player p){
        return getInformationByNameInt(getIslandNameByPlayer(p.getName()), "t_island", "prestige_level");
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
            try{statement.execute("ALTER TABLE t_island ADD level FLOAT NOT NULL DEFAULT 0;");}catch(Exception e){}
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
            try{statement.execute("ALTER TABLE t_island ADD bank INT DEFAULT 0;");}catch(Exception e){}
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
        }catch(Exception e){
            throw new RuntimeException(e);
        }
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