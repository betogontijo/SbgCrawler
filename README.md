# SbgCrawler
Step 1 - Download mongodb server: https://www.mongodb.com/download-center#community
Step 2 - Download mariaDB (user:root, password:123)
Step 3 - Create table on mariaDB:
{
create database SbgDB;
use SbgDB;
create table refs (uri varchar(511) unique);
}
