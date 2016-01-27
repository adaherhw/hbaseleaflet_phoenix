--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--
-- Part 1 create Hbase tables
-- Assuming 2 files in the following location:
-- 1. /mnt/hgfs/Programs/temp/hdp/sre_trip_info/
-- 2. /mnt/hgfs/Programs/temp/hdp/sre_trip_minute_aggregates/
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

------------
-- clean
drop table if exits alex_sr.sre_trip_info;
drop table if exits alex_sr.sre_trip_minute_aggregates;
drop table if exits alex_sr.sre_trip_info_hbase;
drop table if exits alex_sr.sre_trip_minute_aggregates_hbase;
-----------


create schema if not exists alex_sr;
use alex_sr;

create table alex_sr.sre_trip_info (
	trip_vin string,
	trip_number string,
	trip_start_timestamp string,
	trip_end_timestamp string,
	trip_start_day string,
	trip_start_hr string,
	trip_start_min string,
	trip_end_day string,
	trip_end_hr string,
	trip_end_min string,
	trip_duration bigint,
	trip_length_miles double,
	trip_length_kms double,
	trip_hard_break_count bigint,
	trip_fast_accel_count bigint,
	trip_night_time_drive_min double,
	trip_minute_details string
)
row format delimited fields terminated by ','
;

load data local inpath '/mnt/hgfs/Programs/temp/hdp/sre_trip_info/'
overwrite into table alex_sr.sre_trip_info;


create table alex_sr.sre_trip_info_hbase (
	trip_key string,
	trip_vin string,
	trip_number string,
	trip_start_timestamp string,
	trip_end_timestamp string,
	trip_start_day string,
	trip_start_hr string,
	trip_start_min string,
	trip_end_day string,
	trip_end_hr string,
	trip_end_min string,
	trip_duration bigint,
	trip_length_miles double,
	trip_length_kms double,
	trip_hard_break_count bigint,
	trip_fast_accel_count bigint,
	trip_night_time_drive_min double,
	trip_minute_details string
)
stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
with serdeproperties ("hbase.columns.mapping" = ":key,trip_summary:trip_vin,trip_summary:trip_number,trip_summary:trip_start_timestamp,trip_summary:trip_end_timestamp,trip_summary:trip_start_day,trip_summary:trip_start_hr,trip_summary:trip_start_min,trip_summary:trip_end_day,trip_summary:trip_end_hr,trip_summary:trip_end_min,trip_summary:trip_duration,trip_summary:miles_driven,trip_summary:trip_length_kms,trip_summary:hard_break_count,trip_summary:fast_accel_count,trip_summary:night_time,trip_summary:trip_minute_details")
--tblproperties ("hbase.table.name" = "xyz")
;

insert overwrite table alex_sr.sre_trip_info_hbase
select concat_ws('~', trip_vin, trip_number),
	trip_vin,
	trip_number,
	trip_start_timestamp,
	trip_end_timestamp,
	trip_start_day,
	trip_start_hr,
	trip_start_min,
	trip_end_day,
	trip_end_hr,
	trip_end_min,
	trip_duration,
	trip_length_miles,
	trip_length_kms,
	trip_hard_break_count,
	trip_fast_accel_count,
	trip_night_time_drive_min,
	trip_minute_details
from alex_sr.sre_trip_info
;

-- get 'alex_sr.sre_trip_info_hbase', '0507011~2264030781404762355'

create table alex_sr.sre_trip_minute_aggregates (
	trip_number string,
	start_timestamp string,
	end_timestamp string,
	accel_count bigint,
	hard_break_count bigint,
	night_drive_sec bigint,
	miles_driven double,
	speed_in_mph double
)
row format delimited fields terminated by ','
;

load data local inpath '/mnt/hgfs/Programs/temp/hdp/sre_trip_minute_aggregates'
overwrite into table alex_sr.sre_trip_minute_aggregates;


create table alex_sr.sre_trip_minute_aggregates_hbase (
	trip_minute_key string,
        trip_number string,
        start_timestamp string,
        end_timestamp string,
        accel_count bigint,
        hard_break_count bigint,
        night_drive_sec bigint,
        miles_driven double,
        speed_in_mph double
)
stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
with serdeproperties ("hbase.columns.mapping" = ":key,trip_minute:trip_number,trip_minute:trip_start_timestamp,trip_minute:trip_end_timestamp,trip_minute:fast_accel_count,trip_minute:hard_break_count,trip_minute:night_drive_sec,trip_minute:miles_driven,trip_minute:speed_in_mp")
;

insert overwrite table alex_sr.sre_trip_minute_aggregates_hbase
select concat_ws('~', trip_number, substring(start_timestamp, 1, 16)),
        trip_number,
        start_timestamp,
        end_timestamp,
        accel_count,
        hard_break_count,
        night_drive_sec,
        miles_driven,
        speed_in_mph
from alex_sr.sre_trip_minute_aggregates
where speed_in_mph is not null
;


--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--
-- Part 2 Start Hbase rest service (these are not sql scripts)
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

hbase rest start --infoport 12346 --port 12345


curl -I -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_minute_aggregates_hbase/00000040605421171404785144*"
curl -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_minute_aggregates_hbase/00000040415422651404835944~2014-07-08 16:15"
curl -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_minute_aggregates_hbase/00000040415422651404835944~2014-07-08*"
curl -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_info_hbase/0000004~0416442141404839534"
curl -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_info_hbase/0507011~*"
curl -H "Accept: application/json" "http://192.168.1.114:12345/alex_sr.sre_trip_info_hbase/0507011*"



--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--
-- Part 3 Start Tomcat server and deploy 'demo-sre.war'
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

Assuming demo-sre web application has url:
http://localhost:8080/demo-sre/demo-sre.html

Assuming Hbase Rest Service has the following properties:

hbase-server=192.168.1.114
hbase-port=12345
trip-info-table=alex_sr.sre_trip_info_hbase
trip-minute-table=alex_sr.sre_trip_minute_aggregates_hbase

Then after starting the demo-sre web application, set up by type in a browser:

http://localhost:8080/demo-sre/set?hbase-server=192.168.1.114&hbase-port=12345&trip-info-table=alex_sr.sre_trip_info_hbase&trip-minute-table=alex_sr.sre_trip_minute_aggregates_hbase



--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--
-- Part 4 Running the web application
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

There are only 2 vin numbers in our data set:
0507011
0000004


