package com.icthh.xm.commons.migration.db;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static com.icthh.xm.commons.migration.db.Constants.JPA_VENDOR;

@Slf4j
@Component
public class DatabaseTxIdResolver {

    //for postgres version < 13.0
    private static final String POSTGRES_OLD_METHOD = "txid_current";

    //for postgres version >= 13.0
    private static final String POSTGRES_NEW_METHOD = "pg_current_xact_id";

    /**
     * For each thread transactionHolder stores map that contains spring managed transaction status as key
     * and real database transaction id as value.
     * In each thread we can have multiple isolated transactions, so map will contain ids for all transactions
     * opened in current thread, and will prevent redundant database queries for tx id we already know.
     *
     */
    //ThreadLocal<map<spring_transaction, dbTransactionId>>
    private static final ThreadLocal<Map<TransactionStatus, String>> transactionHolder =
        ThreadLocal.withInitial(WeakHashMap::new);

    private final String dbTransactionIdCommand;
    private final DataSource dataSource;

    public DatabaseTxIdResolver(Environment env, DataSource dataSource) {
        this.dataSource = dataSource;
        String jpaVendor = env.getProperty(JPA_VENDOR);
        Objects.requireNonNull(jpaVendor, "Unknown JPA vendor");
        this.dbTransactionIdCommand = initDbCommand(jpaVendor);
    }

    private String initDbCommand(String jpaVendor) {
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
            case "MYSQL":
                return "SELECT tx.trx_id " +
                    "FROM information_schema.innodb_trx tx " +
                    "WHERE tx.trx_mysql_thread_id = connection_id()";
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

    /**
     * If transaction is opened and not readonly will execute query to get transaction id
     * that is assigned at database level.
     * If transaction is not opened or transaction is readonly will return null.
     * If method is called multiple times for the same transaction, query will be run only once,
     * next method calls will return cached transaction id from `transactionHolder`.
     *
     * @return id of the current transaction in database. Or null if transaction is absent or readonly.
     */
    public String getDatabaseTransactionId() {
        TransactionStatus transactionStatus = null;
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                transactionStatus = TransactionAspectSupport.currentTransactionStatus();
            } else {
                log.debug("No transaction is found for thread {}", Thread.currentThread().getName());
            }
        } catch (Exception e) {
            log.error("Not possible to get transaction id.", e);
        }
        //if transaction is absent or readonly id will not be assigned, no need to run sql query
        if (transactionStatus == null || isReadOnly(transactionStatus)) {
            return null;
        }

        return transactionHolder.get().computeIfAbsent(transactionStatus, k -> getDbTransactionId());
    }

    private boolean isReadOnly(TransactionStatus transactionStatus) {
        return transactionStatus instanceof DefaultTransactionStatus
            && ((DefaultTransactionStatus) transactionStatus).isReadOnly();
    }

    private String getDbTransactionId() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(this.dbTransactionIdCommand)) {
            if (rs.next()) {
                return rs.getString(1);
            } else {
                log.error("Transaction is not open for thread: {}", Thread.currentThread().getName());
                return null;
            }
        } catch (SQLException sqlException) {
            throw new HibernateException(
                "Cant get db transaction id for thread: " + Thread.currentThread().getName(),
                sqlException);
        }
    }


}
