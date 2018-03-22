package org.jboss.as.quickstarts.xa.client;

public interface TransactionalLocal {
    String transactionStatus();
    String testSameTransactionEachCall();
}
