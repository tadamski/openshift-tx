#!/bin/bash
set -e

# Project where the test will be run has to be prepared
# oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"

# Importing image of EAP 7.1 where deploy will be run
# oc import-image jboss-eap-71 --from=registry.access.redhat.com/jboss-eap-7/eap71-openshift --confirm

# Updating the remote github repository to be up to date. If you haven't forked it just skip this.
# git add -u && git commit -m "config" && git push origin master

# Cleaning OpenShift namespace from the older attempts
oc delete is,bc,dc,service tx-server
# New application which clones the github repository and deploy the build to EAP 7.1
oc new-app jboss-eap-71~https://github.com/ochaloup/openshift-tx.git#eap71 --context-dir='tx-server' --name='tx-server' --labels name='tx-server'
# The command starts clonging from github and building process with maven
# to check how that goes check running builds with
#  oc get build
# and the logs from the building process with 
#  oc logs build/tx-server-# -f 
# to verify if the pod with the application is already running
#  oc rsh `oc get pods | grep Running | awk '{print $1}'`

# (optional) if you want to access the ejbs on tx-server directly from space out of the OpenShift
# oc expose service tx-server

# To refresh the state of the service - aka. scaledown the current docker image and start a new one
#   oc rollout latest tx-server
# but the last build is used. If there were changes on github you need build new one first
#   oc start-build tx-server

# To get logger output at tx-server from the enlisted XA resource the easiest option is to
#  log to the pod and switch the logging category
#  oc get pods
#  oc rsh tx-server-###
#   /opt/eap/bin/jboss-cli.sh -c
#    /subsystem=logging/logger=org.jboss.as.quickstarts:add(level=TRACE)

