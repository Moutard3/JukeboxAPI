package net.mcjukebox.plugin.bukkit.commands;

import lombok.AllArgsConstructor;
import net.mcjukebox.plugin.bukkit.MCJukebox;
import net.mcjukebox.plugin.bukkit.api.JukeboxAPI;
import net.mcjukebox.plugin.bukkit.managers.RegionManager;
import net.mcjukebox.plugin.bukkit.managers.shows.ShowManager;
import net.mcjukebox.plugin.bukkit.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

@AllArgsConstructor
public class RegionCommand extends JukeboxCommand {

    private static final int REGIONS_PER_PAGE = 5;

    private final RegionManager regionManager;

    @Override
    public boolean execute(CommandSender dispatcher, String[] args) {
        // region add <id> <url>
        if (args.length == 3 && args[0].equalsIgnoreCase("add")){
            if(MCJukebox.getInstance().getRegionManager().hasRegion(args[1])) {
                ShowManager showManager = MCJukebox.getInstance().getShowManager();
                HashMap<UUID, String> playersInRegion = MCJukebox.getInstance().getRegionListener().getPlayerInRegion();

                Iterator<UUID> keys = playersInRegion.keySet().iterator();

                while (keys.hasNext()) {
                    UUID uuid = keys.next();
                    String regionID = playersInRegion.get(uuid);

                    if (regionID.equals(args[1])) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null) continue;

                        if (MCJukebox.getInstance().getRegionManager().getRegions().get(args[1]).charAt(0) == '@') {
                            showManager.getShow(MCJukebox.getInstance().getRegionManager().getRegions().get(args[1])).removeMember(player);
                        } else {
                            JukeboxAPI.stopMusic(player);
                        }
                        keys.remove();
                    }
                }
            }

            MCJukebox.getInstance().getRegionManager().addRegion(args[1], args[2]);
            Bukkit.getOnlinePlayers().forEach(player -> {
                MCJukebox.getInstance().getRegionListener().handleMovement(player, player.getLocation().subtract(1, 1, 1), player.getLocation());
            });

            MessageUtils.sendMessage(dispatcher, "region.registered");
            return true;
        }

        // region remove <id>
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")){
            if(MCJukebox.getInstance().getRegionManager().hasRegion(args[1])){
                MCJukebox.getInstance().getRegionManager().removeRegion(args[1]);
                MessageUtils.sendMessage(dispatcher, "region.unregistered");
            }else{
                MessageUtils.sendMessage(dispatcher, "region.notregistered");
            }
            return true;
        }

        // region list
        if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("list")) {
            ArrayList<String> regions = new ArrayList<String>(regionManager.getRegions().keySet());

            int pageCount = (regions.size() - 1) / REGIONS_PER_PAGE + 1;

            int page = 1;
            if (args.length == 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
                    
                if (page > pageCount) {
                    return false;
                }
            }

            dispatcher.sendMessage(ChatColor.GREEN + "Registered Regions (Page " + page + "/" + pageCount + "):");
            dispatcher.sendMessage("");

            for (int i = (page-1) * REGIONS_PER_PAGE; i < page * REGIONS_PER_PAGE && i < regions.size(); i++) {
                String region = regions.get(i);
                dispatcher.sendMessage(ChatColor.GOLD + "Name: " + ChatColor.WHITE + region);
                dispatcher.sendMessage(ChatColor.GOLD + "URL/Show: " + ChatColor.WHITE + regionManager.getRegions().get(region));

                if (i != regions.size() - 1) {
                    dispatcher.sendMessage("");
                }
            }

            if (page < pageCount) {
                dispatcher.sendMessage(ChatColor.GRAY + "Type '/jukebox region list " + (page + 1) + "' to see more...");
            }

            return true;
        }

        return false;
    }

}
