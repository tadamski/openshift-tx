package org.jboss.as.quickstarts.xa.server.resource;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

public class MockXAResource implements XAResource {
    private static Logger log = Logger.getLogger(MockXAResource.class);

    public enum TestAction {
        NONE,
        PREPARE_THROW_XAER_RMERR, PREPARE_THROW_XAER_RMFAIL, PREPARE_THROW_UNKNOWN_XA_EXCEPTION,
        COMMIT_THROW_XAER_RMERR, COMMIT_THROW_XAER_RMFAIL, COMMIT_THROW_UNKNOWN_XA_EXCEPTION
    }

    protected TestAction testAction;
    private int transactionTimeout;

    public MockXAResource() {
        this.testAction = TestAction.COMMIT_THROW_XAER_RMFAIL;
        System.out.println("TWORZE TEST RESOURCE Z TEST ACTION "+testAction);
    }

    public MockXAResource(TestAction testAction) {
        this.testAction = testAction;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        System.out.println("IDZIE PREPARE");
        log.tracef("prepare xid: [%s]", xid);

        switch (testAction) {
            case PREPARE_THROW_XAER_RMERR:
                System.out.println("WYJATEK");
                throw new XAException(XAException.XAER_RMERR);
            case PREPARE_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case PREPARE_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case NONE:
            default:
                return XAResource.XA_OK;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        System.out.println("IDZIE COMMIT");
        log.tracef("commit xid:[%s], %s one phase", xid, onePhase ? "with" : "without");

        switch (testAction) {
            case COMMIT_THROW_XAER_RMERR:
                throw new XAException(XAException.XAER_RMERR);
            case COMMIT_THROW_XAER_RMFAIL:
                throw new XAException(XAException.XAER_RMFAIL);
            case COMMIT_THROW_UNKNOWN_XA_EXCEPTION:
                throw new XAException(null);
            case NONE:
            default:
                // do nothing
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        log.tracef("end xid:[%s], flag: %s", xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        log.tracef("forget xid:[%s]", xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        log.tracef("getTransactionTimeout: returning timeout: %s", transactionTimeout);
        return transactionTimeout;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        log.tracef("isSameRM returning false to xares: %s", xares);
        return false;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        log.tracef("recover with flags: %s", flag);
        return new Xid[]{};
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        log.tracef("rollback xid: [%s]", xid);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        log.tracef("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        log.tracef("start xid: [%s], flags: %s", xid, flags);
    }
}
