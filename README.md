# Setup for EJB TXN remoting with StatefulSet with EAP 7.2

# How to run

```
# new project
oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"
# creating image streams
oc create -f ./eap72-image-stream.json
# statefulset template creation
oc create -f ./eap72-stateful-set.json
# startup tx-server service
oc new-app --template=eap72-stateful-set -p APPLICATION_NAME=tx-server -p ARTIFACT_DIR=tx-server/target
# startup tx-client service
oc new-app --template=eap72-stateful-set -p APPLICATION_NAME=tx-client -p ARTIFACT_DIR=tx-client/target
```

## How to invoke particular "test cases"

Check the endpoints at java class [EJBTestCallerRestEndpoints.java](tx-client/src/main/java/org/jboss/as/quickstarts/xa/client/EJBTestCallerRestEndpoints.java)

```
# stateless bean without failures using `UserTransaction` to begin transaction
# running two invocations - one is non-txn the other is with transaction started
curl -XGET "http://tx-client-`oc project -q`.`minishift ip`.nip.io/tx-client/api/ejb/stateless-pass"

# stateless bean which crashes tx-server at the end of the business method (see StatelessBeanKillJVMBusiness.java)
curl -XGET "http://tx-client-`oc project -q`.`minishift ip`.nip.io/tx-client/api/ejb/statless-jvm-halt-business"

# stateless bean which crashes tx-server at XAResource.commit (see StatelessBeanKillOnCommit.java)
curl -XGET "http://tx-client-`oc project -q`.`minishift ip`.nip.io/tx-client/api/ejb/stateless-jvm-halt-on-commit-server"

# stateless bean which crashes tx-server at XAResource.prepare (see StatelessBeanKillOnPrepare.java)
curl -XGET "http://tx-client-`oc project -q`.`minishift ip`.nip.io/tx-client/api/ejb/stateless-jvm-halt-on-prepare-server"
```

## Changes in WildFly/EAP configuration

The standalone-openshift.xml configuration is based on the EAP72 configuration
available at https://github.com/jboss-container-images/jboss-eap-modules/blob/master/jboss-eap72-openshift/added/standalone-openshift.xml.

The configuration changes that are needed for the EJB remoting works correctly are:

