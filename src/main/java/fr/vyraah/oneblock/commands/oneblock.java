package fr.vyraah.oneblock.commands;

import com.github.yannicklamprecht.worldborder.api.BorderAPI;
import com.github.yannicklamprecht.worldborder.api.WorldBorderApi;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;

public class oneblock implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String arg, @NotNull String[] args) {

        // ATTENTION: NOUS VOULONS JUSTE UN /IS QUI VERIFIE SI LE JOUEUR EST DEJA DANS UNE ILE
        // SI OUI, LE PLUGIN LUI OUVRE LE PANEL
        // SINON
        // IL FAITL A CREATION DE L'ILE AVEC LA CREATION DES VALEURS DANS LA DB :)

        if(sender instanceof Player p) {
            if(args.length == 0){
                if(MySQL.getPlayerHaveAnIsland(p)){
                    //ouvrir le pannel
                    return true;
                }else{
                    p.sendMessage("§4Tu n'as pas d'ile crée en une grace à la commande /is create <nom>");
                    return true;
                }
            }
            switch (args[0]) {

                // CMD : /is create || /is cr
                case "create", "cr" -> {
                    //mauvaise utilisation de la commande
                    if (args.length == 1 || args.length > 2) {
                        p.sendMessage("§4/is create <nom> pour créer une ile");
                        return false;
                    }
                    String islandName = args[1];

                    //creation des data

                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage("§4tu as deja une ile");
                        return false;
                    }
                    int x = Main.INSTANCE.config.getInt("next_island_x");
                    int z = Main.INSTANCE.config.getInt("next_island_z");
                    try{
                        MySQL sql = Main.INSTANCE.mysql;
                        Statement statement = sql.getConnection().createStatement();

                        //creation des data de l'is
                        statement.execute(String.format("""
                                INSERT INTO t_island (name, center_x, center_z, center_y)
                                VALUES ("%s", %d, %d, 0);
                                """, islandName, x, z));

                        //relation joueur / is
                        int islandId = MySQL.getInformationByNameInt(islandName,"t_island" , "id");
                        if(islandId == 0){
                            p.sendMessage("§4Erreur lors de la création de l'ile");
                            return false;
                        }
                        statement.execute(String.format("""
                                INSERT INTO t_user (name, island_id)
                                VALUES ("%s", %d);
                                """, p.getName(), islandId));
                    }catch (SQLIntegrityConstraintViolationException e){
                        p.sendMessage("§4Cette ile existe deja, veuiller choisir un autre nom !");
                    } catch (Exception e){
                        p.sendMessage("§4ERREUR INCONNUE : votre ile n'a pas pu etre cree veuillez reessayer");
                        return false;
                    }

                    //island creation

                    Location loc = new Location(Bukkit.getWorld("islands"), x, 0, z);
                    loc.getBlock().setType(Material.OAK_WOOD);

                    if( x == z ){ x += 3000; } else { z += 3000; }
                    Main.INSTANCE.config.set("next_island_x", x);
                    Main.INSTANCE.config.set("next_island_z", z);
                    Main.INSTANCE.saveConfig();
                    p.sendMessage("§2Ton ile a bien ete cree fait /is go pour y etre teleporte !");
                    return true;
                }

                // CMD : /is go
                case "go" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage("§4Tu n as pas d ile !");
                    }
                    teleportPlayerToIsland(p, MySQL.getIslandNameByPlayer(p));
                    return true;
                }

                // CMD : /is invite
                case "invite" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage("§4Il te faut une ile pour pouvoir inviter des joueurs ! Commence par faire /is create <nom>");
                        return false;
                    }

                    //check if player has island permission to invite player into

                    String targetName = args[1];
                    Player target = Bukkit.getPlayer(targetName);

                    //return en cas d'auto invitation (s'inviter sois même)
                    if(p.getName().equals(targetName)){
                        p.sendMessage("§4Tu ne peut pas t auto inviter");
                        return false;
                    }

                    //on essaye d'inviter le joueur dans l'ile, en passant ça verifie si le joueur existe et est co
                    try{
                        //on verifie que la cible n'ai pas deja une ile
                        if(MySQL.getPlayerHaveAnIsland(target)){
                            p.sendMessage("§4Le joueur " + targetName + " a deja une ile !");
                            return false;
                        }
                        TextComponent accept = new TextComponent("§2[accepter]    ");
                        TextComponent deny = new TextComponent("    §4[refuser]");
                        target.sendMessage("§aLe joueur " + p.getName() + " t invite a rejoindre son ile !");
                        target.spigot().sendMessage(accept,deny);
                    }catch (NullPointerException e){
                        //ici, le joueur n'existe pas
                        p.sendMessage("§4Le joueur " + targetName + " n existe pas ou est deconnecte");
                        return false;
                    }

                    //enregistrement de l'invitation dans la bdd
                    try{
                        Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
                        statement.execute(String.format("""
                                INSERT INTO t_pending_island_invite (name, island_name, island_invitation_sender)
                                VALUES ("%s", "%s", "%s");
                                """, target.getName(), MySQL.getIslandNameByPlayer(p), p.getName()));
                        p.sendMessage("§2Le joueur " + target.getName() + " a bien été inviter sur votre ile !");
                    } catch (SQLException e) {
                        p.sendMessage("§4Une erreur est survenue");
                        return false;
                    }
                }

                // CMD : /is kick
                case "kick" -> {
                    String target = args[1];
                    int userPermission = MySQL.getInformationByNameInt(p.getName(), "t_user", "user_island_grade");
                    int targetPermission = MySQL.getInformationByNameInt(target, "t_user", "user_island_grade");

                    //regarder si le joueur a la perm de ban

                    if(userPermission <= targetPermission){
                        p.sendMessage("§4Vous ne pouvez pas kick ce joueur de votre ile !");
                        return false;
                    }

                    try {
                        Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
                        statement.execute("DELETE FROM t_user WHERE name=\"" + target + "\";");
                        p.sendMessage("§2 Le joueur §6" + target + " a bien ete kick de votre ile !");
                    }catch(Exception e){}
                }

                // CMD : /is join
                case "join" -> {
                    //on regarde si le joueur a pas deja une ile (normalement non mais on sais jamais)
                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage("§4Tu as deja une ile !");
                        return false;
                    }

                    String target = args[1];
                    boolean validInvite = false;
                    try{
                        //on va chercher l'invitation dans la bdd
                        Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
                        ResultSet result = statement.executeQuery("SELECT * FROM t_pending_island_invite WHERE name=\"" + p.getName() + "\";");
                        String stringResultPlayer = "";
                        String stringResultIsland = "";
                        int islandId = -1;
                        while(result.next()){
                            stringResultIsland = result.getString("island_name");
                            stringResultPlayer = result.getString("island_invitation_sender");
                        }
                        //si l'invitation est un nom d'ile
                        if(stringResultIsland.equalsIgnoreCase(target)){
                            validInvite = true;
                            islandId = MySQL.getInformationByNameInt(target, "t_island", "id");
                        }
                        //si l'invitation est un pseudo de joueur
                        if(stringResultPlayer.equalsIgnoreCase(target)){
                            validInvite = true;
                            islandId = MySQL.getInformationByNameInt(target, "t_user", "island_id");
                        }
                        //ajout du joueur a l'is via la bdd (table t_user)
                        if(validInvite && islandId != -1){
                            statement.execute(String.format("""
                                    INSERT INTO t_user (name, island_id, user_island_grade)
                                    VALUES ("%s", %d, %d);
                                    """, p.getName(), islandId, 4));
                            statement.execute("DELETE FROM t_pending_island_invite WHERE name=\"" + p.getName() + "\";");
                            p.sendMessage("§2Tu a bien rejoin l ile " + MySQL.getIslandNameByPlayer(p) + " !");
                        }else{
                            p.sendMessage("§4 invitation invalide : " + target);
                        }
                        return true;
                    }catch (Exception e){return false;}
                }
            }
        }
        return false;
    }

    public void teleportPlayerToIsland(Player p, String islandName){
        int x = MySQL.getInformationByNameInt(islandName, "t_island", "center_x");
        int y = MySQL.getInformationByNameInt(islandName, "t_island", "center_y");
        int z = MySQL.getInformationByNameInt(islandName, "t_island", "center_z");
        int prestigeLvl = MySQL.getInformationByNameInt(islandName, "t_island", "prestige_level");
        Location loc = new Location(Bukkit.getWorld("islands"), x, y + 2, z);
        p.teleport(loc);
        int borderSize = switch(prestigeLvl){
            case 2 -> 150;
            case 3 -> 450;
            case 4 -> 1000;
            case 5 -> 2500;
            default -> 50;
        };
        Main.INSTANCE.worldBorderApi.setBorder(p, borderSize, loc);
    }
}
