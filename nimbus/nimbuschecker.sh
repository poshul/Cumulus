#!/bin/bash
#this script is intended be places in /etc/cron.d/ and run from the cron table

statusCommand="/etc/init.d/nimbus status"  #the command to get the status of the nimbus daemon

status=$($statusCommand)
if [ "$status" = "stopped" ];
then echo "now we would shutdown";
else echo $status
fi
exit 0