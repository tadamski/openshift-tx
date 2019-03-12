package org.jboss.as.quickstarts.xa.resources;

import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;

@Singleton
@Startup
public class MockXAResourceInitializer {
    private static Logger log = Logger.getLogger(MockXAResource.class);

    /**
     * register the recovery module with the transaction manager.
     */
    @PostConstruct
    public void postConstruct() {
        log.infof("Adding instance of the MockXAResourceRecoveryHelper '%s' to XARecoveryModule", MockXAResourceRecoveryHelper.INSTANCE);
        getRecoveryModule().addXAResourceRecoveryHelper(MockXAResourceRecoveryHelper.INSTANCE);
        MockXAResource.initPreparedXids(MockXAResourceStorage.recoverFromDisk());
    }

    /**
     * unregister the recovery module from the transaction manager.
     */
    @PreDestroy
    public void preDestroy() {
        log.infof("Stopping MockXAResource initializer by removing instance of helper '%s' from XARecoveryModule", MockXAResourceRecoveryHelper.INSTANCE);
        getRecoveryModule().removeXAResourceRecoveryHelper(MockXAResourceRecoveryHelper.INSTANCE);
    }

    private XARecoveryModule getRecoveryModule() {
        for (RecoveryModule recoveryModule : ((Vector<RecoveryModule>) RecoveryManager.manager().getModules())) {
            if (recoveryModule instanceof XARecoveryModule) {
                return (XARecoveryModule) recoveryModule;
            }
        }
        return null;
    }
}
