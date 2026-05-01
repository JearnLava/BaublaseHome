package de.learnjava.baublaseHome.database;

import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<T> {

    protected final DatabaseManager db;

    protected BaseRepository(DatabaseManager db) {
        this.db = db;
    }

    public abstract void createTable();

    protected int update(String sql, Object... params) {
        return db.update(sql, params);
    }

    protected long insert(String sql, Object... params) {
        return db.insert(sql, params);
    }

    protected Optional<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return db.query(sql, mapper, params);
    }

    protected List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        return db.queryList(sql, mapper, params);
    }
}