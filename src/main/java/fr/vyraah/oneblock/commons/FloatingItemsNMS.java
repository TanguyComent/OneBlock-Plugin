package fr.vyraah.oneblock.commons;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FloatingItemsNMS {

    private final Player p;
    private final Location loc;
    private final ItemStack it;
    private ArmorStand as;
    private ItemEntity ie;
    private ServerGamePacketListenerImpl con;

    public FloatingItemsNMS(Player p, Location loc, ItemStack it){
        this.p = p;
        this.loc = loc;
        this.it = it;
    }

    public void setItem(){
        ServerLevel sl = ((CraftWorld) this.loc.getWorld()).getHandle();
        this.as = new ArmorStand(sl, this.loc.getX(), this.loc.getY(), this.loc.getZ());
        ServerPlayer sp = ((CraftPlayer) this.p).getHandle();
        this.con = sp.connection;

        this.as.setMarker(true);
        this.as.setInvisible(true);

        this.ie = new ItemEntity(sl, this.loc.getX(), this.loc.getY(), this.loc.getZ(), CraftItemStack.asNMSCopy(this.it));

        this.ie.setDeltaMovement(0, 0, 0);
        this.ie.setInvulnerable(true);
        this.ie.startRiding(this.as);

        con.send(new ClientboundAddEntityPacket(this.as));
        con.send(new ClientboundSetEntityDataPacket(this.as.getId(), this.as.getEntityData(), true));

        con.send(new ClientboundAddEntityPacket(this.ie));
        con.send(new ClientboundSetEntityDataPacket(this.ie.getId(), this.ie.getEntityData(), true));
        con.send(new ClientboundSetEntityMotionPacket(this.ie));

        con.send(new ClientboundSetPassengersPacket(this.as));
    }

    public void setItem(int asId, int itId){
        ServerLevel sl = ((CraftWorld) this.loc.getWorld()).getHandle();
        this.as = new ArmorStand(sl, this.loc.getX(), this.loc.getY(), this.loc.getZ());
        ServerPlayer sp = ((CraftPlayer) this.p).getHandle();
        this.con = sp.connection;

        this.as.setMarker(true);
        this.as.setInvisible(true);
        this.as.setId(asId);

        this.ie = new ItemEntity(sl, this.loc.getX(), this.loc.getY(), this.loc.getZ(), CraftItemStack.asNMSCopy(this.it));

        this.ie.setDeltaMovement(0, 0, 0);
        this.ie.setInvulnerable(true);
        this.ie.startRiding(this.as);
        this.ie.setId(itId);

        con.send(new ClientboundAddEntityPacket(this.as));
        con.send(new ClientboundSetEntityDataPacket(this.as.getId(), this.as.getEntityData(), true));

        con.send(new ClientboundAddEntityPacket(this.ie));
        con.send(new ClientboundSetEntityDataPacket(this.ie.getId(), this.ie.getEntityData(), true));
        con.send(new ClientboundSetEntityMotionPacket(this.ie));

        con.send(new ClientboundSetPassengersPacket(this.as));
    }

    public void removeItem(){
        this.con.send(new ClientboundRemoveEntitiesPacket(this.ie.getId(), this.as.getId()));
    }

    public int getAsId(){
        return this.as.getId();
    }

    public int getItemId(){
        return this.ie.getId();
    }
}
