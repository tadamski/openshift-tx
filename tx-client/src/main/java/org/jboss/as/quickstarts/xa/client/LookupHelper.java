package org.jboss.as.quickstarts.xa.client;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class LookupHelper {
    private static final String JNDI_PKG_PREFIXES = "org.jboss.ejb.client.naming";

    private LookupHelper() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class, cannot be instantiated");
    }

    public static <T> T lookupRemoteEJBOutbound(Class<? extends T> beanImplClass, Class<T> remoteInterface, boolean isStateful, Properties ejbProperties) throws NamingException {
        return lookupRemoteEJBOutbound(beanImplClass.getSimpleName(), remoteInterface, isStateful, ejbProperties);
    }

    public static <T> T lookupRemoteEJBOutbound(String beanImplName, Class<T> remoteInterface, Properties ejbProperties) throws NamingException {
        return lookupRemoteEJBOutbound(beanImplName, remoteInterface, false, ejbProperties);
    }

    @SuppressWarnings("unchecked")
    public static <T> T lookupRemoteEJBOutbound(String beanImplName, Class<T> remoteInterface, boolean isStateful, Properties ejbProperties) throws NamingException {
        String remoteDeploymentName = System.getProperty("tx.server.host", "tx-server"); // TODO: maybe change the way
        
        final Properties jndiProperties = new Properties();
        if(ejbProperties != null) jndiProperties.putAll(ejbProperties);
        jndiProperties.put(Context.URL_PKG_PREFIXES, JNDI_PKG_PREFIXES);
        // jndiProperties.put("jboss.naming.client.ejb.context", "true"); // ?
        final Context context = new InitialContext(jndiProperties);
        
        return (T) context.lookup("ejb:/" + remoteDeploymentName + "/" + beanImplName + "!"
                + remoteInterface.getName() + (isStateful ? "?stateful" : ""));
    }

    public static <T> T lookupModuleEJB(Class<T> beanImplClass, Properties ejbProperties) {
        final Properties jndiProperties = new Properties();
        if(ejbProperties != null) jndiProperties.putAll(ejbProperties);
        jndiProperties.put(Context.URL_PKG_PREFIXES, JNDI_PKG_PREFIXES);

        try {
            final Context context = new InitialContext(jndiProperties);
            return beanImplClass.cast(context.lookup("java:module/" + beanImplClass.getSimpleName()));
        } catch (NamingException ne) {
            throw new IllegalStateException("Not possible to lookup bean " + beanImplClass.getName(), ne);
        }
    }
}
