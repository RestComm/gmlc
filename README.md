RestComm GMLC
============
The Gateway Mobile Location Centre enables you to offer Location Based Services (LBS) to mobile subscribers in GSM and UMTS network.

In one PLMN (Public Land Mobile Network), there may be more than one GMLC. The GMLC is the first node an external LCS client accesses in a GSM, UMTS or LTE network. The GMLC may request routing information from the HLR (Home Location register) or HSS (Home Subscriber Server). After performing registration authorization, it sends positioning requests to either the VMSC (Visited Mobile Switching Centre), SGSN (Serving GPRS Support Node) MSC (Mobile Switching Centre) Server or MME (Mobility Management Entity) and receives final location estimates from the corresponding network node. Furthermore, it may receive mobile subscriber location reports on event based deferred location requests.

GMLC is built on [RestComm jSS7](https://github.com/RestComm/jSS7) and [RestComm jDiameter](https://github.com/RestComm/jdiameter).

## Downloads

Download source code and build from [here](https://github.com/RestComm/gmlc/releases) or Continuous Delivery binary from [CloudBees](https://mobicents.ci.cloudbees.com/job/RestComm-GMLC/)

Install on Ubuntu 14.04 from Source
========

*NOTE: Requires Maven 3 and will not build with Maven 2.*

You can run these instructions as a normal user (root shouldn't be required).

Where relevant you may have to replace "user" with your username, and "/home/user" with your own home directory.

Install General Dependencies
```
apt-get install git software-properties-common screen ca-certificates
```

Install Oracle Java 7
```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer
sudo apt-get install oracle-java7-set-default
java -version
```

Install Maven 3
```
add-apt-repository "deb http://ppa.launchpad.net/natecarlson/maven3/ubuntu precise main"
apt-get update
apt-get install maven3
ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn
```

Install RestComm JAIN SLEE
```
apt-get install unzip ant
su user
cd ~
mkdir RestComm
cd RestComm
# Download from https://mobicents.ci.cloudbees.com/job/Mobicents-JAIN-SLEE-Release/
unzip ~/mobicents-jainslee-2.7.0.FINAL-jboss-5.1.0.GA.zip
```

Setup JBOSS
```
vi ~/.profile
#
JBOSS_HOME=/home/user/slee/jboss-5.1.0.GA
export JBOSS_HOME
#

JBOSS_HOME=/home/user/mobicents/jboss-5.1.0.GA

vi $JBOSS_HOME/server/default/conf/bootstrap/profile.xml
# Find AttachmentStore, add the class= part below
<constructor><parameter class="java.io.File">
#

Test run the server
cd $JBOSS_HOME/bin
# run the server open on all network interfaces
./run.sh -b 0.0.0.0
```

You can keep the server running and watch each of these get deployed

```
cd ~/RestComm/resources/http-servlet
ant deploy
```

```
cd
git clone https://code.google.com/p/jain-slee.ss7/
cd jain-slee.ss7/resources/map
mvn clean install
```

Basic GMLC configuration
```
vi ~/RestComm/jboss-5.1.0.GA/server/default/data/GmlcManagement_gmlcproperties.xml
#
<?xml version="1.0" encoding="UTF-8" ?>
<gmlcgt value="628184422892"/>
<gmlcssn value="8"/>
<hlrssn value="6"/>
<mscssn value="8"/>
<maxmapv value="3"/>
#
```

Now deploy the GMLC
```
cd ~
git clone https://github.com/RestComm/gmlc.git
cd gmlc
mvn clean install
```

You might see an error message like this:
```
13:03:49,540 WARN  [DeploymentManager] (HDScanner) Unable to INSTALL gmlc-services-du-1.0.0-SNAPSHOT.jar right now. Waiting for dependencies to be resolved.
```

If so, press ctrl-c to shutdown JBoss and restart it again and the dependency issue should be resolved now that both the MAP RA and the GMLC are installed.

You can view the various included web consoles here:

1. http://yourserver:8080/admin-console/
  * user: admin
  * password: admin
2. http://yourserver:8080/slee-management-console
3. http://yourserver:8080/jmx-console/

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

Example Request:
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

Example Successful Response:
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

Example System Error Response:
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

Example Position Error Response:
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
========
1. via HTTP GET (for simple testing)
```
http://yourserver:8080/mobicents?msisdn=<msisdn>
```

2. via HTTP POST (using MLP)
```
POST XML request to http://yourserver:8080/mobicents
```

Send a sample MLP request using cURL to the localhost:
```
sudo apt-get install curl
vi ./req.txt
# Paste in the above example MLP request
curl -X POST -d @req.txt http://yourserver:8080/mobicents
```

MLP Result Codes
========
See section 5.4.1 Result codes in the MLP specification for full details

These system errors (see example System Error Response above) are supported:
* 1 - SYSTEM FAILURE
* 105 - FORMAT_ERROR,
* 4 - UNKNOWN_SUBSCRIBER

These position errors (see example Position Error Response above) are supported:
* 201 - QOP_NOT_ATTAINABLE
* 6 - POSITION_METHOD_FAILURE

Code Summary
========
For a quick bit of help with the code, it's worth mentioning that the primary location where the action takes place is here:

```
gmlc/core/slee/sbbs/src/main/java/org/mobicents/gmlc/slee
```

That's where you'll find the guts of the application.

TODO (v1.0)
========
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

Want to Contribute ? 
========
[See our Contributors Guide](https://github.com/RestComm/Restcomm-Core/wiki/Contribute-to-RestComm)

Issue Tracking and Roadmap
========
[Issue Tracker](https://github.com/RestComm/gmlc/issues)

Questions ?
========
Please ask your question on [StackOverflow](http://stackoverflow.com/questions/tagged/restcomm) or the Google [public forum](http://groups.google.com/group/restcomm)

License
========

RestComm GMLC is lead by [TeleStax](http://www.telestax.com/), Inc. and developed collaboratively by a community of individual and enterprise contributors.

RestComm GMLC is licensed under dual license policy. The default license is the Free Open Source GNU Affero GPL v3.0. Alternatively a commercial license can be obtained from Telestax ([contact form](http://www.telestax.com/contactus/#InquiryForm))

Acknowledgements
========
[See who has been contributing to RestComm](http://www.telestax.com/opensource/acknowledgments/)
