#!/bin/bash
set -e

# oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"
# oc import-image jboss-eap-71 --from=registry.access.redhat.com/jboss-eap-6/eap71-openshift --confirm
# git add -u && git commit -m "config" && git push origin master

oc delete is,bc,dc,service tx-server 
oc new-app jboss-eap-71~https://github.com/mmusgrov/openshift-tx.git#eap71 --context-dir='tx-server' --name='tx-server' --labels name='tx-server'

# add the remoting port 4447 to the ports section in the dc and service yaml config:
# oc edit dc/tx-server && oc edit svc/tx-server
# oc expose service tx-server && oc rollout latest tx-server

# oc logs build/tx-client-1
# oc rsh `oc get pods -n tx-client | grep Running | awk '{print $1}'`
