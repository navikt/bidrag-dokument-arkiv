kubectx dev-fss
kubectl exec --tty deployment/bidrag-dokument-arkiv printenv | grep -E 'AZURE_|_URL|SCOPE|SRV|TOPIC|KAFKA' | grep -v -e 'NAIS_APP_NAME' -e '_PATH' > src/test/resources/application-lokal-nais-secrets.properties
