package com.icthh.xm.commons.domain.event.service;

import org.hibernate.HibernateException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.icthh.xm.commons.migration.db.Constants.JPA_VENDOR;

@Component
public class TxIdResolver {

    //for postgres version < 13.0
    private static final String POSTGRES_OLD_METHOD = "txid_current";

    //for postgres version >= 13.0
    private static final String POSTGRES_NEW_METHOD = "pg_current_xact_id";

    private final String dbTransactionIdCommand;

    public TxIdResolver(Environment env, DataSource dataSource) {
        String jpaVendor = env.getProperty(JPA_VENDOR);
        Objects.requireNonNull(jpaVendor, "Unknown JPA vendor");
        this.dbTransactionIdCommand = initDbCommand(jpaVendor, dataSource);
    }

    public String getTransactionIdCommand() {
        return this.dbTransactionIdCommand;
    }

    private String initDbCommand(String jpaVendor, DataSource dataSource) {
        switch (jpaVendor) {
            case "POSTGRESQL":
                return String.format("SELECT CAST(%s() AS text);", getPostgresTxIdMethod(dataSource));
            case "ORACLE":
                return "SELECT RAWTOHEX(tx.xid) " +
                    "FROM v$transaction tx " +
                    "JOIN v$session s ON tx.addr=s.taddr";
            case "H2":
                //TODO how to get txId from H2?
                return "SELECT 'not_implemented'";
            default:
                throw new IllegalStateException(
                    "Cant define sql command to get transaction id, database: " + jpaVendor + " not supported."
                );
        }
    }


    /**
     * PostgreSQL method to get transaction id was renamed for versions starting from 13.0,
     * need support for versions before and after 13.0.
     *
     * @param dataSource datasource to connect to postgres database.
     * @return supported method name to get current transaction id.
     */
    private String getPostgresTxIdMethod(DataSource dataSource) {
        String methodExistsSql = "select exists(select * from pg_proc where proname = ?);";
        try (PreparedStatement stmt = dataSource.getConnection().prepareStatement(methodExistsSql)) {
            stmt.setString(1, POSTGRES_NEW_METHOD);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean methodExists = rs.getBoolean(1);
                return methodExists ? POSTGRES_NEW_METHOD : POSTGRES_OLD_METHOD;
            } else {
                return POSTGRES_OLD_METHOD;
            }
        } catch (SQLException sqlException) {
            throw new HibernateException(
                "Error occurred while receiving postgreSQL method to get current transaction id: ", sqlException
            );
        }
    }

}
