package de.learnjava.baublaseHome;

import de.learnjava.baublaseHome.commands.HomeCMD;
import de.learnjava.baublaseHome.database.DatabaseManager;
import de.learnjava.baublaseHome.database.dto.HomeObject;
import de.learnjava.baublaseHome.database.repos.HomeAccessRepository;
import de.learnjava.baublaseHome.database.repos.HomeRepository;
import de.learnjava.baublaseHome.listener.ConnectionListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BaublaseHome extends JavaPlugin {

    @Getter
    private static BaublaseHome instance;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private HomeRepository homeRepository;

    @Getter
    private HomeAccessRepository homeAccessRepository;

    @Getter
    private final Map<UUID, Map<String, HomeObject>> playerHomes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        String host     = getConfig().getString("database.host");
        int    port     = getConfig().getInt("database.port");
        String database = getConfig().getString("database.database");
        String user     = getConfig().getString("database.user");
        String password = getConfig().getString("database.password");

        databaseManager = new DatabaseManager(getLogger(), host, port, database, user, password);
        databaseManager.connect();

        homeRepository = new HomeRepository(databaseManager, this);
        homeRepository.createTable();

        homeAccessRepository = new HomeAccessRepository(databaseManager);
        homeAccessRepository.createTable();

        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);

        HomeCMD homeCMD = new HomeCMD();
        getCommand("home").setExecutor(homeCMD);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null && databaseManager.isConnected()) {
            databaseManager.disconnect();
        }
    }

    public String getSavingMethod() {
        return getConfig().getString("storage.method", "config");
    }
}
