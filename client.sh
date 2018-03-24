#!/bin/bash
set -e

# git add -u && git commit -m "config" && git push origin master

oc delete is,bc,dc,service tx-client 
oc new-app jboss-eap-70~https://github.com/mmusgrov/openshift-tx.git#eap70 --context-dir='tx-client' --name='tx-client' --labels name='tx-client'

oc env dc/tx-client JAVA_OPTS_APPEND="-Dtx.server.host=tx-server.eap-transactions.svc.cluster.local"

# oc expose service tx-client && oc rollout latest tx-client

# curl -XGET 'http://tx-client-eap-transactions.192.168.99.100.nip.io/tx-client/api/ejb/stateless/arg'
