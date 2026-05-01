package de.learnjava.baublaseHome.commands;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.repos.HomeObject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class HomeCMD implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        BaublaseHome plugin = BaublaseHome.getInstance();

        if (args.length == 0) {
            send(player, plugin.getConfig().getString("messages.put_name"));
            return true;
        }

        String name = args[0];
        String locString = null;

        switch (plugin.getSavingMethod()) {

            case "mysql":
                Optional<HomeObject> home = plugin.getHomeRepository()
                        .findHome(player.getUniqueId(), name);

                locString = home.map(HomeObject::getLocationFromString).orElse(null);
                break;

            case "config":
                var file = new java.io.File(plugin.getDataFolder(),
                        "homes/" + player.getUniqueId() + ".yml");

                if (file.exists()) {
                    var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                    locString = yaml.getString("homes." + name + ".location");
                }
                break;

            case "local":
                Map<String, HomeObject> homes = plugin.getPlayerHomes().get(player.getUniqueId());
                if (homes != null && homes.containsKey(name)) {
                    locString = homes.get(name).getLocationFromString();
                }
                break;
        }

        if (locString == null) {
            send(player, plugin.getConfig().getString("messages.no_homes"));
            return true;
        }

        Location loc = deserialize(locString);
        if (loc == null) {
            player.sendMessage("§cWorld nicht geladen!");
            return true;
        }

        player.teleport(loc);

        send(player, plugin.getConfig().getString("messages.teleported"), name);
        return true;
    }

    private Location deserialize(String s) {
        try {
            s = s.replace(",", ".");
            String[] split = s.split(":");

            World world = Bukkit.getWorld(split[0]);
            if (world == null) return null;

            return new Location(
                    world,
                    Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]),
                    Double.parseDouble(split[3]),
                    Float.parseFloat(split[4]),
                    Float.parseFloat(split[5])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void send(Player player, String msg) {
        if (msg != null) player.sendMessage(mm.deserialize(msg));
    }

    private void send(Player player, String msg, String home) {
        if (msg != null) {
            player.sendMessage(mm.deserialize(msg.replace("%HOME%", home)));
        }
    }

}