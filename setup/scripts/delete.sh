kubectl delete -f ~/k8s-manifests/redis/redis-cluster.yml
kubectl delete pvc data-redis-cluster-0 data-redis-cluster-1 data-redis-cluster-2 data-redis-cluster-3 data-redis-cluster-4 data-redis-cluster-5
kubectl delete -f ~/k8s-manifests/redis/cm.yml
kubectl delete -f ~/k8s-manifests/namespace/namespace.yml