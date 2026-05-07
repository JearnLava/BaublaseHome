package de.learnjava.baublaseHome.utils;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.dto.HomeObject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class HomeUtils {

    public static final String PREFIX =
            " <bold><gradient:#FA56AF:#4498DB>Baublase</gradient></bold> <dark_gray>| </dark_gray>";

    private static final String VALID_NAME_REGEX = "[a-zA-Z0-9-]+";

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public enum NameResult {
        OK,
        TOO_LONG,
        INVALID_CHARS
    }

    public NameResult validateName(String name) {
        if (name.length() > 10) return NameResult.TOO_LONG;
        if (!name.matches(VALID_NAME_REGEX)) return NameResult.INVALID_CHARS;
        return NameResult.OK;
    }

    public void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(MM.deserialize(PREFIX + message));
    }

    public void send(Player player, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        send(player, message);
    }

    public int getCurrentHomes(Player player) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        Map<String, HomeObject> homes =
                plugin.getPlayerHomes().get(player.getUniqueId());

        return homes != null ? homes.size() : 0;
    }

    public int getMaxHomes(Player player) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        String basePerm = plugin.getConfig().getString("permissions.create");
        if (basePerm == null) return 0;

        int max = 0;

        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission(basePerm + "." + i)) {
                max = i;
            }
        }

        return max;
    }

    public void saveHome(Player player, String name, Location location) {
        String locationString = String.format(
                Locale.US,
                "%s:%.6f:%.6f:%.6f:%.4f:%.4f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );

        saveHome(player, name, locationString);
    }

    public void saveHome(Player player, String name, String locationString) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        if (plugin.getDatabaseManager() == null
                || !plugin.getDatabaseManager().isConnected()) {

            send(player,
                    "<gray>Es ist etwas schiefgelaufen. Bitte versuche es erneut!</gray>");
            return;
        }

        HomeObject home = new HomeObject(
                player.getUniqueId().toString(),
                name,
                locationString
        );

        plugin.getHomeRepository().insertAsync(home);

        plugin.getPlayerHomes()
                .computeIfAbsent(player.getUniqueId(),
                        k -> new HashMap<>())
                .put(name, home);

        send(
                player,
                plugin.getConfig().getString("messages.home_created", ""),
                placeholders(player, name, getMaxHomes(player))
        );
    }

    public Map<String, HomeObject> getAllHomes(Player player) {
        BaublaseHome plugin = BaublaseHome.getInstance();


        return plugin.getPlayerHomes()
                .getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    public Optional<HomeObject> getHome(Player player, String name) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        Map<String, HomeObject> homes =
                plugin.getPlayerHomes().get(player.getUniqueId());

        if (homes == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(homes.get(name));
    }

    public boolean deleteHome(Player player, String name) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        Map<String, HomeObject> homes =
                plugin.getPlayerHomes().get(player.getUniqueId());

        if (homes == null || !homes.containsKey(name)) {
            return false;
        }

        homes.remove(name);

        plugin.getHomeRepository()
                .deleteHomeAsync(player.getUniqueId(), name);

        return true;
    }

    public Location deserializeLocation(String s) {
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

    private Map<String, String> placeholders(
            Player player,
            String homeName,
            int maxHomes
    ) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("%player%", player.getName());
        map.put("%home%", homeName);
        map.put("%HOME%", homeName);
        map.put("%MAX_HOMES%", String.valueOf(maxHomes));
        map.put("%CURRENT_HOMES%", String.valueOf(getCurrentHomes(player)));

        return map;
    }
}