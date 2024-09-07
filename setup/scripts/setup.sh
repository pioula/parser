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
sudo apt update
sudo apt -y install ansible sshpass

# Use the provided username and password in the loop
for i in $(seq -w 01 10); do
    sshpass -p "$password" ssh "$username@${username}vm1$i.rtb-lab.pl" -o StrictHostKeyChecking=no -C "/bin/true"
done

ansible-galaxy collection install kubernetes.core
ansible-galaxy collection install community.kubernetes
ansible-galaxy collection install cloud.common

sudo apt install python3-pip
pip install kubernetes

echo "Installation Ansible successful!"
echo "Configuring kubernetes cluster"

cp -r ../ansible/ ~/
ansible-playbook ~/ansible/playbook/kube_dependencies.yml -i ~/ansible/inventory/kube_inventory
ansible-playbook ~/ansible/playbook/kube_master.yml -i ~/ansible/inventory/kube_inventory
ansible-playbook ~/ansible/playbook/kube_workers.yml -i ~/ansible/inventory/kube_inventory