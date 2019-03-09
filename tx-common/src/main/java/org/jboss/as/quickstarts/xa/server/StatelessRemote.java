package org.jboss.as.quickstarts.xa.server;

import java.rmi.RemoteException;

public interface StatelessRemote {
    int transactionStatus() throws RemoteException;
    int call() throws RemoteException;
}
