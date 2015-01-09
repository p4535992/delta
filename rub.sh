# JDK 1.6 x64
#export JAVA_HOME=/usr/lib/jvm/jdk1.6.0_45
# JDK 1.7 x64
#export JAVA_HOME=/usr/lib/jvm/java-7-oracle
export JAVA_HOME=/usr/lib/jvm/jdk1.7.0_71
# JDK 1.8.0_20 x64
#export JAVA_HOME=/usr/lib/jvm/jdk1.8.0_20
export ANT_HOME=/home/aare/soft/apache-ant-1.9.4
export PATH=$PATH:$JAVA_HOME/bin
export PATH=$PATH:$ANT_HOME/bin
export ANT_OPTS="-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1"

# ant clean-all war -Dconf.name=smit-test -Dconf.organization.name=default -Dappserver=tomcat
# ant clean-all war -Dconf.name=smit-test -Dconf.organization.name=ppa -Dappserver=tomcat
# ant clean-all war -Dconf.name=jum-example -Dconf.organization.name=jum -Dappserver=tomcat
# ant clean-all war -Dconf.name=sim-example -Dconf.organization.name=ppa -Dappserver=tomcat > delta.build.log


