package PlaceHolder;

import fr.vyraah.oneblock.SQL.MySQL;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IslandName extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "islandName";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kitsoko";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if(player == null) return "";
        if(!MySQL.getPlayerHaveAnIsland(player.getPlayer())) return "no island";
        return MySQL.getIslandNameByPlayer(player.getPlayer().getName());
    }
}
