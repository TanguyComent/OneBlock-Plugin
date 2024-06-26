package fr.vyraah.oneblock;

import com.earth2me.essentials.api.Economy;
import com.github.yannicklamprecht.worldborder.api.WorldBorderApi;
import fr.vyraah.oneblock.PlaceHolder.IslandLvl;
import fr.vyraah.oneblock.PlaceHolder.IslandName;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.commands.oneblock;
import fr.vyraah.oneblock.commons.FileC;
import fr.vyraah.oneblock.managers.CommandsManager;
import fr.vyraah.oneblock.managers.EventManager;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public final class Main extends JavaPlugin {

    public static String prefix = net.md_5.bungee.api.ChatColor.of("#fb1010") +"V" + net.md_5.bungee.api.ChatColor.of("#fb3111") + "y" + net.md_5.bungee.api.ChatColor.of("#fc5311") + "r" + net.md_5.bungee.api.ChatColor.of("#fc7412") + "a" + net.md_5.bungee.api.ChatColor.of("#fc9512") + "a" + net.md_5.bungee.api.ChatColor.of("#fdb713") + "h " + net.md_5.bungee.api.ChatColor.of("#fdd813") + "» ";
    public WorldBorderApi worldBorderApi = null;
    public FileConfiguration config = getConfig();
    public static Main INSTANCE;
    private CommandsManager commandManager;
    public static Location spawn;
    public MySQL mysql = new MySQL();
    public static HeadDatabaseAPI head;
    public HashMap<Material, Integer> levelItems = new HashMap<>();
    public HashMap<Integer, Integer> radiusLevel = new HashMap<>();
    public HashMap<Material, Integer> wells = new HashMap<>();
    public ArrayList<Location> scoreboardLocation = new ArrayList<>();
    public static boolean locked = false;

    @Override
    public void onEnable() {
        if(locked) return;
        // world border api implementation

        boolean canStart = true;

        try{
            RegisteredServiceProvider<WorldBorderApi> worldBorderApiRegisteredServiceProvider = getServer().getServicesManager().getRegistration(WorldBorderApi.class);
            worldBorderApi = worldBorderApiRegisteredServiceProvider.getProvider();
        }catch (NoClassDefFoundError e){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin WorldBorderApi a votre server");
            Bukkit.getLogger().info("lien : https://github.com/yannicklamprecht/WorldBorderAPI/releases/tag/1.182.0");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("Multiverse-Core") == null){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin Multiverse-Core a votre server");
            Bukkit.getLogger().info("lien : https://www.spigotmc.org/resources/multiverse-core.390/");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("Essentials") == null){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin Essentials a votre server");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("VoidGen") == null){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin VoidGen a votre server");
            Bukkit.getLogger().info("lien : https://www.spigotmc.org/resources/voidgen.25391/");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("HeadDatabase") == null){
            Bukkit.getLogger().info("Il faut le plugin headdatabase");
            canStart = false;
        }

        if(!canStart) {
            onDisable();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try{
            Bukkit.getWorld("islands").getDifficulty();
        }catch (NullPointerException e){
            getServer().dispatchCommand(getServer().getConsoleSender(), "mv create islands NORMAL -g VoidGen -t FLAT");
        }

        try{
            Bukkit.getWorld("history").getDifficulty();
        }catch (NullPointerException e){
            getServer().dispatchCommand(getServer().getConsoleSender(), "mv create history NORMAL -g VoidGen -t FLAT");
        }

        //--------------------------------------------------------------------------------

        // CHANGE PAR TES PROPRES VALEURS POUR TE CONNECTER A LA DB :)

        config.addDefault("mysql_host", "localhost");
        config.addDefault("mysql_port", 3306); // 3306 <= PORT PRINCPAL DE SQL (MariaDb).
        config.addDefault("mysql_user", "root");
        config.addDefault("mysql_password", "root");
        config.addDefault("mysql_database", "vyraahob");
        config.addDefault("spawn_world", "world");
        config.addDefault("spawn_x", 0);
        config.addDefault("spawn_y", 0);
        config.addDefault("spawn_z", 0);
        config.addDefault("spawn_yaw", 0);
        config.addDefault("spawn_pitch", 0);
        config.addDefault("next_island_x", 0); // utile pour savoir les coos de la prochaine ile
        config.addDefault("next_island_z", 0);
        config.options().copyDefaults(true);
        saveConfig();

        mysql.connect(config.getString("mysql_host"), config.getInt("mysql_port"), config.getString("mysql_database"), config.getString("mysql_user"), config.getString("mysql_password"));

        //--------------------------------------------------------------------------------

        INSTANCE = this;

        //--------------------------------------------------------------------------------

        Bukkit.getLogger().info("===========================");
        Bukkit.getLogger().info("Loading VyraahOneBlock...");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("===========================");

        //--------------------------------------------------------------------------------

        new EventManager(this).registersEvents();

        this.commandManager = new CommandsManager();
        this.commandManager.initCommands();

        //--------------------------------------------------------------------------------

        spawn = new Location(Bukkit.getWorld(config.getString("spawn_world")), config.getLong("spawn_x"), config.getLong("spawn_y"), config.getLong("spawn_z"), config.getLong("spawn_yaw"), config.getLong("spawn_pitch"));

        //--------------------------------------------------------------------------------

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new IslandLvl().register();
            new IslandName().register();
        }

        // HeadDB

        head = new HeadDatabaseAPI();

        // Initialisation of the blocks levels

        FileC.createFile("level-settings");
        try {
            InputStream inputStream = new FileInputStream(FileC.getFile("level-settings"));
            Yaml yaml = new Yaml();
            HashMap<String, Integer> map = yaml.load(inputStream);
            if(map != null)
                map.forEach((key, value) -> levelItems.put(Material.getMaterial(key.toUpperCase()), value));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Initialisation of the wells

        FileC.createFile("wells-settings");
        try{
            InputStream inputStream = new FileInputStream(FileC.getFile("wells-settings"));
            Yaml yaml = new Yaml();
            HashMap<String, Integer> map = yaml.load(inputStream);
            if(map != null)
                map.forEach((key, value) -> wells.put(Material.getMaterial(key.toUpperCase()), value));
        }catch(Exception e){
            throw new RuntimeException(e);
        }

        //mise a jours des sb

        new BukkitRunnable() {
            @Override
            public void run() {
                ArrayList<Location> sbs = MySQL.getScoreboard();
                for(Location sb : sbs){
                    @NotNull Collection<Entity> entities =  sb.getWorld().getNearbyEntities(sb, 0, 10, 0);
                    entities.forEach(Entity::remove);
                    oneblock.setClassementHolo(sb);
                }
                Bukkit.getLogger().info("[Vyraah] : Classement holographic mis a jours !");
            }
        }.runTaskTimer(this, 0, 12000);

        // Configuration du radius

        radiusLevel.put(1, 25);
        radiusLevel.put(2, 50);
        radiusLevel.put(3, 75);
        radiusLevel.put(4, 125);
        radiusLevel.put(5, 200);
    }

    @Override
    public void onDisable(){

        Bukkit.getLogger().info("===========================");
        Bukkit.getLogger().info("Unloading VyraahOneBlock...");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("===========================");

        mysql.disconnect();
    }

    public static String serializedObject(Object object) throws IOException {
        ByteArrayOutputStream io = new ByteArrayOutputStream();
        BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
        os.writeObject(object);
        os.flush();

        byte[] serializedObject = io.toByteArray();

        return Base64.getEncoder().encodeToString(serializedObject);
    }

    public static Object deserializedObject(String serializedObject) throws IOException, ClassNotFoundException {
        byte[] biteSerializedObject = Base64.getDecoder().decode(serializedObject);

        ByteArrayInputStream is = new ByteArrayInputStream(biteSerializedObject);
        BukkitObjectInputStream in = new BukkitObjectInputStream(is);

        return in.readObject();
    }

    public static int getInventorySpace(Player p){
        int space = 0;
        for(int i = 0; i<=35; i++){
            if(p.getInventory().getItem(i) == null) space++;
        }
        return space;
    }

    public static boolean isFull(Player p){
        return getInventorySpace(p) == 0;
    }

}
