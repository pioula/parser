#!/bin/bash

# Get Redis node IPs
export REDIS_NODES=$(kubectl get pods -l app=redis-cluster -o json | jq -r '.items | map(.status.podIP) | join(":6379 ")'):6379

# Activate the Redis cluster
kubectl exec -it redis-cluster-0 -- redis-cli --cluster create --cluster-yes --cluster-replicas 1 ${REDIS_NODES}

# Check if all went well
for x in $(seq 0 5); do
    echo "redis-cluster-$x"
    kubectl exec redis-cluster-$x -- redis-cli role
    echo
done