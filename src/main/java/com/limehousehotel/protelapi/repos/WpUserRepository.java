package com.limehousehotel.protelapi.repos;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WpUserRepository {
    private final JdbcTemplate jdbc;

    public WpUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        var sql = "SELECT ID FROM Xs0Jq_users WHERE user_email = ? LIMIT 1";
        var list = jdbc.query(sql, (rs, i) -> rs.getLong("ID"), email.trim().toLowerCase());
        return list.isEmpty() ? null : list.get(0);
    }
}
