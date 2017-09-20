# SbgCrawler
Step 1 - Download mongodb server: https://www.mongodb.com/download-center#community
Step 2 - Download mariaDB : https://mariadb.com/downloads/mariadb-tx
Step 3 - Create table on mariaDB:
{
CREATE DATABASE SbgDB;
USE SbgDB;
CREATE TABLE refs (uri VARCHAR(511) NOT NULL, PRIMARY KEY (uri));
}
