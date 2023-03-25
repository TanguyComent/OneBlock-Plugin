package fr.vyraah.oneblock.commons;

import fr.vyraah.oneblock.Main;

import java.io.File;
import java.io.IOException;

public class FileC {


    public static void createFile(String filename){
        Main main = Main.INSTANCE;

        if(!main.getDataFolder().exists()){
            main.getDataFolder().mkdir();
        }

        File file = new File(main.getDataFolder(), filename + ".yml");

        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static File getFile(String filename){

        Main main = Main.INSTANCE;
        return new File(main.getDataFolder(), filename + ".yml");
    }
}
