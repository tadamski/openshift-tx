#!/bin/bash
set -e

# Updating the remote github repository to be up to date. If you haven't forked it just skip this.
# git add -u && git commit -m "config" && git push origin master

# Cleaning OpenShift namespace from the older attempts
oc delete is,bc,dc,service tx-client 
# New application which clones the github repository and deploy the build to EAP 7.1
#  passing environment variable which is attached to any starting Java program aka. to starting JBoss EAP server
#  the value of the 'tx.server.host' is used to determine the address where client connects to
oc new-app jboss-eap-71~https://github.com/mmusgrov/openshift-tx.git#eap71 --context-dir='tx-client' --name='tx-client' --labels name='tx-client'\
  -e JAVA_OPTS_APPEND='-Dtx.server.host=tx-server.eap-transactions.svc.cluster.local'
# Environment variable can be added/changed later too
# oc env dc/tx-client JAVA_OPTS_APPEND="-Dtx.server.host=tx-server.eap-transactions.svc.cluster.local"

# Exposing client as service is necessary for being able to invoke the EJB calling server from outside of the OpenShift
# oc expose service tx-client
# Refreshing state of the service could be useful time to time
# oc rollout latest tx-client

# Invoking call to the client service which then calls the server
#   check the particular address with command `oc get route`
# curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateless/arg'
##
TODO!!
# After calling curl you should get response 'success', try to scale down the tx-server
# oc scale dc tx-server --replicas=0
# and the curl returns 
