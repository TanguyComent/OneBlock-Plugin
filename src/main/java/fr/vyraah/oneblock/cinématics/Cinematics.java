package fr.vyraah.oneblock.cinématics;

import com.mojang.authlib.GameProfile;
import fr.vyraah.oneblock.Main;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Cinematics {

    public static void createIslandNarration(Player p){
        Location from = new Location(Bukkit.getWorld("world"), -129.5, 65, -140.5, 0, 90);
        Location to = new Location(Bukkit.getWorld("world"), -120.5, 65, -140.5);
        int duration = 10;
        p.teleport(from);
        CraftPlayer craftPlayer = (CraftPlayer) p;
        ServerPlayer entityPlayer = craftPlayer.getHandle();
        ServerGamePacketListenerImpl con = entityPlayer.connection;
        final double[] percs = {0, 0, 0};
        for(int i = 0; i <= duration * 20; i++){
            double finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double percent = finalI / (duration * 20) * 100;
                    if(percent == 0){
                        p.sendMessage("§6[Vous] : §eAie ma tête...");
                    }else if(percent == 10){
                        p.sendMessage("§6[Vous] : §eOu je suis ? ou sont mes amis...");
                    }else if(percent == 20){
                        p.sendMessage("§6[Vous] : §eNous prenions simplement l'avion pour aller en vacances...");
                    }else if(percent == 22){
                        p.sendMessage("§6[Vous] : §eQue s'est il passé...");
                    }else if(percent == 24){
                        p.sendMessage("§6[Vous] : §eJe suis le dernier... ?");
                    }
                    if(percent == 100){
                        MinecraftServer server = entityPlayer.server;
                        ServerLevel world = entityPlayer.getLevel();
                        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "Capitaine");
                        ServerPlayer npc = new ServerPlayer(server, world, gameProfile);
                        npc.setPos(-117.5, 65, -140.5);
                        ArmorStand as = (ArmorStand) p.getWorld().spawnEntity(new Location(p.getWorld(), -117.5, 65, -140.5), EntityType.ARMOR_STAND);
                        for(Player toHide : Bukkit.getOnlinePlayers()){
                            if(toHide != p) toHide.hideEntity(Main.INSTANCE, as);
                        }
                        con.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc));
                        con.send(new ClientboundAddPlayerPacket(npc));
                    }
                    //movement
                    Vector v =createVector(from, to, duration);
                    p.setVelocity(v);
                    //head movements
                    if(percent >= 0 && percent <= 50){
                        double x = p.getLocation().getX() + v.getX() * (finalI + 10);
                        double z = p.getLocation().getZ() + v.getZ() * (finalI + 10);
                        double fromY = p.getLocation().getY() - 1;
                        double finalY = fromY + 2.5 * ((finalI / (duration * 20) * 100) * (100.0 / 50)) * 0.01;
                        ClientboundPlayerLookAtPacket packet = new ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor.EYES, x, finalY, z);
                        con.send(packet);
                    }
                }
            }.runTaskLater(Main.INSTANCE, i);
        }
    }

    private static Vector createVector(Location from, Location to, int duration){
        double x = ((from.getX() > to.getX()) ? from.getX() - to.getX() : to.getX() - from.getX()) / (duration * 20);
        double y = ((from.getY() > to.getY()) ? from.getY() - to.getY() : to.getY() - from.getY()) / (duration * 20);
        double z = ((from.getZ() > to.getZ()) ? from.getZ() - to.getZ() : to.getZ() - from.getZ()) / (duration * 20);
        int xS = (from.getX() > to.getX()) ? -1 : 1;
        int yS = (from.getY() > to.getY()) ? -1 : 1;
        int zS = (from.getZ() > to.getZ()) ? -1 : 1;
        return new Vector(x * xS, y * yS, z * zS);
    }
}