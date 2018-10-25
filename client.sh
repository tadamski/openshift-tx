#!/bin/bash
set -e

# ======================================
#  start with the commands in server.sh
# ======================================

# Updating the remote github repository to be up to date. If you haven't forked it just skip this.
# git add -u && git commit -m "config" && git push origin master

# Cleaning OpenShift namespace from the older attempts
oc delete is,bc,dc,service tx-client
# New application which clones the github repository and deploy the build to EAP 7.1
#  passing environment variable which is attached to any starting Java program aka. to starting JBoss EAP server
#  the value of the 'tx.server.host' is used to determine the address where client connects to
oc new-app jboss-eap-71~https://github.com/ochaloup/openshift-tx.git#eap71 --context-dir='tx-client' --name='tx-client' --labels name='tx-client'\
  -e JAVA_OPTS_APPEND='-Dtx.server.host=tx-server.eap-transactions.svc.cluster.local'
# Environment variable can be added/changed later too
# oc env dc/tx-client JAVA_OPTS_APPEND="-Dtx.server.host=tx-server.eap-transactions.svc.cluster.local"
# To check the build process by overviewing logs
#  oc get build
#  oc logs build/tx-client-# -f

# Exposing client as service is necessary for being able to invoke the EJB calling server from outside of the OpenShift
oc expose service tx-client

# Invoking call to the client service which then calls the server
#   check the particular address with command `oc get route`
curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateless/arg'

# To refresh the state of the service - aka. scaledown the current docker image and start a new one
#   oc rollout latest tx-client
# the last build is used. if there were some changes on github you need first run a new build
#   oc start-build tx-client -f
# if there were some changes then a new image is pushed to image stream and new deployment
#  should be exectued by default (aka. 'oc rollout' is not necessary)
# check the state of the imagestream related to the deployment via
#   oc describe is tx-client
# checking hitory of the building of the docker image for the service
#   oc describe build tx-client
# checking what data was used to start the particular pod
#   oc describe pod tx-client-###

##
TODO!!
# After calling curl you should get response 'success', try to scale down the tx-server
# oc scale dc tx-server --replicas=0
# and the curl returns
