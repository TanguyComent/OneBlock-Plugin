package fr.vyraah.oneblock.managers;

import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.commands.oneblock;
import fr.vyraah.oneblock.tabCompletor.isCompletor;

public class CommandsManager {

    public Main main = Main.INSTANCE;
    public void initCommands() {
        // main.getCommand("namecommand").setExecutor(new CommandExecutor());
        main.getCommand("is").setExecutor(new oneblock());
        main.getCommand("ob").setExecutor(new oneblock());
        main.getCommand("is").setTabCompleter(new isCompletor());
        main.getCommand("ob").setTabCompleter(new isCompletor());
    }
}
