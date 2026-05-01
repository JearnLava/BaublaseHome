package de.learnjava.baublaseHome.utils;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.repos.HomeObject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HomeUtils {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public void saveHome(Player player, String name, String locationString) {
        BaublaseHome plugin = BaublaseHome.getInstance();
        FileConfiguration config = plugin.getConfig();

        switch (plugin.getSavingMethod()) {

            case "mysql":
                if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                    HomeObject home = new HomeObject(
                            player.getUniqueId().toString(),
                            name,
                            locationString
                    );
                    plugin.getHomeRepository().insertAsync(home);
                    send(player, config.getString("messages.created_succesfully"), name);
                } else {
                    send(player, "<red>Database nicht verbunden!", name);
                }
                break;

            case "config":
                saveToFile(player, name, locationString);
                send(player, config.getString("messages.created_succesfully"), name);
                break;

            case "local":
                plugin.getPlayerHomes()
                        .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                        .put(name, new HomeObject(
                                player.getUniqueId().toString(),
                                name,
                                locationString
                        ));
                send(player, config.getString("messages.created_succesfully"), name);
                break;
        }
    }

    public int getCurrentHomes(Player player) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        switch (plugin.getSavingMethod()) {
            case "config":
                File file = new File(plugin.getDataFolder(), "homes/" + player.getUniqueId() + ".yml");
                if (!file.exists()) return 0;
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                if (!yaml.contains("homes")) return 0;
                return yaml.getConfigurationSection("homes").getKeys(false).size();
            case "local":
            case "mysql":
                Map<String, HomeObject> homes = plugin.getPlayerHomes().get(player.getUniqueId());
                return homes != null ? homes.size() : 0;
        }

        return 0;
    }

    public int getMaxHomes(Player player) {
        BaublaseHome plugin = BaublaseHome.getInstance();
        String basePerm = plugin.getConfig().getString("permissions.create");

        int max = 0;
        if (basePerm == null) return 0;

        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission(basePerm + "." + i)) {
                max = i;
            }
        }

        return max;
    }

    private void saveToFile(Player player, String name, String locationString) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        File folder = new File(plugin.getDataFolder(), "homes");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, player.getUniqueId().toString() + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        yaml.set("homes." + name + ".location", locationString);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern!");
            e.printStackTrace();
        }
    }

    private void send(Player player, String message, String homeName) {
        if (message == null) return;
        message = replaceVars(message, player, homeName, getMaxHomes(player));
        player.sendMessage(miniMessage.deserialize(message));
    }

    public String replaceVars(String text, Player player, String homeName, int maxHomes) {
        if (text == null) return "";
        return text
                .replace("%player%", player.getName())
                .replace("%home%", homeName)
                .replace("%HOME%", homeName)
                .replace("%MAX_HOMES%", String.valueOf(maxHomes));
    }
}