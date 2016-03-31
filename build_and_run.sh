#!/usr/bin/env bash
mvn package -DskipTests; java -jar target/kry-progtest-1.0-SNAPSHOT-fat.jar
