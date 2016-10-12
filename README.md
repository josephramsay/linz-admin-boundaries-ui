# AdminBoundaries

Replacement application for download_admin_bdys.py.

Can be deployed as a standalone dpkg with minimal GUI, a crontask or run as a Java webapp.

Provides a staged import process for Admin Boundary data giving users a decision point to
accept or reject import data.

## Install

git clone https://github.com/josephramsay/AdminBoundaries.git

WebApp install. 
    Copy gradle.properties.template to gradle.properties and edit with desired config.
    Run gradle deploy.
    
Debian install. 
    Run, adminboundaries.deb.build.sh. this will create a debian package download_admin_bdys.deb in your git directory
    Run, dpkg -i download_admin_bdys.deb
    
Crontask install.
    Copy the two files download_admin_bdys.py and download_admin_bdys.ini to your preferred location.
    Edit the ini file with desired parameters
    Add cronjob, python download_admin_bdys [opts]
 
## Usage

python download_admin_bdys.py [opts]
opts:
    load. Read configured files into import tables
    transfer. Transfer import tables to final schema 
    reject. Reject changes and delete import tables
    
## Config

### database Section
host = <database host>
name = <database name>
rolename = <preferred role to execute queries>
user = <database user>
password = <databse pass>
port = <database port>
schema = <schema name to save imported files>
originschema = <destination schema>

### user Section
list=<list of usernames to email>
domain=<domain name for email service>
smtp=<smtp server to process email requests>
link=<link to provide web app users>

### layer Section
