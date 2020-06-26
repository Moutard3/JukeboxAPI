package net.mcjukebox.plugin.bukkit.commands;

import lombok.AllArgsConstructor;
import net.mcjukebox.plugin.bukkit.api.JukeboxAPI;
import net.mcjukebox.plugin.bukkit.api.ResourceType;
import net.mcjukebox.plugin.bukkit.api.models.Media;
import net.mcjukebox.plugin.bukkit.utils.CommandUtils;
import net.mcjukebox.plugin.bukkit.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
public class PlayCommand extends JukeboxCommand {

    private final ResourceType type;

    @Override
    public boolean execute(CommandSender dispatcher, String[] args) {
        if (args.length < 2) return false;

        String url = args[1];
        Media toPlay = new Media(type, url);

        if (args.length >= 3) {
            JSONObject options = jsonFromArgs(args);

            if (options == null) {
                dispatcher.sendMessage(ChatColor.RED + "Unable to parse options as JSON.");
                return true;
            }

            toPlay.loadOptions(options);
        }

        List<Player> players = CommandUtils.selectorToPlayer(dispatcher, args[0]);
        if (!players.isEmpty()) {
            for (Player player: players) {
                if (player != null) {
                    JukeboxAPI.play(player, toPlay);
                }
            }
        } else if (args[0].startsWith("@")) {
            JukeboxAPI.getShowManager().getShow(args[0]).play(toPlay);
        } else {
            HashMap<String, String> findAndReplace = new HashMap<>();
            findAndReplace.put("user", args[0]);
            MessageUtils.sendMessage(dispatcher, "command.notOnline", findAndReplace);
        }

        return true;
    }

}
