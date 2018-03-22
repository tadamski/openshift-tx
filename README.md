eap64

# Starting minishift

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

# create new project
 oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"

## Importing EAP images
 oc import-image jboss-eap-70 --from=registry.access.redhat.com/jboss-eap-7/eap70-openshift --confirm
 oc import-image jboss-eap-71 --from=registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm
 oc import-image jboss-eap-64 --from=registry.access.redhat.com/jboss-eap-6/eap64-openshift --confirm

# create and deploy server
cat server.sh

# create and deploy client
cat client.sh

# trigger an ejb call
curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateful/arg'
curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateless/arg'

# Troubleshooting
oc build-logs tx-client-1
# oc login -u system:admin 
oc rsh `oc get pods -n tx-client | grep Running | awk '{print $1}'
CLI reference: https://docs.openshift.com/enterprise/3.0/cli_reference/basic_cli_operations.html

# adding a user

Create a user called ejbuser that will be used for securing remote EJB calls:

Adding a user using the add-user.sh command generates an application-users.properties file which
needs be added to the S2I builds configuration directory.
It also also generates the secret that that needs to be placed in the client servers config file:

> To represent the user add the following to the server-identities definition <secret value="dGVzdDEyMzQh" />

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
To represent the user add the following to the server-identities definition <secret value="dGVzdDEyMzQh" />
