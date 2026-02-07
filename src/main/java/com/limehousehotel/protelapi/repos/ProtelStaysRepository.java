package com.limehousehotel.protelapi.repos;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class ProtelStaysRepository {

    private final JdbcTemplate jdbc;

    public ProtelStaysRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertStay(
            String protelReservationId,
            Long wpUserId,
            String guestEmail,
            java.time.LocalDate checkinDate,
            java.time.LocalDate checkoutDate,
            String roomType,
            Integer adults,
            Integer children,
            BigDecimal amountSpent,
            String currency,
            String resStatus,
            Instant modifiedAt
    ) {
        // Normalize inputs for DB consistency
        String normalizedEmail = (guestEmail == null) ? null : guestEmail.trim().toLowerCase();
        String normalizedCurrency = (currency == null || currency.isBlank()) ? "GBP" : currency.trim().toUpperCase();
        BigDecimal normalizedAmount = (amountSpent == null) ? BigDecimal.ZERO : amountSpent;

        var sql = """
          INSERT INTO Xs0Jq_protel_stays
            (protel_reservation_id, wp_user_id, guest_email,
             checkin_date, checkout_date,
             room_type, adults, children,
             amount_spent, currency,
             res_status, modified_at)
          VALUES
            (?, ?, ?,
             ?, ?,
             ?, ?, ?,
             ?, ?,
             ?, ?)
          ON DUPLICATE KEY UPDATE
            wp_user_id     = VALUES(wp_user_id),
            guest_email    = VALUES(guest_email),
            checkin_date   = VALUES(checkin_date),
            checkout_date  = VALUES(checkout_date),
            room_type      = VALUES(room_type),
            adults         = VALUES(adults),
            children       = VALUES(children),
            amount_spent   = VALUES(amount_spent),
            currency       = VALUES(currency),
            res_status     = VALUES(res_status),
            modified_at    = VALUES(modified_at)
          """;

        jdbc.update(sql,
                protelReservationId,
                wpUserId,
                normalizedEmail,
                checkinDate,
                checkoutDate,
                roomType,
                adults,
                children,
                normalizedAmount,
                normalizedCurrency,
                resStatus,
                modifiedAt != null ? Timestamp.from(modifiedAt) : null
        );
    }

    public record StayToAward(
            long stayId,
            long wpUserId,
            String reservationId,
            BigDecimal amountSpent,
            String currency,
            Instant modifiedAt
    ) {}

    /**
     * Finds completed stays that were not yet awarded.
     * Includes currency so WordPress can display correct currency in log and metadata.
     */
    public List<StayToAward> findCompletedNotAwarded() {
        var sql = """
          SELECT id, wp_user_id, protel_reservation_id, amount_spent, currency, modified_at
          FROM Xs0Jq_protel_stays
          WHERE res_status = 'CHECKED-OUT'
            AND wp_user_id IS NOT NULL
            AND points_awarded_at IS NULL
          ORDER BY modified_at ASC
          LIMIT 500
          """;

        return jdbc.query(sql, (rs, i) -> {
            var ts = rs.getTimestamp("modified_at");
            Instant modified = (ts != null) ? ts.toInstant() : null;

            String cur = rs.getString("currency");
            if (cur == null || cur.isBlank()) cur = "GBP";

            return new StayToAward(
                    rs.getLong("id"),
                    rs.getLong("wp_user_id"),
                    rs.getString("protel_reservation_id"),
                    rs.getBigDecimal("amount_spent"),
                    cur.trim().toUpperCase(),
                    modified
            );
        });
    }

    public void markAwarded(long stayId, Instant awardedAt) {
        jdbc.update("""
            UPDATE Xs0Jq_protel_stays
            SET points_awarded_at = ?
            WHERE id = ?
              AND points_awarded_at IS NULL
            """, awardedAt != null ? Timestamp.from(awardedAt) : null, stayId);
    }
}
