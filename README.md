eap64
narayana source code is 4.17.39.Final (I think)

# Starting minishift

```
 minishift delete
 cat ~/.minishift/config/config.json
 minishift start
 eval $(minishift oc-env)
 oc login -u developer -p developer # or oc login -u system:admin
 oc project eap-transactions
 minishift docker-env
 eval $(minishift docker-env)
 minishift addon apply registry-route
 #docker pull brew-pulp-docker01.web.prod.ext.phx2.redhat.com:8888/jboss-eap-7/eap71
```

# create new project

```
 oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"
```

## Importing EAP images

```
 # oc import-image jboss-eap-70 --from=registry.access.redhat.com/jboss-eap-7/eap70-openshift --confirm
 # oc import-image jboss-eap-64 --from=registry.access.redhat.com/jboss-eap-6/eap64-openshift --confirm
 oc import-image jboss-eap-71 --from=registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm
```

# create and deploy server

```
cat server.sh
```
see at [server.sh](./server.sh)

# create and deploy client

```
cat client.sh
```
see at [client.sh](./client.sh)

# trigger an ejb call

```
curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateful/arg'
curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateless/arg'
```

# Troubleshooting

```
oc build-logs tx-client-1
# oc login -u system:admin
oc rsh `oc get pods -n tx-client | grep Running | awk '{print $1}'
```

CLI reference: https://docs.openshift.com/enterprise/3.0/cli_reference/basic_cli_operations.html

# adding a user

Create a user called ejbuser that will be used for securing remote EJB calls:

Adding a user using the add-user.sh command generates an application-users.properties file which
needs be added to the S2I builds configuration directory.
It also also generates the secret that that needs to be placed in the client servers config file:

> To represent the user add the following to the server-identities definition &lt;secret value="dGVzdDEyMzQh" /&gt;

```
h-4.2$ pwd
/opt/eap/standalone/configuration
sh-4.2$ ../../bin/add-user.sh

What type of user do you wish to add?
 a) Management User (mgmt-users.properties)
 b) Application User (application-users.properties)
(a): b

Enter the details of the new user to add.
Using realm 'ApplicationRealm' as discovered from the existing property files.
Username : ejb
Password requirements are listed below. To modify these restrictions edit the add-user.properties configuration file.
 - The password must not be one of the following restricted values {root, admin, administrator}
 - The password must contain at least 8 characters, 1 alphabetic character(s), 1 digit(s), 1 non-alphanumeric symbol(s)
 - The password must be different from the username
Password :
Re-enter Password :
What groups do you want this user to belong to? (Please enter a comma separated list, or leave blank for none)[  ]:
About to add user 'ejb' for realm 'ApplicationRealm'
Is this correct yes/no? yes
Added user 'ejb' to file '/opt/eap/standalone/configuration/application-users.properties'
Added user 'ejb' with groups  to file '/opt/eap/standalone/configuration/application-roles.properties'
Is this new user going to be used for one AS process to connect to another AS process?
e.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.
yes/no? yes
```

To represent the user add the following to the server-identities definition `<secret value="dGVzdDEyMzQh" />`

# to debug

```
# debug apps: https://blog.openshift.com/debugging-java-applications-on-openshift-kubernetes/
oc expose service tx-server
oc set env dc/tx-server DEBUG=true # Enable the debug port
oc get pods
oc port-forward tx-client-3-jqn4x 8787:8787 &
oc port-forward tx-server-4-rrr2l 8788:8788 &
```

# Re-build

```
# starting the new build which clones changes from github
oc start-build tx-server
# use '-n' to optionally to define namespace/project if you switched to a different one -n eap-transactions
# use '--follow' to see in console output of the build (or later with 'oc logs build tx-server-#'

# command to start new pod with the newly built code
oc rollout latest tx-server
```

NOTE: the same can be run for the `tx-client` service

# Starting as standalone with WildFly cluster

* copy WildFly distro to two server directories (e.g. `wfly-server1`, `wfly-server2`)
* run them like `./bin/standalone.sh -c standalone-ha.xml` and `./bin/standalone.sh -c standalone-ha.xml -Djboss.socket.binding.port-offset=100 -Djboss.node.name=anothernode`
** be sure to define unique node name for each app server as it's needed for cluster to work correctly

# NOTES

* discovery of ejb clients at https://github.com/wildfly/jboss-ejb-client/blob/master/src/main/java/org/jboss/ejb/client/DiscoveryEJBClientInterceptor.java
* lookup for pods by selector and select their names (see http://blog.chalda.cz/2018/02/28/Querying-Open-Shift-API.html)
  `curl -k   -H "Authorization: Bearer $TOKEN"   -H 'Accept: application/json'   https://${ENDPOINT}/api/v1/namespaces/$NAMESPACE/pods?labelSelector=app%3Dtx-server | jq '.items[].metadata.name'`
