package de.learnjava.baublaseHome.database.repos;

import de.learnjava.baublaseHome.database.BaseRepository;
import de.learnjava.baublaseHome.database.DatabaseManager;
import de.learnjava.baublaseHome.database.RowMapper;
import de.learnjava.baublaseHome.database.dto.HomeAccessObject;
import org.bukkit.Bukkit;

import de.learnjava.baublaseHome.BaublaseHome;

import java.util.List;
import java.util.UUID;

public class HomeAccessRepository extends BaseRepository<HomeAccessObject> {

    private static final RowMapper<HomeAccessObject> MAPPER = rs -> new HomeAccessObject(
            rs.getString("owner_uuid"),
            rs.getString("home_name"),
            rs.getString("allowed_uuid")
    );

    public HomeAccessRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    public void createTable() {
        db.execute(
                "CREATE TABLE IF NOT EXISTS home_access (" +
                        "  owner_uuid   VARCHAR(36) NOT NULL," +
                        "  home_name    VARCHAR(16) NOT NULL," +
                        "  allowed_uuid VARCHAR(36) NOT NULL," +
                        "  PRIMARY KEY (owner_uuid, home_name, allowed_uuid)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        );
    }

    public void addAccess(UUID ownerUuid, String homeName, UUID allowedUuid) {
        insert(
                "INSERT IGNORE INTO home_access (owner_uuid, home_name, allowed_uuid) VALUES (?, ?, ?)",
                ownerUuid.toString(),
                homeName,
                allowedUuid.toString()
        );
    }

    public void removeAccess(UUID ownerUuid, String homeName, UUID allowedUuid) {
        update(
                "DELETE FROM home_access WHERE owner_uuid = ? AND home_name = ? AND allowed_uuid = ?",
                ownerUuid.toString(),
                homeName,
                allowedUuid.toString()
        );
    }

    public List<HomeAccessObject> findByHome(UUID ownerUuid, String homeName) {
        return queryList(
                "SELECT * FROM home_access WHERE owner_uuid = ? AND home_name = ?",
                MAPPER,
                ownerUuid.toString(),
                homeName
        );
    }

    public boolean hasAccess(UUID ownerUuid, String homeName, UUID allowedUuid) {
        return query(
                "SELECT * FROM home_access WHERE owner_uuid = ? AND home_name = ? AND allowed_uuid = ?",
                MAPPER,
                ownerUuid.toString(),
                homeName,
                allowedUuid.toString()
        ).isPresent();
    }

    public void addAccessAsync(UUID ownerUuid, String homeName, UUID allowedUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(
                BaublaseHome.getInstance(),
                () -> addAccess(ownerUuid, homeName, allowedUuid)
        );
    }

    public void removeAccessAsync(UUID ownerUuid, String homeName, UUID allowedUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(
                BaublaseHome.getInstance(),
                () -> removeAccess(ownerUuid, homeName, allowedUuid)
        );
    }
}
