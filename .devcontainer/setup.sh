#!/bin/bash
set -e

echo "Setting workspace permissions..."
sudo chown -R $(id -u):$(id -g) /workspaces

echo "Updating package lists..."
sudo apt-get update

echo "Installing development tools..."
sudo apt-get install -y apache2-utils jq

#sudo systemctl enable postgresql
#sudo systemctl start postgresql

echo "Development environment setup complete!"
