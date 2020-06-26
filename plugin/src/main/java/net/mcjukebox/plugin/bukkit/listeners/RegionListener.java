package net.mcjukebox.plugin.bukkit.listeners;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.mcjukebox.plugin.bukkit.MCJukebox;
import net.mcjukebox.plugin.bukkit.api.JukeboxAPI;
import net.mcjukebox.plugin.bukkit.api.ResourceType;
import net.mcjukebox.plugin.bukkit.api.models.Media;
import net.mcjukebox.plugin.bukkit.managers.RegionManager;
import net.mcjukebox.plugin.bukkit.managers.shows.Show;
import net.mcjukebox.plugin.bukkit.managers.shows.ShowManager;
import org.bukkit.Location;
import net.mcjukebox.plugin.bukkit.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;

import java.util.*;

public class RegionListener implements Listener {

    @AllArgsConstructor
    @Data
    public static class Region {

        private String id;
        private int priority;

    }

    private final RegionManager utils;
    @Getter private final HashMap<UUID, String> playerInRegion = new HashMap<>();

    public RegionListener(RegionManager utils) {
        this.utils = utils;
    }

    private boolean URL(final CommandSender sender) {
        MessageUtils.sendMessage(sender, "user.openLoading");
        Bukkit.getScheduler().runTaskAsynchronously(MCJukebox.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!(sender instanceof Player)) return;
                String token = JukeboxAPI.getToken((Player) sender);
                MessageUtils.sendURL((Player) sender, token);
            }
        });
        return true;
    }

    public List<Region> getApplicableRegions(Location location) {
        ArrayList<Region> regionList = new ArrayList<>();

        WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();

        World world = platform.getMatcher().getWorldByName(Objects.requireNonNull(location.getWorld()).getName());
        com.sk89q.worldguard.protection.managers.RegionManager regionManager = platform.getRegionContainer().get(world);
        org.bukkit.util.Vector bukkitVector = location.toVector();
        BlockVector3 vector = BlockVector3.at(bukkitVector.getX(), bukkitVector.getY(), bukkitVector.getZ());
        Set<ProtectedRegion> regions = Objects.requireNonNull(regionManager).getApplicableRegions(vector).getRegions();

        for (ProtectedRegion region : regions) {
            regionList.add(new Region(region.getId(), region.getPriority()));
        }

        return regionList;
    }

    public void handleMovement(Player player, Location from, Location to) {
        //Only execute if the player moves an entire block
        if (from != null
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        int highestPriority = -1;
        String highestRegion = null;
        for (Region region : getApplicableRegions(to)) {
            if (region.getPriority() > highestPriority && utils.hasRegion(region.getId())) {
                highestPriority = region.getPriority();
                highestRegion = region.getId();
            }
        }

        ShowManager showManager = MCJukebox.getInstance().getShowManager();

        if (highestRegion == null && utils.hasRegion("__global__")) {
            highestRegion = "__global__";
        }

        //In this case, there are no applicable shared so we need go no further
        if (highestRegion == null) {
            if (playerInRegion.containsKey(player.getUniqueId())) {
                String lastShow = utils.getURL(playerInRegion.get(player.getUniqueId()));
                playerInRegion.remove(player.getUniqueId());

                if (lastShow == null || lastShow.toCharArray()[0] != '@') {
                    //Region no longer exists, stop the music.
                    JukeboxAPI.stopMusic(player);
                } else {
                    showManager.getShow(lastShow).removeMember(player);
                }
                return;
            }
            return;
        }

        if (playerInRegion.containsKey(player.getUniqueId()) &&
                playerInRegion.get(player.getUniqueId()).equals(highestRegion) &&
                utils.getURL(playerInRegion.get(player.getUniqueId())).equals(utils.getURL(highestRegion))) {
            return;
        }

        if (playerInRegion.containsKey(player.getUniqueId()) &&
                utils.getURL(playerInRegion.get(player.getUniqueId())).equals(utils.getURL(highestRegion))) {
            // No need to restart the track, or re-add them to a show, but still update our records
            playerInRegion.put(player.getUniqueId(), highestRegion);
            return;
        }

        if (playerInRegion.containsKey(player.getUniqueId())) {
            String lastShow = utils.getURL(playerInRegion.get(player.getUniqueId()));
            if (lastShow.toCharArray()[0] == '@') {
                showManager.getShow(lastShow).removeMember(player);
            }
        }

        if (utils.getURL(highestRegion).toCharArray()[0] == '@') {
            if (playerInRegion.containsKey(player.getUniqueId())) {
                JukeboxAPI.stopMusic(player);
            }
            showManager.getShow(utils.getURL(highestRegion)).addMember(player, true);
            playerInRegion.put(player.getUniqueId(), highestRegion);
            return;
        }

        Media media = new Media(ResourceType.MUSIC, utils.getURL(highestRegion));
        JukeboxAPI.play(player, media);
        playerInRegion.put(player.getUniqueId(), highestRegion);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        URL(event.getPlayer());
        //The from location has to be offset else the event will not be run
        handleMovement(event.getPlayer(), null, event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }
        handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinecartMove(VehicleMoveEvent event) {
        if (event.getVehicle().getPassengers().size() == 0 || !(event.getVehicle().getPassengers().get(0) instanceof Player)) {
            return;
        }
        handleMovement((Player) event.getVehicle().getPassengers().get(0), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        if (playerInRegion.containsKey(event.getPlayer().getUniqueId())) {
            String lastAudio = utils.getURL(playerInRegion.get(event.getPlayer().getUniqueId()));

            if (lastAudio == null || lastAudio.toCharArray()[0] != '@') {
                JukeboxAPI.stopMusic(event.getPlayer());
            }

            playerInRegion.remove(event.getPlayer().getUniqueId());
        }

        ShowManager showManager = MCJukebox.getInstance().getShowManager();
        if (showManager.inInShow(event.getPlayer().getUniqueId())) {
            for (Show show : showManager.getShowsByPlayer(event.getPlayer().getUniqueId())) {
                //Only run if they were added by a region
                if (!show.getMembers().get(event.getPlayer().getUniqueId())) return;
                show.removeMember(event.getPlayer());
            }
        }
    }

}
