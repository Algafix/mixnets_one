#! /bin/sh
#/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/
java -Xmx512M -cp .:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
