package com.limehousehotel.protelapi.repos;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;


@Repository
public class SyncStateRepository {
    private final JdbcTemplate jdbc;

    public SyncStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LocalDateTime getLastModified(String key) {
        return jdbc.queryForObject(
                "SELECT last_modified_at FROM protel_sync_state WHERE sync_key = ?",
                LocalDateTime.class,
                key
        );
    }

    public void updateLastModified(String key, LocalDateTime value) {
        jdbc.update(
                "UPDATE protel_sync_state SET last_modified_at = ? WHERE sync_key = ?",
                value, key
        );
    }
}
