package org.jboss.as.quickstarts.xa.server.resources;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

import java.io.Serializable;

public class MockXAResource implements XAResource, Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(MockXAResource.class);

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
        log.infof("Creating %s with test action %s", this, testAction);
        this.testAction = testAction;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        log.infof("prepare '%s' xid: [%s]", this, xid);

        switch (testAction) {
            case PREPARE_THROW_XAER_RMERR:
                log.info("at prepare '%s' throws XAException(XAException.XAER_RMERR)");
                throw new XAException(XAException.XAER_RMERR);
            case PREPARE_THROW_XAER_RMFAIL:
                log.info("at prepare '%s' throws XAException(XAException.XAER_RMFAIL)");
                throw new XAException(XAException.XAER_RMFAIL);
            case PREPARE_THROW_UNKNOWN_XA_EXCEPTION:
                log.info("at prepare '%s' throws XAException(null)");
                throw new XAException(null);
            case PREPARE_JVM_HALT:
                log.info("at prepare '%s' halting JVM");
                Runtime.getRuntime().halt(1);
            case NONE:
            default:
                return XAResource.XA_OK;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        log.infof("commit '%s' xid:[%s], %s one phase", this, xid, onePhase ? "with" : "without");

        switch (testAction) {
            case COMMIT_THROW_XAER_RMERR:
                log.info("at commit '%s' throws XAException(XAException.XAER_RMERR)");
                throw new XAException(XAException.XAER_RMERR);
            case COMMIT_THROW_XAER_RMFAIL:
                log.info("at commit '%s' throws XAException(XAException.XAER_RMFAIL)");
                throw new XAException(XAException.XAER_RMFAIL);
            case COMMIT_THROW_UNKNOWN_XA_EXCEPTION:
                log.info("at commit '%s' throws XAException(null)");
                throw new XAException(null);
            case COMMIT_JVM_HALT:
                log.info("at commit '%s' halting JVM");
                Runtime.getRuntime().halt(1);
            case NONE:
            default:
                // do nothing
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.infof("end '%s' xid:[%s], flag: %s", this, xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.infof("forget '%s' xid:[%s]", this, xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        log.infof("getTransactionTimeout: '%s' returning timeout: %s", this, transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.tracef("isSameRM returning false to xares: %s", xares);
        return false;
    }

    // TODO: !
    @Override
    public Xid[] recover(int flag) throws XAException {
        log.infof("recover '%s' with flags: %s, returning empty list", this, flag);
        return new Xid[]{};
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.infof("rollback '%s' xid: [%s]", this, xid);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        log.tracef("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.infof("start '%s' xid: [%s], flags: %s", this, xid, flags);
    }
}
