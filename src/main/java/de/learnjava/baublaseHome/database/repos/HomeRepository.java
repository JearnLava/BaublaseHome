package de.learnjava.baublaseHome.database.repos;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.BaseRepository;
import de.learnjava.baublaseHome.database.DatabaseManager;
import de.learnjava.baublaseHome.database.RowMapper;
import de.learnjava.baublaseHome.database.dto.HomeObject;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Consumer;

public class HomeRepository extends BaseRepository<HomeObject> {

    private static final RowMapper<HomeObject> MAPPER = rs -> new HomeObject(
            rs.getString("uuid"),
            rs.getString("home_name"),
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch")
    );

    public HomeRepository(DatabaseManager db, BaublaseHome plugin) {
        super(db);
    }

    @Override
    public void createTable() {
        db.execute(
                "CREATE TABLE IF NOT EXISTS player_homes (" +
                        "  uuid      VARCHAR(36)  NOT NULL," +
                        "  home_name VARCHAR(16)  NOT NULL," +
                        "  world     VARCHAR(255) NOT NULL," +
                        "  x         DOUBLE       NOT NULL," +
                        "  y         DOUBLE       NOT NULL," +
                        "  z         DOUBLE       NOT NULL," +
                        "  yaw       FLOAT        NOT NULL," +
                        "  pitch     FLOAT        NOT NULL," +
                        "  PRIMARY KEY (uuid, home_name)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        );
    }

    public void insert(HomeObject entry) {
        insert(
                "INSERT INTO player_homes (uuid, home_name, world, x, y, z, yaw, pitch) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), " +
                        "y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)",
                entry.getUuid(),
                entry.getHomeName(),
                entry.getWorld(),
                entry.getX(),
                entry.getY(),
                entry.getZ(),
                entry.getYaw(),
                entry.getPitch()
        );

        UUID uuid = UUID.fromString(entry.getUuid());
        BaublaseHome.getInstance().getPlayerHomes()
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .put(entry.getHomeName(), entry);
    }

    public void deleteHome(UUID uuid, String name) {
        update(
                "DELETE FROM player_homes WHERE uuid = ? AND home_name = ?",
                uuid.toString(),
                name
        );

        Map<String, HomeObject> homes = BaublaseHome.getInstance().getPlayerHomes().get(uuid);
        if (homes != null) homes.remove(name);
    }

    public void deleteAllHomes(UUID uuid) {
        update("DELETE FROM player_homes WHERE uuid = ?", uuid.toString());
        BaublaseHome.getInstance().getPlayerHomes().remove(uuid);
    }

    public Optional<HomeObject> findHome(UUID uuid, String name) {
        Map<String, HomeObject> cached = BaublaseHome.getInstance().getPlayerHomes().get(uuid);
        if (cached != null) {
            return Optional.ofNullable(cached.get(name));
        }

        return query(
                "SELECT * FROM player_homes WHERE uuid = ? AND home_name = ?",
                MAPPER,
                uuid.toString(),
                name
        );
    }

    public Map<String, HomeObject> findAllHomes(UUID uuid) {
        List<HomeObject> list = queryList(
                "SELECT * FROM player_homes WHERE uuid = ?",
                MAPPER,
                uuid.toString()
        );

        Map<String, HomeObject> map = new HashMap<>();
        for (HomeObject home : list) {
            map.put(home.getHomeName(), home);
        }

        BaublaseHome.getInstance().getPlayerHomes().put(uuid, map);
        return map;
    }

    public void insertAsync(HomeObject entry) {
        UUID uuid = UUID.fromString(entry.getUuid());
        BaublaseHome.getInstance().getPlayerHomes()
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .put(entry.getHomeName(), entry);

        Bukkit.getScheduler().runTaskAsynchronously(BaublaseHome.getInstance(), () ->
                insert(
                        "INSERT INTO player_homes (uuid, home_name, world, x, y, z, yaw, pitch) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), " +
                                "y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)",
                        entry.getUuid(),
                        entry.getHomeName(),
                        entry.getWorld(),
                        entry.getX(),
                        entry.getY(),
                        entry.getZ(),
                        entry.getYaw(),
                        entry.getPitch()
                )
        );
    }

    public void deleteHomeAsync(UUID uuid, String name) {
        Map<String, HomeObject> homes = BaublaseHome.getInstance().getPlayerHomes().get(uuid);
        if (homes != null) homes.remove(name);

        Bukkit.getScheduler().runTaskAsynchronously(BaublaseHome.getInstance(), () ->
                update(
                        "DELETE FROM player_homes WHERE uuid = ? AND home_name = ?",
                        uuid.toString(),
                        name
                )
        );
    }

    public void countHomesAsync(UUID uuid, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(BaublaseHome.getInstance(), () -> {
            int count = queryList(
                    "SELECT * FROM player_homes WHERE uuid = ?",
                    MAPPER,
                    uuid.toString()
            ).size();
            Bukkit.getScheduler().runTask(BaublaseHome.getInstance(), () -> callback.accept(count));
        });
    }
}