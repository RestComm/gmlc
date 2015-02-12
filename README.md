Mobicents GMLC
============
The Gateway Mobile Location Centre enables you to offer Location Based Services (LBS) to mobile subscribers in GSM and UMTS network.

In one PLMN (Public Land Mobile Network), there may be more than one GMLC. The GMLC is the first node an external LCS client accesses in a GSM or UMTS network. The GMLC may request routing information from the HLR (Home Location register) or HSS (Home Subscriber Server). After performing registration authorization, it sends positioning requests to either the VMSC (Visited Mobile Switching Centre), SGSN (Serving GPRS Support Node) or MSC (Mobile Switching Centre) Server and receives final location estimates from the corresponding entity.

GMLC is built on [Mobicents jSS7](https://github.com/Mobicents/jSS7).

Install on Ubuntu 14.04 from Source
========
You can run these instructions as a normal user (root shouldn't be required).

Where relevant you may have to replace "user" with your username, and "/home/user" with your own home directory.

Install Oracle Java 7
```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer
sudo apt-get install oracle-java7-set-default
java -version
```

Install Maven
```
apt-get install git software-properties-common screen
add-apt-repository "deb http://ppa.launchpad.net/natecarlson/maven3/ubuntu precise main"
apt-get update
apt-get install maven3
ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn
```

Install Mobicents JAIN SLEE
```
apt-get install unzip ant
su user
cd ~
mkdir mobicents
cd mobicents
# Download from http://sourceforge.net/projects/mobicents/files/Mobicents%20JAIN%20SLEE%20Server/2.7.0.FINAL/mobicents-jainslee-2.7.0.FINAL-jboss-5.1.0.GA.zip/download
# www.mobicents.org/slee/downloads.html
unzip ~/mobicents-jainslee-2.7.0.FINAL-jboss-5.1.0.GA.zip
```

Setup JBOSS
```
vi ~/.profile
#
JBOSS_HOME=/home/user/mobicents/jboss-5.1.0.GA
export JBOSS_HOME
#

vi $JBOSS_HOME/server/default/conf/bootstrap/profile.xml
conf/bootstrap/profile.xml
# Find AttachmentStore
<constructor><parameter class="java.io.File">
#

Test run the server
cd $JBOSS_HOME/bin
# run the server open on all network interfaces
./run.sh -b 0.0.0.0
```

You can keep the server running and watch each of these get deployed

```
cd ~/mobicents/resources/http-servlet
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
vi ~/mobicents/jboss-5.1.0.GA/server/default/data/GmlcManagement_gmlcproperties.xml
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
git clone https://github.com/Mobicents/gmlc.git
cd gmlc
mvn clean install
```

You can view the various included web consoles here:

* http://yourserver:8080/admin-console/
** user: admin
** password: admin
* http://yourserver:8080/slee-management-console
* http://yourserver:8080/jmx-console/

Want to Contribute ? 
========
[See our Contributors Guide](https://github.com/Mobicents/Restcomm/wiki/Contribute-to-RestComm)

Issue Tracking and Roadmap
========
[Issue Tracker](https://github.com/Mobicents/gmlc/issues)

Questions ?
========
Please ask your question on [StackOverflow](http://stackoverflow.com/search?q=mobicents) or the Google [public forum](http://groups.google.com/group/mobicents-public)

License
========

Mobicents GMLC is lead by [TeleStax](http://www.telestax.com/), Inc. and developed collaboratively by a community of individual and enterprise contributors.

Mobicents GMLC is licensed under dual license policy. The default license is the Free Open Source GNU Affero GPL v3.0. Alternatively a commercial license can be obtained from Telestax ([contact form](http://www.telestax.com/contactus/#InquiryForm))

Acknowledgements
========
[See who has been contributing to RestComm](http://www.telestax.com/opensource/acknowledgments/)
