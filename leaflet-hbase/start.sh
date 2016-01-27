cd /root/leaflet-hbase/leaflet-maven
##mvn clean package
classpath=target/leaflet-maven-0.0.1-SNAPSHOT.jar:`cat classpath`:/usr/hdp/current/phoenix-client/phoenix-client.jar
##java -cp $classpath com.my.leaflet.prepare.PhoenixTable
java -cp $classpath com.my.leaflet.MyApplication

