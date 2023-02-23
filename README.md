Prerequisite: JRE 1.8

How to run locally: mvn spring-boot:run

To build : mvn assembly:assembly -DskipTests=true
It generates a jar file in /target.

Run the Jar file : 
Command: java -jar demo-0.0.1-SNAPSHOT.jar --spring.config.location=file:///{your application.properties config file location}

This starts the Backend service in 8080 port.

To verify the process: lsof -i:8080

To stop: kill -9 processId


==========Sample application.properties file===
server.port=8080
remote.server.hostname= 10.170.16.195
remote.server.user= ipbx
remote.server.password= Ub1Hd1!
