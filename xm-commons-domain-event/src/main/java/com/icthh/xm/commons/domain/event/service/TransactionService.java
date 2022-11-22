package com.icthh.xm.commons.domain.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    //ThreadLocal<map<spring_transaction, dbTransactionId>>
    private static final ThreadLocal<Map<TransactionStatus, String>> transactionHolder =
        ThreadLocal.withInitial(HashMap::new);

    private final DataSource dataSource;
    private final TxIdResolver txIdResolver;

    public String getDatabaseTransactionId() {
        TransactionStatus transactionStatus = null;
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                transactionStatus = TransactionAspectSupport.currentTransactionStatus();
            } else {
                log.info("No transaction is found for thread {}", Thread.currentThread().getName());
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
        String query = txIdResolver.getTransactionIdCommand();
        try (Statement stmt = dataSource.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
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
