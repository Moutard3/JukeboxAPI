package com.oliverdunk.jukeboxapi.commands;

import com.oliverdunk.jukeboxapi.api.JukeboxAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JukeboxCommand implements CommandExecutor {

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GREEN + "Jukebox" + ChatColor.GRAY + "] " + ChatColor.WHITE;

    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if(!(commandSender instanceof Player)) return false;
        Player player = (Player) commandSender;
        if(!player.hasPermission("jukeboxapi.admin")){
            player.sendMessage(PREFIX + "You need the 'jukebox.admin' permission to do this.");
            return true;
        }
        if(args.length == 3){
            if(args[0].equalsIgnoreCase("music") | args[0].equalsIgnoreCase("sound")){
                Player playFor = Bukkit.getPlayer(args[1]);
                if(playFor == null){
                    player.sendMessage(PREFIX + args[1] + " is not currently online.");
                    return true;
                }
                if(args[0].equalsIgnoreCase("music")) JukeboxAPI.playSong(playFor, args[2]);
                else JukeboxAPI.playSoundEffect(playFor, args[2]);
            }else{

            }
        }else{
            player.sendMessage(PREFIX + "Usage: /jukeboxapi <music/sound> <player> <url>");
        }
        return true;
    }

}
