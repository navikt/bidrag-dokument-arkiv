#echo kubectl get pods -o json | jq '.items[].spec.containers[].env[]?.valueFrom.secretKeyRef.name' | grep -v null | sort | uniq | grep bidrag-dokument-arkiv

mkdir src/test/resources/kafka
cd src/test/resources/kafka
SECRET_NAME=aiven-bidrag-dokument-arkiv-4334638c-2023-35
kubectl get secret $SECRET_NAME -o 'go-template={{index .data "client.keystore.p12" | base64decode}}' > client.keystore.p12
kubectl get secret $SECRET_NAME -o 'go-template={{index .data "client.truststore.jks" | base64decode}}' > client.truststore.jks
kubectl get secret $SECRET_NAME -o 'go-template={{index .data "KAFKA_PRIVATE_KEY"}}' > kafka-private-key.pem
kubectl get secret $SECRET_NAME -o 'go-template={{index .data "KAFKA_CERTIFICATE"}}' > kafka.certificate.crt
kubectl get secret $SECRET_NAME -o 'go-template={{index .data "KAFKA_CA"}}' > kafka-ca.pem

#base64 --decode -i client.truststore.txt -o client.truststore.jks
#base64 --decode -i client.keystore.txt -o client.keystore.p12