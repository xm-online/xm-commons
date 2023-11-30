import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())

def operation = lepContext.inArgs.args[0]

TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
    @Override
    void afterCompletion(int status) {
        try {

            if (status == 0) {
                super.afterCommit()
                operation.call()
            }

        } catch(Exception e) {
            log.error("Error send sms", e)
            throw e;
        }
    }
});
