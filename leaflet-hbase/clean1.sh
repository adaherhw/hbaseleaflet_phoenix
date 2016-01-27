### Clean Hbase from PHOENIX 
cd /usr/hdp/current/phoenix-client/bin
./sqlline.py sandbox.hortonworks.com:2181:/hbase-unsecure
!tables
drop table LEAFLET.TRIP;
### Clean from HBase
hbase shell
disable "LEAFLET.TRIP"
drop "LEAFLET.TRIP"

