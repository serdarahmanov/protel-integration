package com.limehousehotel.protelapi.repos;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Repository
public class SyncStateRepository {
    private final JdbcTemplate jdbc;

    public SyncStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Instant getLastModified(String key) {
        LocalDateTime dt = jdbc.queryForObject(
                "SELECT last_modified_at FROM protel_sync_state WHERE sync_key = ?",
                LocalDateTime.class,
                key
        );
        return dt == null ? null : dt.toInstant(ZoneOffset.UTC);
    }

    public void updateLastModified(String key, Instant value) {
        LocalDateTime dt = LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        jdbc.update(
                "UPDATE protel_sync_state SET last_modified_at = ? WHERE sync_key = ?",
                dt, key
        );
    }
}
