package fr.vyraah.oneblock.commands;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.cinématics.Cinematics;
import fr.vyraah.oneblock.guis.guis;
import net.ess3.api.MaxMoneyException;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class oneblock implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String arg, @NotNull String[] args) {

        // ATTENTION: NOUS VOULONS JUSTE UN /IS QUI VERIFIE SI LE JOUEUR EST DEJA DANS UNE ILE
        // SI OUI, LE PLUGIN LUI OUVRE LE PANEL
        // SINON
        // IL FAITL A CREATION DE L'ILE AVEC LA CREATION DES VALEURS DANS LA DB :)

        if(sender instanceof Player p) {
            if(MySQL.getPlayerHaveAnIsland(p))
                MySQL.updateQuest(MySQL.getIslandNameByPlayer(p.getName()));

            if(args.length == 0){
                if(MySQL.getPlayerHaveAnIsland(p)){
                    //ouvrir le pannel
                    p.openInventory(guis.islandInformations(p));
                    return true;
                }else{
                    p.sendMessage(Main.prefix + ChatColor.RED + "Tu n'as pas d'ile crée en une grace à la commande /is create <nom>");
                    return true;
                }
            }
            if(!MySQL.getPlayerHaveAnIsland(p) && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("invite") && !args[0].equalsIgnoreCase("join")){
                p.sendMessage(Main.prefix + "§4Vous devez avoir une ile pour effectuer cette action !\n" + Main.prefix + "§4Créez une ile grace a la commande /is create <nom>");
                return false;
            }
            switch(args[0].toLowerCase()){

                //CMD : /is help
                case "help" -> {
                    p.sendMessage(String.format("""
                            %s§eListe des commandes d'iles :
                            \n
                            %s§b/ob create <nom> -> §7Permet de crée son ile
                            %s§b/ob go -> §7Permet de se téléporter à son ile
                            %s§b/ob invite <joueur> -> §7Permet d'inviter un joueur sur son ile
                            %s§b/ob invite list -> §7Permet d'afficher les invitations d'ile en attente que vous avez
                            %s§b/ob kick <joueur> -> §7Permet de kick un membre de son ile
                            %s§b/ob join <joueur> OU <ile> -> §7Permet d'accepter une invitation à rejoindre une ile
                            %s§b/ob leave -> §7Permet de quitter son ile (impossible si vous êtes fondateur de l'ile)
                            %s§b/ob disband -> §7Permet de supprimer son ile (réservé aux fondateurs d'ile)
                            %s§b/ob visite <ile> -> §7Permet de visiter une ile si l'ile accepte les visites
                            %s§b/ob setwarp -> §7Permet de crée un warp d'ile (point de tp accessible à tous) (BETA)
                            %s§b/ob warp <nom du warp> -> §7Permet de se téleporter à un warp d'ile précis (BETA)
                            %s§b/ob warp -> §7ouvre le menu des warps (NON FONCTIONNEL (enfin à moitié))
                            """, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix, Main.prefix));
                    return true;
                }

                // CMD : /is create || /is cr
                case "create", "cr" -> {
                    //mauvaise utilisation de la commande
                    if (args.length == 1 || args.length > 2) {
                        p.sendMessage(Main.prefix + ChatColor.RED + "/is create <nom> pour créer une ile");
                        return false;
                    }

                    String islandName = args[1];

                    if(islandName.equalsIgnoreCase("error")){
                        p.sendMessage(Main.prefix + "Ce nom est utilisé par le programme à des fin de détection d'erreur ! Il est par conséquent interdit !\nVous pouvez cependant remplacer des lettres par des chiffres ou rajouter un charactère à la fin !");
                        return false;
                    }

                    //creation des data

                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(Main.prefix + ChatColor.RED + "tu as deja une ile");
                        return false;
                    }

                    int x = Main.INSTANCE.config.getInt("next_island_x");
                    int z = Main.INSTANCE.config.getInt("next_island_z");

                    try{
                       MySQL.createIsland(p, islandName, x, 0, z);
                    }catch (SQLIntegrityConstraintViolationException e){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Cette ile existe deja, veuiller choisir un autre nom !");
                    }catch(Exception e){
                        p.sendMessage(Main.prefix + ChatColor.RED + "ERREUR INCONNUE : votre ile n'a pas pu etre cree veuillez reessayer");
                        e.printStackTrace();
                        return false;
                    }

                    //island creation

                    Location loc = new Location(Bukkit.getWorld("islands"), x, 0, z);
                    loc.getBlock().setType(Material.OAK_WOOD);

                    if( x == z ){ x += 3000; } else { z += 3000; }
                    Main.INSTANCE.config.set("next_island_x", x);
                    Main.INSTANCE.config.set("next_island_z", z);
                    Main.INSTANCE.saveConfig();
                    p.sendMessage(Main.prefix + ChatColor.GREEN + "Ton ile a bien ete cree fait /is go pour y etre teleporte !");
                    return true;
                }

                // CMD : /is go
                case "go", "teleport" -> {
                    teleportPlayerToIsland(p, MySQL.getIslandNameByPlayer(p.getName()));
                    return true;
                }

                // CMD : /is invite
                case "invite" -> {
                    if(args.length == 1){
                        p.sendMessage(Main.prefix + "§4Mauvaise utilisation /is invite <joueur>");
                        return false;
                    }

                    if(!MySQL.getPlayerHaveAnIsland(p)){
                        if(args.length == 2){
                            if(args[1].equalsIgnoreCase("list")){
                                //affiche la liste des invitations du joueur

                                ArrayList<String> invitations = MySQL.getIslandInvite(p);
                                p.sendMessage(Main.prefix + ChatColor.BLUE + "Liste de tes invitations :");
                                if(invitations.size() == 0) p.sendMessage(Main.prefix + ChatColor.GREEN + "Vous n'avez aucune invitation");
                                for(String msg : invitations){
                                    p.sendMessage(msg);
                                }
                                return true;
                            }
                        }
                        p.sendMessage(Main.prefix + ChatColor.RED + "Il te faut une ile pour pouvoir inviter des joueurs ! Commence par faire /is create <nom>");
                        return false;
                    }

                    String playerIsland = MySQL.getIslandNameByPlayer(p.getName());
                    int islandPrestigeLvl = MySQL.getIslandPrestigeByPlayer(p),
                        islandMemberCount = MySQL.howManyPlayersHasIsland(playerIsland);

                    if(islandPrestigeLvl == 1){
                        if(islandMemberCount >= 5){
                            p.sendMessage(Main.prefix + "§4Votre ile est pleine, vous devez augmenter votre niveau de prestige pour augmenter le nombre de joueur qu'une ile peut accueillir");
                            return false;
                        }
                    }else if(islandPrestigeLvl == 2){
                        if(islandMemberCount >= 6){
                            p.sendMessage(Main.prefix + "§4Votre ile est pleine, vous devez augmenter votre niveau de prestige pour augmenter le nombre de joueur qu'une ile peut accueillir");
                            return false;
                        }
                    }else if(islandPrestigeLvl == 3){
                        if(islandMemberCount >= 7){
                            p.sendMessage(Main.prefix + "§4Votre ile est pleine, vous devez augmenter votre niveau de prestige pour augmenter le nombre de joueur qu'une ile peut accueillir");
                            return false;
                        }
                    }else if(islandPrestigeLvl == 4){
                        if(islandMemberCount >= 8){
                            p.sendMessage(Main.prefix + "§4Votre ile est pleine, vous devez augmenter votre niveau de prestige pour augmenter le nombre de joueur qu'une ile peut accueillir");
                            return false;
                        }
                    }else if(islandPrestigeLvl == 5){
                        if(islandMemberCount >= 9){
                            p.sendMessage(Main.prefix + "§4Votre ile est pleine.");
                            return false;
                        }
                    }

                    //check if player has island permission to invite player into

                    String targetName = args[1];
                    Player target = Bukkit.getPlayer(targetName);

                    //return en cas d'auto invitation (s'inviter sois même)
                    if(p.getName().equals(targetName)){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Tu ne peut pas t auto inviter");
                        return false;
                    }

                    //on essaye d'inviter le joueur dans l'ile, en passant ça verifie si le joueur existe et est co
                    try{
                        //on verifie que la cible n'ai pas deja une ile
                        if(MySQL.getPlayerHaveAnIsland(target)){
                            p.sendMessage(Main.prefix + ChatColor.RED + "Le joueur " + targetName + " a deja une ile !");
                            return false;
                        }
                        TextComponent accept = new TextComponent(ChatColor.GREEN + "[accepter]    ");
                        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§2Click ici pour accepter").create()));
                        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/is join " + p.getName()));
                        TextComponent deny = new TextComponent(ChatColor.RED + "    [refuser]");
                        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§4Click ici pour refuser").create()));
                        target.sendMessage(Main.prefix + ChatColor.GREEN + "Le joueur "+ ChatColor.GOLD + p.getName() + ChatColor.GREEN + " t invite a rejoindre son ile !");
                        target.spigot().sendMessage(accept,deny);
                    }catch (NullPointerException e){
                        //ici, le joueur n'existe pas
                        p.sendMessage(Main.prefix + ChatColor.RED + "Le joueur " + targetName + " n existe pas ou est deconnecte");
                        return false;
                    }

                    //enregistrement de l'invitation dans la bdd
                    try{
                        MySQL.invitePlayerToIsland(playerIsland, target);
                        p.sendMessage(Main.prefix + ChatColor.GREEN + "Le joueur " + target.getName() + " a bien été inviter sur votre ile !");
                    }catch(RuntimeException e) {
                        if(e.getMessage().equals("AlreadyInvited")){
                            p.sendMessage(Main.prefix + ChatColor.RED + " " + targetName + " a deja été invité a rejoindre ton ile");
                        }else{
                            p.sendMessage(Main.prefix + ChatColor.RED + "Une erreur inconnue est survenue");
                            e.printStackTrace();
                        }
                        return false;
                    }
                }

                // CMD : /is kick
                case "kick" -> {
                    if(args.length == 1){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Vous ne pouvez pas kick de l'air ! /is kick <joueur>");
                        return false;
                    }
                    String target = args[1];
                    if(MySQL.inSameIsland(p.getName(), target)){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Ce joueur n est pas dans votre ile !");
                        return false;
                    }

                    boolean authorised = false;

                    //regarder si le joueur a la perm de kick

                    if(!authorised){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Vous ne pouvez pas kick ce joueur de votre ile !");
                        return false;
                    }

                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("DELETE FROM t_user WHERE name=\"" + target + "\";");
                        p.sendMessage(Main.prefix + ChatColor.GREEN + " Le joueur " + ChatColor.GOLD + target + ChatColor.GREEN + " a bien ete kick de votre ile !");
                    }catch(Exception e){return false;}
                }

                // CMD : /is join
                case "join" -> {
                    //on regarde si le joueur a pas deja une ile (normalement non mais on sais jamais)
                    if(MySQL.getPlayerHaveAnIsland(p)){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Tu as deja une ile !");
                        return false;
                    }

                    if(args.length == 1){
                        p.sendMessage(Main.prefix + "§4Mauvaise utilisation /is join <joueur>");
                        return false;
                    }

                    String target = args[1];
                    boolean isTargetPlayer = true;
                    int islandId;

                    try{
                        Player t = Bukkit.getPlayer(target);
                    }catch (Exception e){
                        isTargetPlayer = false;
                    }

                    if(isTargetPlayer){
                        islandId = MySQL.getIslandIdByPlayer(target);
                    }else{
                        islandId = MySQL.getIslandIdByIslandName(target);
                    }

                    if(!MySQL.hasInvitation(p.getUniqueId(), islandId)){
                        p.sendMessage(Main.prefix + "§4Tu n'as pas d'invitation pour rejoindre cette ile !");
                        return false;
                    }

                    MySQL.addPlayerToIsland(p, islandId, 4);
                }

                // CMD : /is phase

                case "phase", "phases" -> {
                    p.openInventory(guis.obPhases(p));
                }

                // CMD : /is leave
                case "leave" -> {
                    if(MySQL.getPlayerGrade(p.getName()) == 1){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Vous êtes le chef de votre ile ! Vous devez donc faire la commande /is disband pour supprimer votre ile");
                        return false;
                    }

                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("DELETE FROM t_user WHERE name=\"" + p.getName() + "\";");
                        p.sendMessage(Main.prefix + ChatColor.GREEN + "Vous avez bien quitté votre ile !");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                // CMD : /is disband
                case "disband" -> {
                    if(MySQL.getPlayerGrade(p.getName()) != 1){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Vous n'avez pas la permission !");
                        return false;
                    }
                    p.sendMessage(Main.prefix + ChatColor.RED  + "Veuillez ecrire 'CONFIRMER' dans le chat pour valider la suppréssion de votre ile");
                    p.sendMessage(Main.prefix + ChatColor.RED + "ATTENTION : cette action supprimera DEFINITIVEMENT votre ile, AUCUNE recuperation possible");
                    p.sendMessage(Main.prefix + ChatColor.RED + "Ecrire n'importe quoi d'autre dans le chat annulera la suppression");
                    p.getPersistentDataContainer().set(NamespacedKey.fromString("is-delete"), PersistentDataType.STRING, "");
                    return true;
                }

                // CMD : /is visit
                case "visit", "visite" -> {
                    if(args.length == 1){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Tu dois spécifier l'ile que tu veut visiter !");
                        return false;
                    }
                    String target = args[1];
                    boolean canVisit = false; //Récuperer le setting invite
                    if(canVisit){
                        teleportPlayerToIsland(p, target);
                        p.sendMessage(Main.prefix + ChatColor.GREEN + "Bienvenue sur l'ile " + ChatColor.GOLD + target);
                    }else{
                        p.sendMessage(Main.prefix + ChatColor.RED + "Cette ile n'accepte pas les visiteurs ou est inexistante !");
                    }
                }

                // CMD : /is setwarp
                case "setwarp" -> {
                    if(args.length == 1) return false;
                }

                // CMD : /is warp
                case "warp", "w" -> {
                    if (args.length == 1) {
                        p.openInventory(guis.warpsMenu(0));
                        return false;
                    }
                    String target = "";
                    int id = 0;
                    if(id == 0){
                        p.sendMessage(Main.prefix + ChatColor.RED + "Warp inconnue !");
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
                    boolean canVisit = false; // recuperer dans les settings la perm visit
                    if(!canVisit) return false;
                    Location loc = new Location(Bukkit.getWorld("islands"), 0, 0, 0, 0, 0); // Recuperer la Loccation du warp
                    p.teleport(loc);
                    p.sendMessage(Main.prefix + ChatColor.GREEN + "Teleportation reussi !");
                 }

                //cmd /is promote && /is remote
                case "promote", "remote" -> {
                    //a refaire
                }

                //cmd /is ban
                case "ban" -> {

                }

                //cmd /is calculate
                case "calculate", "calc" -> {
                    Location middle = MySQL.getCenterLocationByIslandName(MySQL.getIslandNameByPlayer(p.getName()));
                    int radius = Main.INSTANCE.radiusLevel.get(MySQL.getIslandPrestigeByPlayer(p));
                    AtomicInteger newLevel = new AtomicInteger();
                    p.sendMessage(Main.prefix + "§2Début du calcul du niveau d'ile...");
                    for(int y = -60; y < 320; y++){
                        for(int x = (int) middle.getX()-radius; x < middle.getX()+radius; x++){
                            for(int z = (int) middle.getZ()-radius; z < middle.getZ()+radius; z++){
                                Location toCheck = new Location(Bukkit.getWorld("islands"), x, y, z);
                                if(Main.INSTANCE.levelItems.containsKey(toCheck.getBlock().getType())){
                                    newLevel.addAndGet(Main.INSTANCE.levelItems.get(toCheck.getBlock().getType()));
                                }
                            }
                        }
                    }
                    ArrayList<ArrayList<Object>> wells = MySQL.getWellsInformation(MySQL.getIslandIdByPlayer(p.getName()));
                    wells.forEach((wls) -> {
                        Material mat = Material.getMaterial((String) wls.get(0));
                        int amount = (int) wls.get(1) - 1;
                        int level = Main.INSTANCE.levelItems.get(mat);
                        newLevel.addAndGet(amount * level);
                    });
                    p.sendMessage(Main.prefix + "§2Calcul terminée ! Nouveau niveau : §6" + newLevel);
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("UPDATE t_island SET level=" + newLevel + " WHERE name='" + MySQL.getIslandNameByPlayer(p.getName()) + "';");
                    }catch(Exception e){throw new RuntimeException(e);}
                }

                case "prestige" -> {
                    p.openInventory(guis.obPrestige(p));
                }

                case "createshop" -> {
                    if(Main.isFull(p)){
                        p.sendMessage(Main.prefix + "Votre inventaire est plein ! Veuillez y laisser au moin 1 place et réessayer");
                        return false;
                    }
                    p.openInventory(guis.sellOrBuyChoice());
                }

                case "bank" -> {
                    int bankSold = MySQL.getObBankMoney(MySQL.getIslandNameByPlayer(p.getName()));
                    if(args.length == 1){
                        p.sendMessage(Main.prefix + "§2Vous avez §e" + bankSold + "$ §2dans votre bank d'ile");
                        return true;
                    }
                    if(args.length > 3){
                        p.sendMessage(Main.prefix + "§4Commande inconnue");
                        return false;
                    }
                    if(args[1].equalsIgnoreCase("deposit") || args[1].equalsIgnoreCase("withdraw")){
                        if(args.length == 2) {
                            p.sendMessage(Main.prefix + "§4Mauvaise utilisation : /is bank " + args[1] + " <nombre entier>");
                            return false;
                        }
                        long value;
                        try{
                            value = Integer.valueOf(args[2]);
                        }catch(NumberFormatException e){
                            p.sendMessage(Main.prefix + "§4Mauvaise utilisation : /is bank " + args[1] + " <nombre entier>");
                            return false;
                        }

                        if(args[1].equalsIgnoreCase("deposit")){
                            try {
                                if(Economy.hasEnough(p.getName(), value)){
                                    Economy.substract(p.getName(), BigDecimal.valueOf(value));
                                    MySQL.alterBankSold(MySQL.getIslandNameByPlayer(p.getName()), value, 1);
                                    p.sendMessage(Main.prefix + "§2Vous avez bien déposer §e" + value + "$ §2dans votre bank d'ile\n"
                                            + Main.prefix + "§2Nouveau solde de la bank : §e" + MySQL.getObBankMoney(MySQL.getIslandNameByPlayer(p.getName())) + "$");
                                }else{
                                    p.sendMessage(Main.prefix + "§4Vous n'avez pas asser d'argent pour effectuer cela !");
                                }
                            } catch (UserDoesNotExistException | MaxMoneyException | NoLoanPermittedException e) {
                                throw new RuntimeException(e);
                            }
                        }else if(args[1].equalsIgnoreCase("withdraw")) {
                            if(MySQL.alterBankSold(MySQL.getIslandNameByPlayer(p.getName()), value, -1)){
                                try {
                                    Economy.add(p.getName(), BigDecimal.valueOf(value));
                                } catch (UserDoesNotExistException | NoLoanPermittedException | MaxMoneyException e) {
                                    throw new RuntimeException(e);
                                }
                                p.sendMessage(Main.prefix + "§2Vous avez bien retiré §e" + value + "$ §2de votre bank");
                            }else{
                                p.sendMessage(Main.prefix + "§4Vous n'avez pas asser d'argent dans votre bank pour effectuer cela !");
                            }
                        }
                    }else{
                        p.sendMessage(Main.prefix + "§4Commande inconnue");
                    }
                }

                case "quest" -> {
                    p.openInventory(guis.dailyQuestInv(p));
                }

                default -> p.sendMessage(Main.prefix + "§4Commande inconnue");

                //PARTIE MODERATEUR

                //cmd /is setholotop

                case "setholotop" -> {
                    if(!p.isOp()) return false;
                    Location location = setClassementHolo(p.getLocation());
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute("INSERT INTO t_holo (x, y, z, type, world) VALUES (" + location.getX() + ", " + p.getLocation().getY() + ", " + location.getZ() +" , \"sb\", \"" + location.getWorld().getName() + "\");");
                    }catch(Exception e){throw new RuntimeException(e);}
                    p.sendMessage(Main.prefix + "§2Classement mis !");
                }

                case "sqlorder" -> {
                    if(!p.isOp()) return false;
                    int i = 1;
                    String order = "";
                    while(i < args.length) {
                        order += args[i] + " ";
                        i++;
                    }
                    p.sendMessage("§2Order : §e" + order + "§2has been queried");
                    try(Statement statement = Main.INSTANCE.mysql.getConnection().createStatement()){
                        statement.execute(order);
                    }catch(Exception e){
                        p.sendMessage("§4[SQL ERROR] : " + e.getMessage());
                    }
                }

                //cmd de test pour afficher rapidement des valeurs ou faire en vitesse des tests
                case "test" -> {
                    if(!p.isOp()) return false;
                    Cinematics.createIslandNarration(p);
                }
            }
        }
        return false;
    }

    public static void teleportPlayerToIsland(Player p, String islandName){
        Location loc = MySQL.getSpawnLocationByIslandName(islandName); // recuperer la loc de spawn de l'ile
        p.teleport(loc.add(.5, 2, .5));
        int borderSize = Main.INSTANCE.radiusLevel.get(MySQL.getIslandPrestigeByIslandName(islandName))*2;
        Main.INSTANCE.worldBorderApi.setBorder(p, borderSize, loc);
    }

    public static Location setClassementHolo(Location location){
        location = location.getBlock().getLocation();
        ArmorStand holo = (ArmorStand) location.getWorld().spawnEntity(location.add(.5, 1.3, .5), EntityType.ARMOR_STAND);
        holo.setGravity(false);
        holo.setCanPickupItems(false);
        holo.setCustomName("§6Les §ameilleurs iles §6du server !");
        holo.setCustomNameVisible(true);
        holo.setVisible(false);
        ArrayList<ArrayList<Object>> isTop = MySQL.getIslandTop(10);
        for(int i = 0; i <= 9; i++){
            ArmorStand top = (ArmorStand) location.getWorld().spawnEntity(location.add(0, -.3, 0), EntityType.ARMOR_STAND);
            top.setGravity(false);
            top.setCanPickupItems(false);
            try {
                top.setCustomName("§6" + (i + 1) + " §a: " + isTop.get(i).get(0) + " §6niveau §a" + isTop.get(i).get(1));
            }catch(IndexOutOfBoundsException e){
                top.setCustomName("§6" + (i + 1) + " §a: Null");
            }
            top.setCustomNameVisible(true);
            top.setVisible(false);
        }
        return location;
    }
}