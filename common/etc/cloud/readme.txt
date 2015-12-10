Setup web server
---------------------
1) Launch a new instance on AWS EC2
1.1) Choose an instance type that has local instance storage (i.e. not "EBS only")
1.2) In Step 3: Configure Instance Details -> Advanced Details -> User Data enter the following:
nickname=a_good_name_for_web_server
alfresco-global.db.host=hostname_of_db_server
alfresco-global.foo=bar
     All lines beginning with alfresco-global will be merged to alfresco-global.properties
1.3) In Step 4: Add Storage attach "Instance Store 0" as /dev/sdb
2) After instance has launched, create a new volume for DHS data (completely new or from a snapshot)
   and attach it as /dev/sdf
3) If data volume is empty, then copy dhs.war and alfresco-global.properties (from etc/conf/wm-test-cloud/classes)
   to the instance with SFTP or SCP (to ec2-user's home folder). If data volume is not empty, then skip this step.
4) Copy all files from this folder to the instance with SFTP or SCP (to ec2-user's home folder)
5) SSH to the instance as ec2-user
6) Execute sudo bash setup-delta.sh

Setup database server
---------------------
