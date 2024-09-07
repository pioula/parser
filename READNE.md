# Kafka

helm uninstall kafka oci://registry-1.docker.io/bitnamicharts/kafka

helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka --set listeners.client.protocol=PLAINTEXT --set listeners.controller.protocol=PLAINTEXT --set heapOpts="-Xmx512m -Xms512m -XX:MetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80 -XX:+ExplicitGCInvokesConcurrent"

kubectl run kafka-client --restart='Never' --image docker.io/bitnami/kafka:3.8.0-debian-12-r3 --namespace parser --command -- sleep infinity
kubectl exec --tty -i kafka-client --namespace parser -- bash

kafka-topics.sh --bootstrap-server kafka:9092 --create --topic usertagevents --partitions 3 --replication-factor 3

kafka-topics.sh --bootstrap-server kafka:9092 --list

# MYSQL
kubectl run --rm -it myshell --image=container-registry.oracle.com/mysql/community-operator -- mysqlsh