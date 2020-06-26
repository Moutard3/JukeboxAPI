package net.mcjukebox.plugin.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandUtils {
    public static List<Player> selectorToPlayer(CommandSender sender, String selector) {
        List<Player> found = new ArrayList<>();

        try {
            List<Entity> selected = Bukkit.selectEntities(sender, selector);

            if (selected.size() > 0) {
                Entity entity = selected.iterator().next();
                if (entity instanceof Player) {
                    found.add((Player) entity);
                }
            }
        } catch (IllegalArgumentException ignored) {}

        return found;
    }

}