* what the state after pod is installed means?
  ```
  NAME                READY     STATUS             RESTARTS   AGE
  alpine-testing      0/1       CrashLoopBackOff   5          3m
  ...
  ```
  That means there was possibly defined a `command` which is executed in the container
  and the container exits immediatelly after start up
  A good thing is to check what `oc describe pod <name>` will talk about e.g. 'command' etc.
* if you want to check something from inside your cluster you can run a test docker fedora image like
  `oc run -i --tty fedora-testing --image=fedora --restart=Never -- bash`
  (https://kubernetes.io/blog/2015/10/some-things-you-didnt-know-about-kubectl_28/)
* find out ip address to DNS record: `getent hosts openshift.default.svc`
* error
  ```
  WARN  [org.jgroups.protocols.openshift.KUBE_PING] (ServerService Thread Pool -- 69) Problem getting Pod json from Kubernetes Client[masterUrl=https://172.30.0.1:443/api/v1, headers={}, connectTimeout=5000, readTimeout=30000, operationAttempts=3, operationSleep=1000, streamProvider=org.openshift.ping.common.stream.TokenStreamProvider@13e58e86] for cluster [ee], namespace [eap-transactions], labels [app=tx-server]; encountered [java.lang.Exception: 3 attempt(s) with a 1000ms sleep to execute [OpenStream] failed. Last failure was [java.io.IOException: Server returned HTTP response code: 403 for URL: https://172.30.0.1:443/api/v1/namespaces/eap-transactions/pods?labelSelector=app%3Dtx-server]]
  ```
  could mean there is no `view` RBAC permission for the user to list pods (see http://blog.chalda.cz/2018/02/28/Querying-Open-Shift-API.html)
  The fast way to fix it is to permit the default system user being a viewer
  ```
  oc login -u system:admin
  oc policy add-role-to-user view -z default
  ```
  or the shortcut as:  oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
* to check what is the cluster view 
  ```
  oc get pods
  oc rsh tx-server-###
    /opt/eap/bin/jboss-cli.sh -c 
    /subsystem=jgroups/channel=ee:read-resource(include-runtime=true) #, recursive=true
    /subsystem=jgroups/channel=ee/protocol=openshift.KUBE_PING:read-resource(include-runtime=true)
  ```
* if your run the web application in needs to be defined as `<distributable/>` in web.xml
* the `@org.jboss.ejb3.annotation.Clustered` annotation is in `org.jboss.ejb3:jboss-ejb3-ext-api:2.2.0.Final`
* cluster nodes start to try to communicate with each other only when it's deployed application
  which requires clustering. Or if there is some settings requiring it - for example HA for messaging or similar.
* clustering for remote ejb calls described somehow here: http://www.mastertheboss.com/jboss-server/jboss-cluster/ejb-to-ejb-communication-in-a-cluster-jboss-as-7-wildfly
* to run the `tx-client` copy the `standalone-client.xml` to the `$JBOSS_HOME/standalone/configuration` and start like
  `./bin/standalone.sh -c standalone-client.xml  -Djboss.socket.binding.port-offset=200 -Dtx.server.host=localhost`
* setup logging for the quickstarts to show more info for all servers
  `for I in 9990 10090 10190; do ./bin/jboss-cli.sh -c --controller=localhost:$I '/subsystem=logging/logger=org.jboss.as.quickstarts:add(level=TRACE)'; done`
* running curl on the ejb endpoint `curl -XGET localhost:8280/tx-client/api/ejb/stateless/arg` to see what the application does
* now about configuration. For the things to work the tx-client needs to define 'standalone.xml' remote outbound connection to one from the servers
```
  <security-realm name="ejb-security-realm">
      <server-identities>
          <secret value="dGVzdA=="/>
      </server-identities>
  </security-realm>
  ...
  <outbound-connections>
    <remote-outbound-connection name="remote-ejb-connection" outbound-socket-binding-ref="remote-ejb" username="ejb"
                                security-realm="ejb-security-realm" protocol="http-remoting">
        <properties>
            <property name="SASL_POLICY_NOANONYMOUS" value="false"/>
            <property name="SSL_ENABLED" value="false"/>
        </properties>
    </remote-outbound-connection>
  </outbound-connections>
  ...
  <outbound-socket-binding name="remote-ejb">
    <remote-destination host="${tx.server.host}" port="8080"/>
  </outbound-socket-binding>
```
and configure the `jboss-ejb-client.xml` descriptor with that
```
<!-- see https://docs.jboss.org/author/display/AS72/EJB+invocations+from+a+remote+server+instance -->
<jboss-ejb-client xmlns="urn:jboss:ejb-client:1.0">
    <client-context>
        <ejb-receivers>
            <remoting-ejb-receiver outbound-connection-ref="remote-ejb-connection"/>
        </ejb-receivers>

        <clusters>
	      <cluster name="ejb" security-realm="ejb-security-realm" username="ejb">
	        <connection-creation-options>
		        <property name="org.xnio.Options.SSL_ENABLED" value="false" />
		        <property name="org.xnio.Options.SASL_POLICY_NOANONYMOUS" value="false" />
	        </connection-creation-options>
	      </cluster>
	    </clusters>
    </client-context>
</jboss-ejb-client>
```
* to setup all beans are clustered then use `jboss-ejb3.xml` descriptor
```
<?xml version="1.0" encoding="UTF-8"?>
<jboss:ejb-jar xmlns:jboss="http://www.jboss.com/xml/ns/javaee" xmlns="http://java.sun.com/xml/ns/javaee"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:s="urn:security" xmlns:c="urn:clustering:1.0"
               xsi:schemaLocation="http://www.jboss.com/xml/ns/javaee http://www.jboss.org/j2ee/schema/jboss-ejb3-2_0.xsd
                     http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd"
               version="3.1" impl-version="2.0">
    <enterprise-beans>
    </enterprise-beans>

    <assembly-descriptor>
        <!-- mark all EJB's of the application as clustered (without using the jboss specific @Clustered annotation for each class) -->
        <c:clustering>
            <ejb-name>*</ejb-name>
            <c:clustered>true</c:clustered>
        </c:clustering>
    </assembly-descriptor>
</jboss:ejb-jar>
```
* you can define multiple remote outbound connections and declare them in the `jboss-ejb-client.xml`. Then if server fails the client tries another server to connect.
  There is no loadbalancing as in case of the cluster ejb.

## DevOps
```
cp tx-server/target/tx-server.war ~/tmp/wfly-server1/standalone/deployments/;cp tx-server/target/tx-server.war ~/tmp/wfly-server2/standalone/deployments/;cp tx-client/target/tx-client.war ~/tmp/wfly-client/standalone/deployments
```

To set-up a logging to see information about the quickstart processing and the remoting processing

```
for I in `oc get pods | grep Running | grep 'tx-server' | awk '{print $1}'`; do
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.as.quickstarts:add(level=TRACE)'
done
```
and

```
for I in `oc get pods | grep Running | grep 'tx-client' | awk '{print $1}'`; do
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.as.quickstarts:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.ejb.client:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.as.remoting:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.remoting3:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.ejb.protocol.remote:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jgroups.protocols.kubernetes:add(level=TRACE)'&
done
```

### TODO - configure kube ping clustering

```
oc set env dc/tx-client OPENSHIFT_KUBE_PING_LABELS='app=tx-client' OPENSHIFT_KUBE_PING_NAMESPACE='eap-transactions'
oc set env dc/tx-server OPENSHIFT_KUBE_PING_LABELS='app=tx-server' OPENSHIFT_KUBE_PING_NAMESPACE='eap-transactions'
```

How to create cluster selector for ejb client

* http://git.app.eng.bos.redhat.com/git/jbossqe/eap-tests-ejb.git/tree/ejb-multi-server-ts/src/test/java/org/jboss/qa/ejb/tests/clusternodeselector/ClusterNodeSelectorTestCase.java#n98
* http://git.app.eng.bos.redhat.com/git/jbossqe/eap-tests-ejb.git/tree/ejb-multi-server-ts/src/test/java/org/jboss/qa/ejb/tests/clusternodeselector/CustomClusterNodeSelector.java
* cluster node selector in `jboss-ejb-client.xml` : https://developer.jboss.org/thread/198898
