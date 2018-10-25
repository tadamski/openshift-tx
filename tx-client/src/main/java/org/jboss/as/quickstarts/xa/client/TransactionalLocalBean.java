package org.jboss.as.quickstarts.xa.client;

import static javax.ejb.TransactionManagementType.BEAN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.quickstarts.xa.server.StatefulRemote;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;

@Stateless
@TransactionManagement(BEAN)
@Remote (TransactionalLocal.class)
public class TransactionalLocalBean implements TransactionalLocal {
    @Resource
    private UserTransaction userTransaction;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    private StatelessRemote statelessRemote;

    private StatefulRemote statefulEJB;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String transactionStatus() {
            String message;

            try {
                StatelessRemote bean = getTransactionalBean(
                        "StatelessBean", StatelessRemote.class.getCanonicalName());

                assert Status.STATUS_NO_TRANSACTION == bean.transactionStatus() : "No transaction expected!";
                userTransaction.begin();
                try {
                    assert Status.STATUS_ACTIVE == bean.transactionStatus() : "Active transaction expected!";
                } finally {
                    userTransaction.rollback();
                    message = "success";
                }
            } catch (RemoteException | NotSupportedException | SystemException | IllegalStateException |
                    SecurityException | NamingException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                message = sw.toString();
            }

            return message;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String testSameTransactionEachCall() {
        String message;

        try {
            StatefulRemote bean = getTransactionalStatefulBean(
                    "StatefulBean", StatefulRemote.class.getCanonicalName());

            userTransaction.begin();
            try {
                bean.sameTransaction(true);
                bean.sameTransaction(false);
            } finally {
                userTransaction.rollback();
                message = "success";
            }
        } catch (NotSupportedException | SystemException | RemoteException | IllegalStateException | SecurityException
                | NamingException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            message = sw.toString();
        }

        return message;
    }

    private StatelessRemote getTransactionalBean(String beanName, String viewClassName) throws NamingException {
        if (statelessRemote == null)
            statelessRemote = (StatelessRemote) lookupBean(beanName, viewClassName, false);

        return statelessRemote;
    }

    private StatefulRemote getTransactionalStatefulBean(String beanName, String viewClassName) throws NamingException {
        if (statefulEJB == null)
            statefulEJB = (StatefulRemote) lookupBean(beanName, viewClassName, true);

        return statefulEJB;
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

        System.out.printf("looking up bean name %s%n", jndiName);
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
