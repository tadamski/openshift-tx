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
oc new-app jboss-eap-71~https://github.com/mmusgrov/openshift-tx.git#eap71 --context-dir='tx-server' --name='tx-server' --labels name='tx-server'

# (optional) if you want to access the ejbs on tx-server directly from space out of the OpenShift
# oc expose service tx-server

# Verification of how the build and deploy go on
## to track the progress of build you can use switch '-f' as 'oc logs -f ...'
# oc logs build/tx-server-1
# oc rsh `oc get pods | grep Running | awk '{print $1}'`
