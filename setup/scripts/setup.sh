# Files to change
# - kube_inventory st115 -> your_user_name
# - kube_master -> st115 -> your_user_name
# - kube_workers -> st115 -> your_user_name
# on vm110
#!/bin/bash

# Ask for username and password
read -p "Enter your username: " username
read -s -p "Enter your password: " password
echo

echo "Installing Ansible"
sudo apt update && sudo apt upgrade -y
sudo apt install -y python3-pip
sudo apt -y install ansible sshpass

#Download Kubernetes Tools using Curl:
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

#Verify Checksum (Response should be kubectl:OK): 
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"

echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check

#Install Kubernetes Tools: 
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Use the provided username and password in the loop
for i in $(seq -w 01 10); do
    sshpass -p "$password" ssh "$username@${username}vm1$i.rtb-lab.pl" -o StrictHostKeyChecking=no -C "/bin/true"
done

ansible-galaxy collection install kubernetes.core
ansible-galaxy collection install community.kubernetes
ansible-galaxy collection install cloud.common

sudo apt -y install python3-pip
pip install kubernetes

echo "Installation Ansible successful!"
echo "Configuring kubernetes cluster"

cp -r ../ansible/ ~/
ansible-playbook --extra-vars "ansible_user=$username ansible_password=$password" ~/ansible/playbook/kube_dependencies.yml -i ~/ansible/inventory/kube_inventory
ansible-playbook --extra-vars "ansible_user=$username ansible_password=$password" ~/ansible/playbook/kube_master.yml -i ~/ansible/inventory/kube_inventory
ansible-playbook --extra-vars "ansible_user=$username ansible_password=$password" ~/ansible/playbook/kube_workers.yml -i ~/ansible/inventory/kube_inventory

echo "Configuration of the cluster complete!"
echo "Creating kubernetes resources:"
cp -r ../k8s-manifests ~/

mkdir ~/.kube
sudo sshpass -p $password ssh -o "StrictHostKeyChecking=no" "$username@${username}vm102.rtb-lab.pl" "sudo cat /etc/kubernetes/admin.conf" > ~/.kube/config

ansible-playbook --extra-vars "ansible_user=$username ansible_password=$password" ~/ansible/playbook/create_k8s_resources.yml -i ~/ansible/inventory/kube_inventory