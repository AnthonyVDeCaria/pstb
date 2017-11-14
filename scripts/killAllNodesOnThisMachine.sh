#!/bin/bash

echo "Killing all clients"
pkill -f PhysicalClient
echo "Killing all brokers"
pkill -f PhysicalBroker

