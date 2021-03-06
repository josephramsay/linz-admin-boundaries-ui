CREATE DATABASE testdb;

CREATE ROLE aims_admin
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
  
CREATE ROLE aims_dba
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE ROLE aims_reader
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
  
CREATE ROLE aims_user
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
  
CREATE ROLE dummyuser LOGIN
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
  
CREATE ROLE testuser0 LOGIN
  ENCRYPTED PASSWORD 'md57da3ce7fd1770f35384d56e6d84a1122'
  NOSUPERUSER INHERIT NOCREATEDB CREATEROLE NOREPLICATION;
GRANT aims_user TO testuser0;

CREATE ROLE testuser1 LOGIN
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
GRANT aims_dba TO testuser1;

CREATE ROLE testuser2 LOGIN
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
GRANT aims_admin TO testuser2;

CREATE ROLE testuser3 LOGIN
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
GRANT aims_user TO testuser3;

CREATE ROLE testuser4 LOGIN
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;
GRANT aims_admin TO testuser4;
GRANT aims_dba TO testuser4;
GRANT aims_reader TO testuser4;
GRANT aims_user TO testuser4;
GRANT non_aims_group TO testuser4;