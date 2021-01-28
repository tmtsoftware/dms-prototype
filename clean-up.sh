#!/bin/bash
hdfs dfs -rm -r /tmp/json/
hdfs dfs -rm -r /data/json/
hdfs dfs -mkdir /data/json
hdfs dfs -ls /data/
