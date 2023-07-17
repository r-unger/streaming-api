#!/bin/bash

#SSHSRV=prdmain2
#SSHSRV=prdmain1
	# all xxxmain servers work
	# which one gets used, depends on the client and the DNS names
SSHSRV=prdcld01

APISRV=usdp.at

WARDIR=./build/libs
WARFILE=streaming-api-v1.war

gradle clean build

if [ ! -f $WARDIR/$WARFILE ]; then
   echo "The file '$WARFILE' was not generated. IMPT: Are the 2 lines of build.gradle active (not commented out)?"
   exit
fi

echo "'$WARFILE' successfully created"
ls -l $WARDIR/$WARFILE
cp $WARDIR/$WARFILE ~/projects/devops/vagrant/multisrv2/puppet/environments/production/modules/msrv_tomcat_new/files/webapps/
ls -l ~/projects/devops/vagrant/multisrv2/puppet/environments/production/modules/msrv_tomcat_new/files/webapps/*.war
echo "(Compare these files)"

echo "Running a puppet apply for $SSHSRV ..."
~/projects/devops/vagrant/multisrv2/puppet-apply.sh $SSHSRV

# should be done automatically
# echo "Forcing tomcat to update the webapp ..."
# ssh $SSHSRV 'sudo rm -rdf /var/lib/tomcat/webapps/streaming-api-v1/ && sudo service tomcat restart'

echo "Now ping the API with:"
echo "curl https://$APISRV/streaming-api-v1/employees | json_pp"
echo "or:"
echo "curl -X POST -H 'Content-Type: application/json' -d '{}' https://$APISRV/streaming-api-v1/serverspots?group=live.eftv"
