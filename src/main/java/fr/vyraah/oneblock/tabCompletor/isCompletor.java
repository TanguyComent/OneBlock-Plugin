package fr.vyraah.oneblock.tabCompletor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class isCompletor implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command cmd, @NotNull String arg, @NotNull String[] args) {
        ArrayList<String> completions = new ArrayList<>();

        completions.add("wait");

        return completions;
    }
}
