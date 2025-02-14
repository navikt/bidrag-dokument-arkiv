#!/bin/bash
kubectx dev-fss

deployment="deployment/bidrag-dokument-arkiv"
[ "$1" == "q1" ] && deployment="deployment/bidrag-dokument-arkiv-feature"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|SRV|TOPIC|KAFKA' | grep -v -e 'NAIS_APP_NAME' -e '_PATH' > src/test/resources/application-lokal-nais-secrets.properties