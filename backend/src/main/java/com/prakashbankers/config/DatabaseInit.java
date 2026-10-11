package com.prakashbankers.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent, schema-level fixes Hibernate's ddl-auto=update will never apply on its own
 * (it never drops an existing NOT NULL constraint). Runs after the schema phase on every startup.
 */
@Component
public class DatabaseInit implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DatabaseInit(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Lender phone is now optional in the UI/entity. The lenders table created under the old
        // entity keeps phone NOT NULL, which makes saving a lender without a phone fail with
        // "Column 'phone' cannot be null". Make it nullable (idempotent: safe on a fresh DB too).
        Integer phoneExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'lenders' AND column_name = 'phone'",
                Integer.class);
        if (phoneExists != null && phoneExists > 0) {
            jdbc.execute("ALTER TABLE lenders MODIFY COLUMN phone varchar(255) NULL");
        }
    }
}