#!/bin/bash
set -e

# oc new-project eap-transactions --display-name="JBoss EAP Transactional EJB"
# oc import-image jboss-eap-64 --from=registry.access.redhat.com/jboss-eap-6/eap64-openshift --confirm
# git add -u && git commit -m "config" && git push origin master

oc delete is,bc,dc,service tx-server 
oc new-app jboss-eap-64~https://github.com/mmusgrov/openshift-tx.git#eap64 --context-dir='tx-server' --name='tx-server' --labels name='tx-server'

# debug EAP boot
oc set env dc/tx-server DEBUG=true
oc get pod --all-namespaces
oc env dc/tx-server JAVA_OPTS_APPEND="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"
oc port-forward tx-server-4-smhrj 8787:8787 &


# add the remoting port 4447 to the ports section in the dc and service yaml config:
# oc edit dc/tx-server && oc edit svc/tx-server
# oc expose service tx-server && oc rollout latest tx-server

# oc build-logs tx-client-1
# oc rsh `oc get pods -n tx-client | grep Running | awk '{print $1}'`
# use the minishift console to add health probe
