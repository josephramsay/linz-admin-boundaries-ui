# AdminBoundaries

Replacement application for download_admin_bdys.py.

Can be deployed as a standalone dpkg with minimal GUI, a crontask or run as a Java webapp.

Provides a staged import process for Admin Boundary data giving users a decision point to
accept or reject import data.

## Install

git clone https://github.com/josephramsay/AdminBoundaries.git

#### WebApp install. 
 Copy gradle.properties.template to gradle.properties and edit with desired config.
 Run gradle deploy.
    
#### Debian install. 
 Run, adminboundaries.deb.build.sh. this will create a debian package download_admin_bdys.deb in your git directory
 Run, dpkg -i download_admin_bdys.deb
    
#### Basic install.
 Copy the two files download_admin_bdys.py and download_admin_bdys.ini to your preferred location.
 Edit the ini file with desired parameters
 Add cronjob if required, python download_admin_bdys [opts]
 
## Usage

python download_admin_bdys.py [opts]
opts:
    load. Read configured files into import tables
    transfer. Transfer import tables to final schema 
    reject. Reject changes and delete import tables
    
Using the Web UI similar functioality is provided with buttons activating the load, transfer 
and reject functions.
In addition the Web UI provides individual table comparison functions (this returns the 
results of the table_version.compare_tables() database function) and an 'optional' button which executes 
specially configured functions that may need to be run after a successful update.

 
## Config
```

# reserved for future use i.e. when stats provide web access
[source]
base_uri = http://maps.stats.govt.nz/wss/service/arcgis1/guest/Boundaries

# the database being updated, optionally any number of database servers can be 
# entered here provided they all conform to a common configuration. The first 
# named database will be the one displayed in the Web UI. Based on the webserver
# name this field can be omitted and a default value for the database server 
# in the 'cluster' will be chosen
[database]
name = <dbname (autocomplete if omitted)>
rolename = <role?>
host = <database host>
user = <database user>
port = <database port>
password = <database user password>
schema = <temp_import_schema>
originschema = <destination schema>

# list of notification recipients and email details
[user]
list = <user list as csv, email recipients>
domain = linz.govt.nz
smtp = <smtp server>
link =

# grid settings
[layer]
name = territorial_authority
output_srid = 4167
geom_column = shape
create_grid = True
grid_res = 0.05
shift_geometry = True

# ftp settings for connection to meshblock data
[connection]
ftphost = <ftphost>
ftpport = <ftpport>
ftpuser = <ftpuser>
ftppass = <ftppass>
ftppath = <ftppath>

# import settings for meshblock layers
[meshblock]
filepattern = Stats_Meshblock_concordance_(\d{8}).zip
localpath = .
colmap = {
    "statsnz_meshblock":{
    "table":"meshblock",
    "rename":[{"old":"mb_code","new":"code"},{"old":"geom","new":"shape"}],
    "drop":["ogc_fid"],
    "primary":"code",
    "geom":"shape",
    "srid":"4167",
    "permission":["user1","user2"]
    },
    "statsnz_ta":{
    "table":"territorial_authority",
    "rename":[{"old":"geom","new":"shape"}],
    "drop":["ta_code"],
    "primary":"ogc_fid",
    "geom":"shape",
    "srid":"4167",
    "permission":["user1","user2"],
    "grid":{"geocol":"shape","res":"10"}
    },
    "meshblock_concordance":{
    "table":"meshblock_concordance",
    "primary":"meshblock",
    "permission":["user1","user2"]
    }
    }
    
# import settings for localities layer
[nzlocalities]
filepath = /mnt/geo_dat/NZ Localities/
filename = nz_localities
colmap = {
    "nz_locality":{
    "table":"nz_locality",
    "rename":[{"old":"geom","new":"shape"}],
    "drop":["ogc_fid"],
    "cast":[{"cast":"id","type":"integer"}],
    "primary":"id",
    "geom":"shape",
    "srid":"4167",
    "permission":["user1","user2"]
    }
    }
    
# the colmap parameter in the import settings dictates the file to table mapping
# for the named layer. fields include:
# table - the table being populated
# rename - column renaming from old->new
# drop - columns to drop
# cast - column data type to set
# primary - column to set as primary key
# geom - name of the geometry column
# srid - SRID of this table
# permission - comma sep user list of users who have full access to this table
# grid - grid column name and required grid resolution

# list of function to be run post import 
[optional]
functions = ["aims_stage.fnreferencedataupdatelocality('referencedata.maintainer')",
    "aims_stage.fnreferencedataupdatemeshblock('referencedata.maintainer')"]

# placeholder for validation functions if any (not implemented)
[validation]
data = [('select count(*) from meshblock where length(code)>7','0'),]
spatial = [{'',''},]
```
