package org.jboss.as.quickstarts.xa.resources;

import javax.transaction.xa.XAResource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

public class MockXAResourceRecoveryHelper implements XAResourceRecoveryHelper {
    public static final MockXAResourceRecoveryHelper INSTANCE = new MockXAResourceRecoveryHelper();
    private static final MockXAResource mockXARecoveringInstance = new MockXAResource();

    private MockXAResourceRecoveryHelper() {
        if(INSTANCE != null) {
            throw new IllegalStateException("singleton instance can't be instantiated twice");
        }
    }

    @Override
    public boolean initialise(String p) throws Exception {
        MockXAResource.initPreparedXids(MockXAResourceStorage.recoverFromDisk());
        return true;
    }

    @Override
    public XAResource[] getXAResources() throws Exception {
        return new XAResource[] { mockXARecoveringInstance };
    }

}
