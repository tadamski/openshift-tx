package org.jboss.as.quickstarts.xa.resources;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

public class MockXAResource implements XAResource, Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MockXAResource.class);

    // using Set for two Xids would not be part of the collection
    private static final Collection<Xid> preparedXids = ConcurrentHashMap.newKeySet();

    private Xid currentXid = null; 

    public enum TestAction {
        NONE,
        PREPARE_THROW_XAER_RMERR, PREPARE_THROW_XAER_RMFAIL, PREPARE_THROW_UNKNOWN_XA_EXCEPTION,
        COMMIT_THROW_XAER_RMERR, COMMIT_THROW_XAER_RMFAIL, COMMIT_THROW_UNKNOWN_XA_EXCEPTION,
        PREPARE_JVM_HALT, COMMIT_JVM_HALT
    }

    protected TestAction testAction;
    private int transactionTimeout;

    public MockXAResource() {
        this(TestAction.NONE);
    }

    public MockXAResource(TestAction testAction) {
        LOG.infof("Creating %s with test action %s", this, testAction);
        this.testAction = testAction;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        LOG.infof("prepare '%s' xid: [%s]", this, xid);
        if (!xid.equals(currentXid)) {
            LOG.warnf("%s.commit - wrong Xid. Wanted to commit '%s' but started Xid is '%s'",
                    this.getClass(), xid, currentXid);
        }


        switch (testAction) {
            case PREPARE_THROW_XAER_RMERR:
                LOG.info("at prepare '%s' throws XAException(XAException.XAER_RMERR)");
                throw new XAException(XAException.XAER_RMERR);
            case PREPARE_THROW_XAER_RMFAIL:
                LOG.info("at prepare '%s' throws XAException(XAException.XAER_RMFAIL)");
                throw new XAException(XAException.XAER_RMFAIL);
            case PREPARE_THROW_UNKNOWN_XA_EXCEPTION:
                LOG.info("at prepare '%s' throws XAException(null)");
                throw new XAException(null);
            case PREPARE_JVM_HALT:
                LOG.infof("at prepare '%s' halting JVM", xid);
                Runtime.getRuntime().halt(1);
            case NONE:
            default:
                preparedXids.add(xid);
                MockXAResourceStorage.writeToDisk(preparedXids);
                return XAResource.XA_OK;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        LOG.infof("commit '%s' xid:[%s], %s one phase", this, xid, onePhase ? "with" : "without");
        if (!xid.equals(currentXid)) {
            LOG.warnf("%s.rollback - wrong Xid. Wanted to rollback '%s' but started Xid is '%s'",
                    this.getClass(), xid, currentXid);
        }

        switch (testAction) {
            case COMMIT_THROW_XAER_RMERR:
                LOG.info("at commit '%s' throws XAException(XAException.XAER_RMERR)");
                throw new XAException(XAException.XAER_RMERR);
            case COMMIT_THROW_XAER_RMFAIL:
                LOG.info("at commit '%s' throws XAException(XAException.XAER_RMFAIL)");
                throw new XAException(XAException.XAER_RMFAIL);
            case COMMIT_THROW_UNKNOWN_XA_EXCEPTION:
                LOG.info("at commit '%s' throws XAException(null)");
                throw new XAException(null);
            case COMMIT_JVM_HALT:
                LOG.infof("at commit '%s' halting JVM", xid);
                Runtime.getRuntime().halt(1);
            case NONE:
            default:
                removeLog(xid);
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        LOG.infof("end '%s' xid:[%s], flag: %s", this, xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        LOG.infof("forget '%s' xid:[%s]", this, xid);
        removeLog(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        LOG.infof("getTransactionTimeout: '%s' returning timeout: %s", this, transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        LOG.tracef("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        LOG.debugf("recover '%s' with flags: %s, returning list of xids '%s'", this, flag, preparedXids);
        return preparedXids.toArray(new Xid[preparedXids.size()]);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        LOG.infof("rollback '%s' xid: [%s]", this, xid);
        removeLog(xid);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        LOG.tracef("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        LOG.infof("start '%s' xid: [%s], flags: %s", this, xid, flags);
        currentXid = xid;
    }

    /**
     * Loading 'prepared' xids from the persistent file storage.
     * Expected to be used just at the start of the application.
     */
    static synchronized void initPreparedXids(Collection<Xid> xidsToBeDefinedAsPrepared) {
        preparedXids.addAll(xidsToBeDefinedAsPrepared);
    }

    private void removeLog(Xid xid) {
        preparedXids.remove(xid);
        MockXAResourceStorage.writeToDisk(preparedXids);
        currentXid = null;
    }
}
