package fr.vyraah.oneblock.SQL;

import fr.vyraah.oneblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;

public class MySQL {
    private Connection conn;


    // =========================================================================================
    //                             GET INFORMATION PLAYER ISLAND
    // =========================================================================================

    public static boolean getPlayerHaveAnIsland(Player p){

        // ICI TU VAS VERIFIER SI LE JOUEUR EST DEJA DANS UNE ILE (Donc dans une liste, dans la db tu vas get toutes les iles).
        // SI IL N'EST PAS DEDANS TU AURAS JUSTE A RETURN FALSE;
        // SINON
        // TU RETURN TRUE ET TU APPELLES LA VARIABLE DANS TA CLASSE ONEBLOCK.JAVA DE TA CMD

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

    // =========================================================================================
    //                             CONNECTION & DECONNEXION DE LA DB
    // =========================================================================================

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