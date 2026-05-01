package de.learnjava.baublaseHome.commands;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.utils.HomeUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SetHomeCMD implements CommandExecutor {

    private final HomeUtils utils = new HomeUtils();
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        BaublaseHome plugin = BaublaseHome.getInstance();

        String perm = plugin.getConfig().getString("permissions.create");
        if (perm != null && !player.hasPermission(perm)) {
            send(player, plugin.getConfig().getString("messages.no_perms"));
            return true;
        }

        if (args.length == 0) {
            send(player, plugin.getConfig().getString("messages.put_name"));
            return true;
        }

        int maxHomes = utils.getMaxHomes(player);
        int currentHomes = utils.getCurrentHomes(player);

        if (currentHomes >= maxHomes && maxHomes != 0) {
            String msg = utils.replaceVars(
                    plugin.getConfig().getString("messages.max_homes"),
                    player,
                    "",
                    maxHomes
            );
            send(player, msg);
            return true;
        }

        String name = args[0];
        Location loc = player.getLocation();

        String locationString = String.format(Locale.US, "%s:%.6f:%.6f:%.6f:%.4f:%.4f",
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );

        utils.saveHome(player, name, locationString);
        return true;
    }

    private void send(Player player, String msg) {
        if (msg != null) player.sendMessage(mm.deserialize(msg));
    }
}