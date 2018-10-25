package org.jboss.as.quickstarts.xa.client;

import static javax.ejb.TransactionManagementType.BEAN;

import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.quickstarts.xa.server.StatefulRemote;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

@Stateless
@TransactionManagement(BEAN)
@Remote (TransactionalLocal.class)
public class TransactionalLocalBean implements TransactionalLocal {
    private static final Logger log = Logger.getLogger(TransactionalLocalBean.class);

    @Resource
    private UserTransaction userTransaction;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Override
    public String transactionStatus() {
            try {
                StatelessRemote bean = getTransactionalBean(
                        "StatelessBean", StatelessRemote.class.getCanonicalName());

                // calling remote bean to find out the status of transaction
                int status = bean.transactionStatus();
                if(Status.STATUS_NO_TRANSACTION != status) {
                    return "ERROR: No transaction expected but transaction status was " + stringForm(status);
                }

                // remote call transaction propagation
                userTransaction.begin();
                try {
                    status = bean.call();
                    if(status != Status.STATUS_ACTIVE) {
                        return "ERROR: Active transaction expected but was " + stringForm(status);
                    }
                } finally {
                    userTransaction.rollback();
                }
            } catch (Exception e) {
                log.error("Cannot call remote stateless ejb or rollback transaction", e);
                return "ERROR: Error on calling remote ejb and rollbacking transaction";
            }

            return "SUCCESS";
    }

    @Override
    public String testSameTransactionEachCall() {
        try {
            StatefulRemote bean = getTransactionalStatefulBean(
                    "StatefulBean", StatefulRemote.class.getCanonicalName());

            userTransaction.begin();
            try {
                bean.sameTransaction(true);
                bean.sameTransaction(false);
            } finally {
                userTransaction.rollback();
            }
        } catch (Exception e) {
            log.error("Cannot call remote stateful ejb or rollback transaction", e);
            return "ERROR: Error on calling remote ejb and rollbacking transaction";
        }

        return "SUCCESS";
    }

    private StatelessRemote getTransactionalBean(String beanName, String viewClassName) throws NamingException {
        return (StatelessRemote) lookupBean(beanName, viewClassName, false);
    }

    private StatefulRemote getTransactionalStatefulBean(String beanName, String viewClassName) throws NamingException {
        return (StatefulRemote) lookupBean(beanName, viewClassName, true);
    }

    private Object lookupBean(String beanName, String viewClassName, boolean staeful) throws NamingException {
        Properties properties = new Properties();

        // remote lookup which does not utilize the remote binding
        // Hashtable properties = new Hashtable();
        // properties.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        // properties.put(javax.naming.Context.PROVIDER_URL,"http-remoting://localhost:8080");

        properties.put(javax.naming.Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        javax.naming.Context jndiContext = new javax.naming.InitialContext(properties);
        // context.lookup("ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName + "?stateful");
        String jndiName = String.format("ejb:/tx-server//%s!%s", beanName, viewClassName);
        if (staeful) jndiName += "?stateful";

        log.infof("looking up bean name %s", jndiName);
        return jndiContext.lookup(jndiName);
    }

    public static String stringForm(int status) {
        switch (status) {
            case javax.transaction.Status.STATUS_ACTIVE:
                return "javax.transaction.Status.STATUS_ACTIVE";
            case javax.transaction.Status.STATUS_COMMITTED:
                return "javax.transaction.Status.STATUS_COMMITTED";
            case javax.transaction.Status.STATUS_MARKED_ROLLBACK:
                return "javax.transaction.Status.STATUS_MARKED_ROLLBACK";
            case javax.transaction.Status.STATUS_NO_TRANSACTION:
                return "javax.transaction.Status.STATUS_NO_TRANSACTION";
            case javax.transaction.Status.STATUS_PREPARED:
                return "javax.transaction.Status.STATUS_PREPARED";
            case javax.transaction.Status.STATUS_PREPARING:
                return "javax.transaction.Status.STATUS_PREPARING";
            case javax.transaction.Status.STATUS_ROLLEDBACK:
                return "javax.transaction.Status.STATUS_ROLLEDBACK";
            case javax.transaction.Status.STATUS_ROLLING_BACK:
                return "javax.transaction.Status.STATUS_ROLLING_BACK";
            case javax.transaction.Status.STATUS_UNKNOWN:
            default:
                return "javax.transaction.Status.STATUS_UNKNOWN";
        }
    }
}
