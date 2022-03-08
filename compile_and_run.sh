#!/bin/bash
source auth.jdbc.TEMPLATE 
javac *.java
java -cp mysql-connector-java-8.0.16.jar:. InnReservations