* setting up [remote outbound connection](https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.0/html/configuration_guide/configuring_remoting#remoting_remote_outbound_connection), see [standalone-openshift.xml](https://github.com/tadamski/openshift-tx/blob/e78a49b8e6e2461c7a1d61aab60aa6e3db1d1c35/tx-client/configuration/standalone-openshift.xml#L507)
* do not use `bindall` as there is know bug in clustering/remoting, see https://issues.jboss.org/browse/JBEAP-15874
* configuration of security needs to be defined in by property `-Dwildfly.config.url` with definition that [custom-config.xml](tx-client/configuration/custom-config.xml), setup in [OpenShift template](https://github.com/tadamski/openshift-tx/blob/e78a49b8e6e2461c7a1d61aab60aa6e3db1d1c35/eap72-stateful-set.json#L452), see https://issues.jboss.org/browse/JBEAP-15738
* the _tx-server_ defines `client-mapping` (`<client-mapping destination-address="${jboss.node.name}.tx-server"/>`) for the http `socket-binding`, see https://issues.jboss.org/browse/JBEAP-16420
* TODO: issue on programmatic authentication is here https://issues.jboss.org/browse/JBEAP-16149

## Notes on "How to run"

* if you want to run from different git repo and branch
```
oc new-app --template=eap72-stateful-set -p APPLICATION_NAME=tx-client -p ARTIFACT_DIR=tx-client/target -p SOURCE_REPOSITORY_URL=https://github.com/ochaloup/openshift-tx.git -p SOURCE_REPOSITORY_REF=master
```

* for scaling
```
# to scale down or up the statefulset
oc scale sts tx-server --replicas=0
oc scale sts tx-client --replicas=0
```

* to delete the namespace created

```
oc delete all --all; oc delete $(oc get pvc -o name); oc delete template eap72-stateful-set
```

* For changing configuration you can create file `configuration/wfly-init.script`
which defines WildFly CLI commands. They will be executed just before the WildFly pod is started.
This functionality is configured in the template `json` as `PostStart` hook.

# How to debug

Resources about OpenShift Java debudding at
https://blog.openshift.com/debugging-java-applications-on-openshift-kubernetes/

```
# check if there is route to tx-server and in case create new one
oc get route
oc expose service tx-server
# set the DEBUG env variable which causes the EAP is started with debug port opened
# for changing the opened debug port (in container!) you can use DEBUG_PORT
oc set env dc/tx-server DEBUG=true # Enable the debug port
# forward the opended debug port to the hosting machine
oc port-forward tx-server-0 8787:8787 &
```


## Appendix 1: Starting minishift

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

## Appendix 2: Useful command and snippets

* when the project/namespace is cleaned with `oc delete` commands it could be helpful to just start
all the building. These lines could help with that

```
oc create -f eap72-image-stream.json
oc create -f eap72-stateful-set.json
REF=tadamski-master-unchanged-my-changes
REPO=ochaloup
oc new-app --template=eap72-stateful-set -p APPLICATION_NAME=tx-client -p ARTIFACT_DIR=tx-client/target -p SOURCE_REPOSITORY_URL=https://github.com/${REPO}/openshift-tx.git -p SOURCE_REPOSITORY_REF=$REF
oc new-app --template=eap72-stateful-set -p APPLICATION_NAME=tx-server -p ARTIFACT_DIR=tx-server/target -p SOURCE_REPOSITORY_URL=https://github.com/${REPO}/openshift-tx.git -p SOURCE_REPOSITORY_REF=$REF
sleep 10; oc logs -f bc/tx-client
```

* To build the code at the local machine and load the build to OpenShift, see
  https://docs.openshift.com/container-platform/3.6/dev_guide/dev_tutorials/binary_builds.html#binary-builds-local-code-changes.
  When build is done (`oc get bc`) the StatefulSet does not redeploy automatically (TODO: not sure if it could be configured somewhere).

  The template defines the `BuildConfig` with source strategy `Git`. If the `start-build` is invoked then
  [_it is dynamically disabled, since Binary and Git are mutually exclusive, and the data in the binary stream provided to the builder takes precedence_](https://docs.okd.io/latest/dev_guide/builds/build_inputs.html#binary-source).
  It means that `BuildConfig` defined by template is change to be `Binary` when `start-build` is used.

```
cd openshift-tx
# to build tx-client `bc/tx-client`
oc start-build tx-client --from-dir="." --follow
# to build tx-server `bc/tx-server`
oc start-build tx-server --from-dir="." --follow
```

* If we work with the build - for example changing `.s2i/bin/assemble` script it's
  needed to create a new build to accommodate such changes. We can delete
  the whole build and create a new one with the `oc new-build` command.
  We define to be `Binary` which means no build is done automatically
  and what is about to be build is specified by `start-build`.

```
oc delete bc tx-client
oc new-build --image-stream=eap-transactions/jboss-eap72-openshift:latest --to=tx-client:latest\
  -eARTIFACT_DIR=tx-client/target -eMAVEN_ARGS_APPEND='-Dcom.redhat.xpaas.repo.jbossorg' --binary=true
oc start-build tx-client --from-dir="." --follow
```


* To run the new build (if there is some, see above) you need to scale down and up
  the StatefulSet. This oneliner could help with that

```
APP=tx-client
oc scale sts $APP --replicas=0; while `oc get pods | grep -q $APP-0`;\
  do echo "sleeping one second"; sleep 1; done; echo done; oc scale sts $APP --replicas=1
```

* Forcing periodic recovery to be executed

```
RECOVERY_FOR_POD=tx-client-0
oc rsh $RECOVERY_FOR_POD java -cp /opt/eap/modules/system/layers/base/org/jboss/jts/main/narayana-jts-idlj-5.9.0.Final-redhat-00001.jar com.arjuna.ats.arjuna.tools.RecoveryMonitor -host $RECOVERY_FOR_POD -port 4712 -timeout 1800000
```

* Setting `com.arjuna` for `TRACE` logging level for all active pods

```
for I in `oc get pods | grep Running | grep -E '(tx-server)|(tx-client)' | awk '{print $1}'`; do
  echo "Changing log category 'com.arjuna' to level TRACE on '$I'"
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=com.arjuna:write-attribute(name=level,value=TRACE)'
done
```

or

```
for I in `oc get pods | grep Running | grep 'tx-client' | awk '{print $1}'`; do
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=com.arjuna:write-attribute(name=level,value=TRACE)'
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.as.quickstarts:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.ejb.client:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.as.remoting:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.remoting3:add(level=TRACE)'&
  oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jboss.ejb.protocol.remote:add(level=TRACE)'&
  # oc rsh $I /opt/eap/bin/jboss-cli.sh -c '/subsystem=logging/logger=org.jgroups.protocols.kubernetes:add(level=TRACE)'&
done
```

* Lookup for pods by selector and select their names (see http://blog.chalda.cz/2018/02/28/Querying-Open-Shift-API.html)
  `curl -k   -H "Authorization: Bearer $TOKEN"   -H 'Accept: application/json'   https://${ENDPOINT}/api/v1/namespaces/$NAMESPACE/pods?labelSelector=app%3Dtx-server | jq '.items[].metadata.name'`


* Checking something from inside of the cluster you can run a test docker fedora image like
  `oc run -i --tty fedora-testing --image=fedora --restart=Never -- bash`
  (https://kubernetes.io/blog/2015/10/some-things-you-didnt-know-about-kubectl_28/)
* find out ip address to DNS record: `getent hosts openshift.default.svc`


* RBAC permission to add the default service account to view all in the namespace
  `oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)`

* Troubles with building s2i. Run the s2i localy to see what's happen
  `s2i build ./ registry.access.redhat.com/jboss-eap-7/eap72-openshift:latest  local/testing-tx-client`

### Appendix 3: Outbound connection standalone.xml changes

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

* to setup the authentication for JTA as workaround for the Elytron JTA remoting authentication issue. You need to define `custom-config.xml`
  which is provided with the system property `wildfly.config.url` (see [eap72-stateful-set.json](eap72-stateful-set.json#L439) )
```
<configuration>
    <authentication-client xmlns="urn:elytron:1.0">
	<authentication-rules>
            <rule use-configuration="jta">
                <match-abstract-type name="jta" authority="jboss"/>
	    </rule>
        </authentication-rules>
        <authentication-configurations>
	     <configuration name="jta">
                 <sasl-mechanism-selector selector="DIGEST-MD5"/>
                 <providers>
                     <use-service-loader />
	         </providers>
		 <set-user-name name="ejb"/>
	         <credentials>
                      <clear-password password="ejb"/>
	         </credentials>
                 <set-mechanism-realm name="ApplicationRealm" />
             </configuration>
        </authentication-configurations>
    </authentication-client>
</configuration>
```

* if you run the queries on the HTTP endpoints of the WFLY container to get status etc.
  (runnig like this: `curl  --digest -XGET  http://localhost:9990/management?operation=attribute\&name=server-state`)
  and you don't want to care about authentication you need to have defined `<http-interface console-enabled="false">`
  and not `<http-interface security-realm="ManagementRealm">`
