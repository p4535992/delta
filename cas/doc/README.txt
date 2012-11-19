
Temporary fix for building - add the following line to hosts file:
89.167.251.252 repository.jboss.com
Maybe it is resolved in newer CAS version?

Build:
	mvn clean package
And result is target/cas.war

Check if there are newer versions of dependencies available:
	mvn versions:display-dependency-updates
