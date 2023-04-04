package fr.vyraah.oneblock.commands;

import com.github.yannicklamprecht.worldborder.api.BorderAPI;
import com.github.yannicklamprecht.worldborder.api.WorldBorderApi;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.guis.guis;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.Color;
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
            String prefix = net.md_5.bungee.api.ChatColor.of("#fb1010") +"V" + net.md_5.bungee.api.ChatColor.of("#fb3111") + "y" + net.md_5.bungee.api.ChatColor.of("#fc5311") + "r" + net.md_5.bungee.api.ChatColor.of("#fc7412") + "a" + net.md_5.bungee.api.ChatColor.of("#fc9512") + "a" + net.md_5.bungee.api.ChatColor.of("#fdb713") + "h " + net.md_5.bungee.api.ChatColor.of("#fdd813") + "» ";
            if(args.length == 0){
                if(MySQL.getPlayerHaveAnIsland(p)){
                    //ouvrir le pannel
                    p.openInventory(guis.islandInformations(p));
                    return true;
                }else{
                    p.sendMessage(prefix + ChatColor.RED + "Tu n'as pas d'ile crée en une grace à la commande /is create <nom>");
                    return true;
                }
            }
            switch (args[0]) {

                // CMD : /is create || /is cr
                case "create", "cr" -> {
                    //mauvaise utilisation de la commande
                    if (args.length == 1 || args.length > 2) {
                        p.sendMessage(prefix + ChatColor.RED + "/is create <nom> pour créer une ile");
                        return false;
                    }
                    String islandName = args[1];

                    //creation des data

                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(prefix + ChatColor.RED + "tu as deja une ile");
                        return false;
                    }
                    int x = Main.INSTANCE.config.getInt("next_island_x");
                    int z = Main.INSTANCE.config.getInt("next_island_z");
                    try{
                        MySQL sql = Main.INSTANCE.mysql;
                        Statement statement = sql.getConnection().createStatement();

                        //creation des data de l'is
                        statement.execute(String.format("""
                                INSERT INTO t_island (name, center_x, center_z, center_y, oneblock_x, oneblock_z, oneblock_y, spawn_x, spawn_z, spawn_y)
                                VALUES ("%s", %d, %d, 0, %d, %d, 0, %d, %d, 0);
                                """, islandName, x, z, x, z, x, z));

                        //relation joueur / is
                        int islandId = MySQL.getInformationByNameInt(islandName,"t_island" , "id");
                        if(islandId == 0){
                            p.sendMessage(prefix + ChatColor.RED + "Erreur lors de la création de l'ile");
                            return false;
                        }
                        statement.execute(String.format("""
                                INSERT INTO t_user (name, island_id)
                                VALUES ("%s", %d);
                                """, p.getName(), islandId));
                    }catch (SQLIntegrityConstraintViolationException e){
                        p.sendMessage(prefix + ChatColor.RED + "Cette ile existe deja, veuiller choisir un autre nom !");
                    } catch (Exception e){
                        p.sendMessage(prefix + ChatColor.RED + "ERREUR INCONNUE : votre ile n'a pas pu etre cree veuillez reessayer");
                        return false;
                    }

                    //island creation

                    Location loc = new Location(Bukkit.getWorld("islands"), x, 0, z);
                    loc.getBlock().setType(Material.OAK_WOOD);

                    if( x == z ){ x += 3000; } else { z += 3000; }
                    Main.INSTANCE.config.set("next_island_x", x);
                    Main.INSTANCE.config.set("next_island_z", z);
                    Main.INSTANCE.saveConfig();
                    p.sendMessage(prefix + ChatColor.GREEN + "Ton ile a bien ete cree fait /is go pour y etre teleporte !");
                    return true;
                }

                // CMD : /is go
                case "go", "teleport" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(prefix + ChatColor.RED + "Tu n as pas d ile !");
                    }
                    teleportPlayerToIsland(p, MySQL.getIslandNameByPlayer(p));
                    return true;
                }

                // CMD : /is invite
                case "invite" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        if(args.length == 2){
                            if(args[1].equalsIgnoreCase("list")){
                                //affiche la liste des invitations du joueur
                                try {
                                    Statement statement = Main.INSTANCE.mysql.getConnection().createStatement();
                                    ResultSet result = statement.executeQuery("SELECT * FROM t_pending_island_invite WHERE name=\"" + p.getName() + "\";");
                                    ArrayList<String> invitations = new ArrayList<>();
                                    while(result.next()){
                                        invitations.add(prefix + ChatColor.GOLD + result.getString("island_invitation_sender") + ChatColor.GREEN + "t a invite a rejoindre son ile (" + ChatColor.GOLD +  result.getString("island_name") + ChatColor.GREEN + "§2)");
                                    }
                                    p.sendMessage(prefix + ChatColor.BLUE + "Liste de tes invitations :");
                                    if(invitations.size() == 0) invitations.add(prefix + ChatColor.GREEN + "Vous n'avez aucune invitation");
                                    for(String msg : invitations){
                                        p.sendMessage(msg);
                                    }
                                }catch(Exception e){}

                                return true;
                            }
                        }
                        p.sendMessage(prefix + ChatColor.RED + "Il te faut une ile pour pouvoir inviter des joueurs ! Commence par faire /is create <nom>");
                        return false;
                    }

                    //check if player has island permission to invite player into

                    String targetName = args[1];
                    Player target = Bukkit.getPlayer(targetName);

                    //return en cas d'auto invitation (s'inviter sois même)
                    if(p.getName().equals(targetName)){
                        p.sendMessage(prefix + ChatColor.RED + "Tu ne peut pas t auto inviter");
                        return false;
                    }

                    //on essaye d'inviter le joueur dans l'ile, en passant ça verifie si le joueur existe et est co
                    try{
                        //on verifie que la cible n'ai pas deja une ile
                        if(MySQL.getPlayerHaveAnIsland(target)){
                            p.sendMessage(prefix + ChatColor.RED + "Le joueur " + targetName + " a deja une ile !");
                            return false;
                        }
                        TextComponent accept = new TextComponent(ChatColor.GREEN + "[accepter]    ");
                        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/is join " + p.getName()));
                        TextComponent deny = new TextComponent(ChatColor.RED + "    [refuser]");
                        target.sendMessage(prefix + ChatColor.GREEN + "Le joueur "+ ChatColor.GOLD + p.getName() + ChatColor.GREEN + " t invite a rejoindre son ile !");
                        target.spigot().sendMessage(accept,deny);
                    }catch (NullPointerException e){
                        //ici, le joueur n'existe pas
                        p.sendMessage(prefix + ChatColor.RED + "Le joueur " + targetName + " n existe pas ou est deconnecte");
                        return false;
                    }

                    //enregistrement de l'invitation dans la bdd
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute(String.format("""
                                INSERT INTO t_pending_island_invite (name, island_name, island_invitation_sender)
                                VALUES ("%s", "%s", "%s");
                                """, target.getName(), MySQL.getIslandNameByPlayer(p), p.getName()));
                        p.sendMessage(prefix + ChatColor.GREEN + "Le joueur " + target.getName() + " a bien été inviter sur votre ile !");
                    } catch (SQLException e) {
                        p.sendMessage(prefix + ChatColor.RED + "Une erreur est survenue");
                        return false;
                    }
                }

                // CMD : /is kick
                case "kick" -> {
                    if(args.length == 1){
                        p.sendMessage(prefix + ChatColor.RED + "Vous ne pouvez pas kick de l air ! /is kick <joueur>");
                        return false;
                    }
                    String target = args[1];
                    if(MySQL.getInformationByNameInt(p.getName(), "t_user", "island_id") != MySQL.getInformationByNameInt(target, "t_user", "island_id")){
                        p.sendMessage(prefix + ChatColor.RED + "Ce joueur n est pas dans votre ile !");
                        return false;
                    }
                    int userPermission = MySQL.getInformationByNameInt(p.getName(), "t_user", "user_island_grade");
                    int targetPermission = MySQL.getInformationByNameInt(target, "t_user", "user_island_grade");

                    //regarder si le joueur a la perm de kick

                    if(userPermission >= targetPermission){
                        p.sendMessage(prefix + ChatColor.RED + "Vous ne pouvez pas kick ce joueur de votre ile !");
                        return false;
                    }

                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("DELETE FROM t_user WHERE name=\"" + target + "\";");
                        p.sendMessage(prefix + ChatColor.GREEN + " Le joueur " + ChatColor.GOLD + target + ChatColor.GREEN + " a bien ete kick de votre ile !");
                    }catch(Exception e){return false;}
                }

                // CMD : /is join
                case "join" -> {
                    //on regarde si le joueur a pas deja une ile (normalement non mais on sais jamais)
                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(prefix + ChatColor.RED + "Tu as deja une ile !");
                        return false;
                    }

                    String target = args[1];
                    boolean validInvite = false;
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        //on va chercher l'invitation dans la bdd
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
                            p.sendMessage(prefix + ChatColor.GREEN + "Tu a bien rejoin l ile " + MySQL.getIslandNameByPlayer(p) + " !");
                        }else{
                            p.sendMessage(prefix + ChatColor.RED + "invitation invalide : " + target);
                        }
                        return true;
                    }catch (Exception e){return false;}
                }

                // CMD : /is leave
                case "leave" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(prefix + ChatColor.RED + "Vous n'avez pas d'ile !");
                        return false;
                    }

                    if(MySQL.getPlayerGrade(p) == 1){
                        p.sendMessage(prefix + ChatColor.RED + "Vous êtes le chef de votre ile ! Vous devez donc faire la commande /is disband pour supprimer votre ile");
                        return false;
                    }

                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("DELETE FROM t_user WHERE name=\"" + p.getName() + "\";");
                        p.sendMessage(prefix + ChatColor.GREEN + "Vous avez bien quitté votre ile !");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                // CMD : /is disband
                case "disband" -> {
                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(prefix + ChatColor.RED + "Vous n avez pas d ile");
                        return false;
                    }
                    if(MySQL.getPlayerGrade(p) != 1){
                        p.sendMessage(prefix + ChatColor.RED + "Vous n avez pas la permission !");
                        return false;
                    }
                    p.sendMessage(prefix + ChatColor.RED  + "Veuillez ecrire 'CONFIRMER' dans le chat pour valider la suppréssion de votre ile");
                    p.sendMessage(prefix + ChatColor.RED + "ATTENTION : cette action supprimera DEFINITIVEMENT votre ile, AUCUNE recuperation possible");
                    p.getPersistentDataContainer().set(NamespacedKey.fromString("is-delete"), PersistentDataType.STRING, "");
                    return true;
                }

                // CMD : /is visit
                case "visit", "visite" -> {
                    String target = args[1];
                    boolean canVisit = (MySQL.getInformationByNameInt(target, "t_island", "allow_visitors") == 1) ? true : false;
                    if(canVisit){
                        teleportPlayerToIsland(p, target);
                        p.sendMessage(prefix + ChatColor.GREEN + "Bienvenue sur l ile " + ChatColor.GOLD + target);
                    }else{
                        p.sendMessage(prefix + ChatColor.RED + "Cette ile n accepte pas les visiteurs !");
                    }
                }

                // CMD : /is setwarp
                case "setwarp" -> {
                    if(args.length == 1) return false;
                    if(MySQL.isPlayerOnHisIsland(p)){
                        try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                            statement.execute(String.format("""
                                    INSERT INTO t_island_warp (name, island_id, warp_x, warp_y, warp_z, yaw, pitch)
                                    VALUES ("%s", %d, %d, %d, %d, %d, %d);
                                    """, args[1], MySQL.getInformationByNameInt(MySQL.getIslandNameByPlayer(p), "t_island", "id"),(int) p.getLocation().getX()
                                    , (int) p.getLocation().getY(), (int) p.getLocation().getZ(), (int) p.getLocation().getYaw(), (int) p.getLocation().getPitch()));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        p.sendMessage(prefix + ChatColor.GREEN + "Le warp " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " a bien ete cree");
                    }else{
                        p.sendMessage(prefix + ChatColor.RED + "Vous devez etre sur votre ile pour pouvoir deposer un is warp !");
                        return false;
                    }
                }

                // CMD : /is warp
                case "warp", "w" -> {
                    if (args.length == 1) {
                        p.openInventory(guis.warpsMenu(0));
                        return false;
                    }
                    String target = "";
                    int id = MySQL.getInformationByNameInt(args[1], "t_island_warp", "island_id");
                    if(id == 0){
                        p.sendMessage(prefix + ChatColor.RED + "Warp inconnue !");
                        return false;
                    }
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        ResultSet result = statement.executeQuery("SELECT * FROM t_island WHERE id=" + id + ";");
                        while(result.next()){
                            target = result.getString("name");
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    boolean canVisit = (MySQL.getInformationByNameInt(target, "t_island", "allow_visitors") == 1) ? true : false;
                    if(!canVisit) return false;
                    int x = MySQL.getInformationByNameInt(args[1], "t_island_warp", "warp_x");
                    int y = MySQL.getInformationByNameInt(args[1], "t_island_warp", "warp_y");
                    int z = MySQL.getInformationByNameInt(args[1], "t_island_warp", "warp_z");
                    int yaw = MySQL.getInformationByNameInt(args[1], "t_island_warp", "yaw");
                    int pitch = MySQL.getInformationByNameInt(args[1], "t_island_warp", "pitch");
                    Location loc = new Location(Bukkit.getWorld("islands"), x, y, z, yaw, pitch);
                    p.teleport(loc);
                    p.sendMessage(prefix + ChatColor.GREEN + "Teleportation reussi !");
                }

                case "test" -> {
                    p.sendMessage(prefix + net.md_5.bungee.api.ChatColor.of("#fdd813") + "ce message est une information");
                    p.sendMessage(prefix + net.md_5.bungee.api.ChatColor.of(new Color(170, 0, 0)) + "ce message est une erreur");
                    p.sendMessage(prefix + net.md_5.bungee.api.ChatColor.of(new Color(51, 255, 73)) + "ce message est une reussite");
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
            case 2 -> 100;
            case 3 -> 150;
            case 4 -> 250;
            case 5 -> 400;
            default -> 50;
        };
        Main.INSTANCE.worldBorderApi.setBorder(p, borderSize, loc);
    }
}