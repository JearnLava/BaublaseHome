package de.learnjava.baublaseHome.database.repos;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.BaseRepository;
import de.learnjava.baublaseHome.database.DatabaseManager;
import de.learnjava.baublaseHome.database.RowMapper;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Consumer;

public class HomeRepository extends BaseRepository<HomeObject> {

    private static final RowMapper<HomeObject> MAPPER = rs -> new HomeObject(
            rs.getString("uuid"),
            rs.getString("name"),
            rs.getString("locationFromString")
    );

    private final BaublaseHome plugin;
    private final String tableName;

    public HomeRepository(DatabaseManager db, BaublaseHome plugin) {
        super(db);
        this.plugin = plugin;
        this.tableName = plugin.getConfig().getString("database.table", "player_homes");
    }

    @Override
    public void createTable() {
        db.execute(
                "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                        "uuid CHAR(36) NOT NULL," +
                        "name VARCHAR(255) NOT NULL," +
                        "locationFromString VARCHAR(255) NOT NULL," +
                        "PRIMARY KEY (uuid, name)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        );
    }

    public void insert(HomeObject entry) {

        insert(
                "INSERT INTO " + tableName + " (uuid, name, locationFromString) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE locationFromString = VALUES(locationFromString)",
                entry.getUuid(),
                entry.getName(),
                entry.getLocationFromString()
        );

        UUID uuid = UUID.fromString(entry.getUuid());

        plugin.getPlayerHomes()
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .put(entry.getName(), entry);
    }

    public Optional<HomeObject> findHome(UUID uuid, String name) {

        if (!plugin.getPlayerHomes().containsKey(uuid)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                plugin.getPlayerHomes().get(uuid).get(name)
        );
    }

    public Map<String, HomeObject> findAllHomes(UUID uuid) {

        List<HomeObject> list = queryList(
                "SELECT * FROM " + tableName + " WHERE uuid = ?",
                MAPPER,
                uuid.toString()
        );

        Map<String, HomeObject> map = new HashMap<>();

        for (HomeObject home : list) {
            map.put(home.getName(), home);
        }

        plugin.getPlayerHomes().put(uuid, map);

        return map;
    }

    public void insertAsync(HomeObject entry) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            insert(
                    "INSERT INTO " + tableName + " (uuid, name, locationFromString) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE locationFromString = VALUES(locationFromString)",
                    entry.getUuid(),
                    entry.getName(),
                    entry.getLocationFromString()
            );

        });

        UUID uuid = UUID.fromString(entry.getUuid());

        plugin.getPlayerHomes()
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .put(entry.getName(), entry);
    }

    public void deleteHomeAsync(UUID uuid, String name) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            update(
                    "DELETE FROM " + tableName + " WHERE uuid = ? AND name = ?",
                    uuid.toString(),
                    name
            );
        });

        if (plugin.getPlayerHomes().containsKey(uuid)) {
            plugin.getPlayerHomes().get(uuid).remove(name);
        }
    }

    public void countHomesAsync(UUID uuid, Consumer<Integer> callback) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            int count = queryList(
                    "SELECT * FROM " + tableName + " WHERE uuid = ?",
                    MAPPER,
                    uuid.toString()
            ).size();

            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(count));
        });
    }

    public void deleteHome(UUID uuid, String name) {

        update(
                "DELETE FROM " + tableName + " WHERE uuid = ? AND name = ?",
                uuid.toString(),
                name
        );

        if (plugin.getPlayerHomes().containsKey(uuid)) {
            plugin.getPlayerHomes().get(uuid).remove(name);
        }
    }

    public void deleteAllHomes(UUID uuid) {

        update(
                "DELETE FROM " + tableName + " WHERE uuid = ?",
                uuid.toString()
        );

        plugin.getPlayerHomes().remove(uuid);
    }
}