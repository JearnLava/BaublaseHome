package de.learnjava.baublaseHome.commands;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.repos.HomeObject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public class DelHomeCMD implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        BaublaseHome plugin = BaublaseHome.getInstance();

        String perm = plugin.getConfig().getString("permissions.delete");
        if (perm != null && !player.hasPermission(perm)) {
            send(player, plugin.getConfig().getString("messages.no_perms"));
            return true;
        }

        if (args.length == 0) {
            send(player, plugin.getConfig().getString("messages.put_name"));
            return true;
        }

        String name = args[0];

        switch (plugin.getSavingMethod()) {

            case "mysql":
                plugin.getHomeRepository().deleteHomeAsync(player.getUniqueId(), name);
                send(player, plugin.getConfig().getString("messages.deleted_succesfully"), name);
                break;

            case "config":
                File file = new File(plugin.getDataFolder(), "homes/" + player.getUniqueId() + ".yml");

                if (!file.exists()) {
                    send(player, plugin.getConfig().getString("messages.no_homes"));
                    return true;
                }

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                if (!yaml.contains("homes." + name)) {
                    send(player, plugin.getConfig().getString("messages.no_homes"));
                    return true;
                }

                yaml.set("homes." + name, null);

                try {
                    yaml.save(file);
                    send(player, plugin.getConfig().getString("messages.deleted_succesfully"), name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "local":
                if (!plugin.getPlayerHomes().containsKey(player.getUniqueId())) {
                    send(player, plugin.getConfig().getString("messages.no_homes"));
                    return true;
                }

                Map<String, HomeObject> homes = plugin.getPlayerHomes().get(player.getUniqueId());

                if (!homes.containsKey(name)) {
                    send(player, plugin.getConfig().getString("messages.no_homes"));
                    return true;
                }

                homes.remove(name);

                send(player, plugin.getConfig().getString("messages.deleted_succesfully"), name);
                break;
        }

        return true;
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