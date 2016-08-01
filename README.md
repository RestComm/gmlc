RestComm GMLC
============
The Gateway Mobile Location Centre enables offering Location Based Services (LBS) involving mobile subscribers roaming accross either legacy GSM or UMTS/HSPA+ networks, or Next Generation Networks like LTE/LTE-Advanced.

In one PLMN (Public Land Mobile Network), there may be more than one GMLC. The GMLC is the first node an external LCS client accesses in a GSM, UMTS or LTE network. The GMLC may request routing information from the HLR (Home Location register) or HSS (Home Subscriber Server). After performing registration authorization, it sends positioning requests to either the VMSC (Visited Mobile Switching Centre), SGSN (Serving GPRS Support Node) MSC (Mobile Switching Centre) Server or MME (Mobility Management Entity) and receives final location estimates from the corresponding network node. Furthermore, it may receive mobile subscriber location reports on event based deferred location requests.

GMLC is built on [RestComm jSS7](https://github.com/RestComm/jSS7) and [RestComm jDiameter](https://github.com/RestComm/jdiameter).

Downloads
========

Download source code and build from [here](https://github.com/RestComm/gmlc/releases) or Continuous Delivery binary from [CloudBees](https://mobicents.ci.cloudbees.com/job/RestComm-GMLC/)

Installation
============
The best way to install the RestComm GMLC is from the latest binary release zip file which can be found [here](https://github.com/RestComm/gmlc/releases). Alternately, you can build it yourself from source, but this is only recommended if you intend to make development changes

Binary Installation
===================
### Download the latest [binary release](https://github.com/RestComm/gmlc/releases)
```
cd ~
rm -rf RestComm
unzip ~/gmlc/release/restcomm-gmlc-1.0.0-SNAPSHOT.zip
mv restcomm-gmlc-1.0.0-SNAPSHOT RestComm
```
### Setup the GMLC configuration files
```
vi $JBOSS_HOME/server/default/data/GmlcManagement_gmlcproperties.xml
<?xml version="1.0" encoding="UTF-8" ?>
<gmlcgt value="628184422892"/>
<gmlcssn value="145"/>
<hlrssn value="6"/>
<mscssn value="8"/>
<maxmapv value="3"/>
```
### Run the server
```
cd ~/RestComm/jboss-5.1.0.GA/bin/
chmod +x ./run.sh
./run.sh -b 0.0.0.0
```
Source Installation
========
*Requires Maven 3 and will not build with Maven 2. Ubuntu 14.04 and CentOS 6 are the tested Linux install environments, but the system should work so long as you install matching packages*

In the below instructions, replace "user" with your local username, and "/home/user" with your own home directory.
### Ubuntu 14.04
#### Install Package Dependencies
```
sudo apt-get install git software-properties-common screen ca-certificates unzip ant
# The below fixes an issue with jruby and the documentation compilation
# reporting "NotImplementedError: block device detection unsupported or native support failed to load"
sudo apt-get install libc6-dev
```
#### Install Oracle Java 7
```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer
sudo apt-get install oracle-java7-set-default
java -version
```
#### Install Maven 3
```
add-apt-repository "deb http://ppa.launchpad.net/natecarlson/maven3/ubuntu precise main"
apt-get update
apt-get install maven3
ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn
```
#### Setup JBOSS environment variable
```
vi ~/.profile
#
JBOSS_HOME=/home/user/RestComm/jboss-5.1.0.GA
export JBOSS_HOME
#
JBOSS_HOME=/home/user/RestComm/jboss-5.1.0.GA
```
### CentOS 6
#### Install Package Dependencies
```
sudo yum install git software-properties-common screen ca-certificates
# This installs java as well
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
mvn --version
sudo yum install unzip ant
```
#### Setup JBOSS environment variable
```
vi ~/.bashrc
#
JBOSS_HOME=/home/user/RestComm/jboss-5.1.0.GA
export JBOSS_HOME
#
source ~/.bashrc
JBOSS_HOME=/home/user/RestComm/jboss-5.1.0.GA
```
### For All Systems
#### Install [JSS7](https://github.com/RestComm/jss7/wiki/Build-jSS7-from-Source) dependency
```
cd ~
mkdir jss7_dependency
cd jss7_dependency
wget https://www.dialogic.com/files/DSI/developmentpackages/linux/dpklnx.Z
tar --no-same-owner -zxvf dpklnx.Z
mvn install:install-file -DgroupId=com.vendor.dialogic -DartifactId=gctapi -Dversion=6.7.1 -Dpackaging=jar -Dfile=./JAVA/gctApi.jar
wget http://www.datanucleus.org/downloads/maven2/com/sun/jdmk/jmxtools/1.2.1/jmxtools-1.2.1.jar
mvn install:install-file -DgroupId=com.sun.jdmk -DartifactId=jmxtools -Dversion=1.2.1 -Dpackaging=jar -Dfile=jmxtools-1.2.1.jar
```
#### Download and build the GMLC source code
```
cd ~
git clone git@github.com:RestComm/gmlc.git
cd gmlc/release
# This may take a while
ant
# The end result will be a zip file containing a full release build like "restcomm-gmlc-1.0.0-SNAPSHOT.zip"
```
#### Install newly created GMLC release
```
cd ~
rm -rf RestComm
unzip ~/gmlc/release/restcomm-gmlc-1.0.0-SNAPSHOT.zip
mv restcomm-gmlc-1.0.0-SNAPSHOT RestComm
```
#### Setup the GMLC configuration files
```
vi $JBOSS_HOME/server/default/data/GmlcManagement_gmlcproperties.xml
<?xml version="1.0" encoding="UTF-8" ?>
<gmlcgt value="628184422892"/>
<gmlcssn value="145"/>
<hlrssn value="6"/>
<mscssn value="8"/>
<maxmapv value="3"/>
```
#### Run the server
```
cd ~/RestComm/jboss-5.1.0.GA/bin/
chmod +x ./run.sh
./run.sh -b 0.0.0.0 -Xdebug -Xrunjdwp:transport=dt_socket,address=52297,suspend=n,server=y
```

Getting Started as a Developer
==============================
We'd love to have you join us and contribute ideas and code to improve and extend the RestComm GMLC (as well as other components of the RestComm) suite. Before you get started, there's a few things you need to do:
1. Visit the TeleStax [Open Source Project Information Site](https://telestax.com/open-source/)
   1. Here you can see the other TeleStax open source projects and also register as an official contributor
2. Read the TeleStax [Open Source Playbook](https://telestax.com/wp-content/uploads/2016/04/TeleStaxOpenSourcePlaybook.pdf)
   1. This is critical for learning how to properly develop and contribute code to RestComm
   2. Please follow the work-flow described in the Playbook for how to properly design, develop, test, and release new code

How to Setup a Development Environment
========
A standard development environment consists of a Desktop environment along with a virtual machine running Linux for building and testing.
### Setup your development VM
1.  Install a server edition of CentOS 7 or Ubuntu 14.04 in a VM
2. Create a local user account for yourself, and use this account instead of root
3. Install the GMLC
    1. The reason for installing a release first is that you will deploy your development code on top of the installed release, but the release install will give you all of the required dependencies
    2. Either install from source (see "Source Installation" above) if you want the latest, or you can install a binary release (see "Binary Installation" above)
### Develop some code
#### Option A: Make code changes by hand
This is fine for small changes, or where you don't need a debugger, etc. Just directly make any changes required, and then deploy them on top of the installed GMLC release from Step 2. 

Run the maven command below in ~/gmlc to build and deploy your changes
```
cd gmlc
mvn clean install
```
#### Option B: Use a full IDE with debugging capabilities
IntelliJ IDEA Ultimate provides direct integration with JBoss and is the recommended development IDE. 

*Note: Ultimate edition is required for access to the JBoss plug-in*

1. Ensure SSH is installed in your VM to allow for sftp file synchronization
   1. sudo apt-get install openssh-server openssh-sftp-server
2. Remove the existing deployed copy of the GMLC from JBoss on your VM
    1. The GMLC will be re-deployed by IDEA directly
    2. Stop JBoss if it's currently running
	3. rm ~/RestComm/jboss-5.1.0.GA/server/default/deploy/gmlc-services-du-1.0.0-SNAPSHOT.jar 
3. Download and install the Oracle Java JDK on your desktop, with a version that matches the one installed in your development VM. At this time we use the 32-bit [Java SE JDK 7u80](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
4. Clone a local copy of the GMLC source code on your desktop
5. Create a project in IDEA
    1. New Project from Existing Sources
    2. Select pom.xml from the root directory of the local desktop copy of the GMLC source
    3. Un-check "Search for projects recursively"
    4. Check "Keep project files in", choose an alternate directory like "gmlc-idea" below your "gmlc" source dir
    5. Check "Import Maven projects automatically"
    6. Keep the other defaults
    7. It's not necessary to select any profiles
    8. Keep the default "org.mobicents.gmlc:parent" as the project to import
    9. Select JDK 1.7 as the project SDK
6. Install the GCTAPI manually using IDEA's maven
    1. Download the [GCT API archive](https://www.dialogic.com/files/DSI/developmentpackages/linux/dpklnx.Z)
    2. Extract the contents out to any local directory (note: This is a tar/gzip file, it may help you to rename it to .tar.gz)
    3. Either manually run Maven if you have it accessible on the command-line (this will work on Windows too)
        ```
        mvn install:install-file -DgroupId=com.vendor.dialogic -DartifactId=gctapi -Dversion=6.7.1 -Dpackaging=jar -Dfile=./JAVA/gctApi.jar
        ```
    4. Or you can use IDEA's built-in Maven as a quick work-around:
        1. Run->Edit Configurations
        2. Press the + button
        3. Select Maven
        4. Name: install gctapi
        5. Working directory: [select your GMLC source directory]
        6. Command-line: `install:install-file -DgroupId=com.vendor.dialogic -DartifactId=gctapi -Dversion=6.7.1 -Dpackaging=jar -Dfile=[MYLOCALDIRECTORYFROMABOVE]/gctapi/JAVA/gctApi.jar`
        7. Press Ok
        8. Run->Run install gctapi
        9. Scroll through the console output, and you should see a line this, meaning it worked:
            ```
            [INFO] --- maven-install-plugin:2.3.1:install-file (default-cli) @ restcomm-gmlc-parent ---
            [INFO] Installing C:\src\download\gctapi\JAVA\gctApi.jar to C:\Users\andrew\.m2\repository\com\vendor\dialogic\gctapi\6.7.1\gctapi-6.7.1.jar
            ```
        10. You can now remove the 'install gctapi' run configuration as you won't need it again
7. Download a local copy of [JBoss 5.1.0](https://sourceforge.net/projects/jboss/files/JBoss/JBoss-5.1.0.GA/jboss-5.1.0.GA.zip/download) on to your desktop
    1. Extract the `jboss-5.1.0.GA` folder and place it somewhere on your desktop
8. Setup the JBoss plug-in for remote debugging and deployment
    1. Run->Edit Configurations
    2. Press the + button
    3. Select JBoss Server->Remote
    4. Press Configure next to Application server
        1. Select the `jboss-5.1.0.GA` folder you placed on your desktop above
    7. Open Browser (optional)
        1. Change the URL to your VM's IP address like: http://YourVMIPaddress:8080/
    9. JBoss Server Settings
        1. JMX Port 1099
        2. Username admin  (this is the default unless you change it)
        3. Password admin (this is the default unless you change it)
        4. Remote staging
            1. Type: sftp
            2. Host: Press `...`
            3. Press +
            4. Name: myvm
            5. Type: sftp
            6. SFTP host: YourVMIPaddress
            7. Root path: /home/user
                1. Note: This will actually use /home/user/gmlc-staging
            7. Web server root URL: http://YourVMIPaddress:8080
            8. Press OK
            9. Now select this newly created entry as your Host
            10. Staging
            11. Path from root: /gmlc-staging
            12. Mapped as: /home/user/gmlc-staging
    10. Remote Connection Settings
        1. Host: YourVMIPaddress
        2. Port 8080
    11. Before launch (at the bottom)
        1. Press +
        2. Choose "Run Maven Goal"
        3. Working directory: [select your GMLC source directory]
        4. Command line: package
        5. Press OK
    11. Deployment tab
        12. Deploy at server start-up
        13. Press +
        14. External source
            1. Choose this JAR file: <gmlc src dir>/core/slee/services/du/target/gmlc-services-du-1.0.0-SNAPSHOT.jar
            2. Note: The JAR file may not exist yet - if so, come back to this step after completing step #10 below related to the Maven "package" command. After you run this command once, this JAR will be created so you can select it. It will then be automatically updated before being copied by the "Before launch" command you added above
        14. Deployment method: staging
    12. Startup/Connection tab
        1. Select Debug
        2. Change the Port to 8787
9. Configure JBoss on your VM for debug support
    1. Note: Replace SERVERIP below with your server's IP that's accessible to IDEA - note: don't use 0.0.0.0 in this case
    2. You can run JBoss one-time with debugging support:
        ```
        # Note: make sure this port number below matches the one above in IDEA
        JAVA_OPTS="-Djava.rmi.server.hostname=SERVERIP -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,suspend=n,server=y"
        export JAVA_OPTS
        cd ~/RestComm/jboss-5.1.0.GA/bin
        sh ./run.sh -b 0.0.0.0
        ```
    3. Or you can permanently enable the debug option by adding the below to the bottom of $JBOSS_HOME/bin/run.conf
        ```
        # IDEA remote socket debugging
        JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=SERVERIP -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"
        ```
        Then run the server
        ```
        cd ~/RestComm/jboss-5.1.0.GA/bin
        sh ./run.sh -b 0.0.0.0
        ```
10. TODO: Figure out if there's a better way.. but this the work-around for fixing IDEA's "Make" to build with the missing JiBX class files:
    1. Run->Edit Configurations
    2. Press the + button
    3. Select Maven
    4. Name: package
    5. Working directory: [select your GMLC source directory]
    6. Command line: package
    7. Press OK
    8. Run->Run package
    9. This will run a Maven build/package process, which will cause the missing JiBX class files to be created so that IDEA can then find them for it's internal compile process
10. Run Build->Make in IDEA and ensure that you area able to build the project successfully
11. Execute your JBoss Debug configuration in IDEA via Run->Debug to connect to JBoss
    1. Note: This connects to JBoss, and the connection stays active while you make and upload new changes
    2. It's not necessary (or desirable) to re-run the debug configuration each time you make a build change
12. Make a code change, then press Make, which will prompt you to re-build the code and deploy it to JBoss
13. This should live deploy the change to the running server and will be effective without a restart

Additional notes about how the above build process works:
* The IDEA JBoss "Remote Staging" just refers to a directory on the remote server where IDEA uploads JAR files that are then directly deployed by it issuing a command to JBoss
* The configuration above builds the "gmlc-services-du" jar using "mvn package", which is what IDEA uploads
* While the IDEA JBoss debugger is connected, it is also then able to hotswap code changes so long as the same class already exists within the deployed JAR
* This means you can make changes to any of the existing code and (as above) just press Make to quickly deploy the change out live to the running system
* Every time you fully restart the JBoss Debug process in IDEA, it will redeploy the full JAR

Administration Console
======================
You can view the various included web consoles here:

1. http://yourserver:8080/admin-console/
    ```
    user: admin
    password: admin
    ```
2. http://yourserver:8080/restcomm-slee-management/
3. http://yourserver:8080/jss7-management-console

Backend
========
* MAP ATI: Any-Time-Interrogation, to gather Cell Global Identity, age of location information and state of the target mobile station from the HLR.
* MAP SRIforLCS: Send Routing Information for Location Services, to gather IMSI and core network entity address (MSC or SGSN) to which send further location request.
* MAP PSL: Provide Subscriber Location, to gather location information from the UTRAN (UMTS Terrestrial Radio Access Network), which should include, besides Cell Global Identity, a location estimate in geographic coordinates of the target User Equipment, depending on available positioning methods (e.g. E-OTD, OTDOA, UTDOA, A-GPS, etc.).
* MAP SLR: Subscriber Location Report, to gather location of a target
User Equipment from the MSC or SGSN when a request for location is either implicitly administered or made at some earlier time in MAP PSL for event based deferred type of location.
* Diameter Send Routing Information for Location Services: analogous to MAP SRIforLCS but over Diameter based SLh interface between GMLC and HSS.
* Diameter Provide Subscriber Location: analogous to MAP PSL but over Diameter based Evolve Packet Core Location Protocol (ELP) SLg interface between GMLC and MME.
* Diameter Subscriber Location Report: analogous to MAP SLR, but over Diameter based Evolve Packet Core Location Protocol (ELP) SLg interface between GMLC and MME.

Operator Backend Requirements
========
Connectivity to the operator's HLR/HSS is needed (SIGTRAN or legacy E1 links over Lh interface, or Diameter connectivity within Evolved Packet Core over SLh interface for LTE location). 
For GSM (2G) location, the GMLC will send a MAP ATI request to the HLR, which will respond with Cell Global Identity, state and age of location (in minutes) for which this location was last updated in the HLR from the MSC/VLR or SGSN at which the target mobile station is attached to.
For 3G (UMTS/HSPA+) location, the GMLC will firstly send a MAP SRIforLCS to the HLR ans subsequently, a MAP PSL to the corresponding MSC or SGSN. Either the MSC or SGSN could send event based MAP SLR back to the GMLC when applicable.
For 4G (LTE) location, the GMLC will proceed in an analogous fashion as for 3G location but over SLh and SLg Diameter based interfaces. 

OMA MLP Support (based on MLP v3.1)
========
Location requests can be sent to the GMLC using plain XML over HTTP(S), with the request being encoded in OMA MLP (Mobile Location Protocol).

* See the full OMA MLP technical specification here:
http://technical.openmobilealliance.org/Technical/technical-information/release-program/current-releases/mlp-v3-1

At present, the GMLC only supports a limited subset of MLP, allowing a simple position request for a single MSISDN, and returning a CirculaArea containing a X/Y coordinate point with a radius (accuracy).

Requests are to be sent over HTTP POST.

For the purpose of testing, you may use the dummy mobile number 19395550113 to receive a successful dummy response.

Example MLP Request
---------------
```
<?xml version="1.0" encoding="UTF-8"?>
 <!DOCTYPE svc_init SYSTEM "MLP_SVC_INIT_310.DTD">
 <svc_init xmlns="MLP_SVC_INIT_310.dtd">
   <hdr>
     <client>
       <id>USERNAME</id>
       <pwd>PASSWORD</pwd>
       <serviceid>SERVICEID</serviceid>
     </client>
   </hdr>
   <slir>
     <msids>
       <msid type="MSISDN">MSISDN OF TARGET</msid>
     </msids>
     <eqop>
        <resp_timer>15</resp_timer>
     </eqop>
   </slir>
 </svc_init>
```
Example Successful MLP Response
---------------------------
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE svc_result SYSTEM "MLP_SVC_RESULT_310.DTD">
<svc_result xmlns="MLP_SVC_RESULT_310.dtd" ver="3.1.0">
    <slia ver="3.1.0">
        <pos>
            <msid>MSISDN OF TARGET</msid>
            <pd>
                <time utc_off="-0700">20150314141702</time>
                <shape>
                    <CircularArea>
                        <coord>
                            <X>27 28 25.00S</X>
                            <Y>153 01 43.00E</Y>
                        </coord>
                        <radius>5000</radius>
                    </CircularArea>
                </shape>
            </pd>
        </pos>
    </slia>
</svc_result>
```
Example MLP System Error Response
-----------------------------
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE svc_result SYSTEM "MLP_SVC_RESULT_310.dtd">
<svc_result xmlns="MLP_SVC_RESULT_310.dtd" ver="3.1.0">
  <slia ver="3.1.0">
    <result resid="1">SYSTEM FAILURE</result>
    <add_info>Internal IO or parsing error occurred</add_info>
  </slia>
</svc_result>
```
Example MLP Position Error Response
-------------------------------
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE svc_result SYSTEM "MLP_SVC_RESULT_310.DTD">
<svc_result xmlns="MLP_SVC_RESULT_310.dtd" ver="3.1.0">
    <slia ver="3.1.0">
        <pos>
            <msid>MSISDN OF TARGET</msid>
            <poserr>
                <result resid="6">POSITION METHOD FAILURE</result>
                <time utc_off="-0700">20150314133523</time>
            </poserr>
        </pos>
    </slia>
</svc_result>
```
Request a Location from the GMLC
================================
HTTP GET (for simple testing)
---------------------------------
```
http://yourserver:8080/restcomm/gmlc/rest?msisdn=<msisdn>
```
HTTP POST (using MLP)
-------------------------
```
sudo apt-get install curl
vi ./req.txt
# Paste in the above example MLP request
curl -X POST -d @req.txt http://yourserver:8080/restcomm/gmlc/mlp
```
### MLP Result Codes
See section 5.4.1 Result codes in the MLP specification for full details
#### Supported system errors (see example System Error Response above)
* 1 - SYSTEM FAILURE
* 105 - FORMAT_ERROR,
* 4 - UNKNOWN_SUBSCRIBER
#### Supported position errors (see example Position Error Response above)
* 201 - QOP_NOT_ATTAINABLE
* 6 - POSITION_METHOD_FAILURE

Code Summary
============
For a quick bit of help with the code, it's worth mentioning that the primary location where the action takes place is here:

```
gmlc/core/slee/sbbs/src/main/java/org/mobicents/gmlc/slee
```

That's where you'll find the guts of the application.

TODO (v1.0)
=========
1. Add MLP error/handling code for network timeout
2. Simple Cell ID -> lat/lon conversion database
3. Basic username/password authentication system
4. Add SS7 backend support for 3G & LTE (system currently only works with GSM)
5. Basic cell triangulation support to improve accuracy beyond purely cell-id

Roadmap
========
v1.0
* Basic MLP API front-end interface
* Add SS7 backend support for 3G & LTE (system currently only works with GSM)
* Simple Cell ID -> lat/lon conversion database
* Basic cell triangulation support to improve accuracy beyond purely cell-id (hopefully with timing advance to get a partial arc area instead of only a full cell)
  * http://wiki.opencellid.org/wiki/Menu_map_view#database
* Basic username/password authentication system
v1.1
* Add support for MAP PSI (provide subscriber info) and probably add MAP SRI and MtForwardSM to force update of location if the cell-id information is very stale
* Position history database (last known position, perhaps more)
* More features added to OMA MLP interface
* Privacy management
* Triangulation/accuracy improvements
* Replace location timestamp with actual network timestamp
* Add support for multiple MSISDN requests

Want to Contribute? 
====================
[See our Contributors Guide](https://github.com/RestComm/Restcomm-Core/wiki/Contribute-to-RestComm)

Issue Tracking and Roadmap
=========================
[Issue Tracker](https://github.com/RestComm/gmlc/issues)

Questions?
===========
Please ask your question on [StackOverflow](http://stackoverflow.com/questions/tagged/restcomm) or the Google [public forum](http://groups.google.com/group/restcomm)

License
========

RestComm GMLC is lead by [TeleStax](http://www.telestax.com/), Inc. and developed collaboratively by a community of individual and enterprise contributors.

RestComm GMLC is licensed under dual license policy. The default license is the Free Open Source GNU Affero GPL v3.0. Alternatively a commercial license can be obtained from Telestax ([contact form](http://www.telestax.com/contactus/#InquiryForm))

Acknowledgements
================
[See who has been contributing to RestComm](http://www.telestax.com/opensource/acknowledgments/)
