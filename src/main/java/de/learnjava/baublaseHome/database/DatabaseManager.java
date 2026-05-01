package de.learnjava.baublaseHome.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    private final String host;
    private final int    port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseManager(Logger logger,
                           String host, int port,
                           String database,
                           String username, String password) {
        this.logger   = logger;
        this.host     = host;
        this.port     = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        config.setKeepaliveTime(60_000);
        config.setConnectionTestQuery("SELECT 1");

        config.setPoolName("MyPlugin-Pool");

        try {
            dataSource = new HikariDataSource(config);
            logger.info("[DB] Verbindung zum Pool hergestellt.");
        } catch (Exception e) {
            throw new RuntimeException("[DB] Fehler beim Aufbau des Connection-Pools!", e);
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[DB] Connection-Pool geschlossen.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void execute(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement  stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.severe("[DB] execute() fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int update(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[DB] update() fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public long insert(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            logger.severe("[DB] insert() fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public <T> Optional<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper.map(rs));
            }
        } catch (SQLException e) {
            logger.severe("[DB] query() fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            logger.severe("[DB] queryList() fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public void transaction(TransactionAction action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                action.run(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                logger.severe("[DB] Transaktion fehlgeschlagen, Rollback: " + e.getMessage());
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("[DB] Verbindung für Transaktion fehlgeschlagen: " + e.getMessage());
        }
    }

    private void setParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @FunctionalInterface
    public interface TransactionAction {
        void run(Connection conn) throws SQLException;
    }
}