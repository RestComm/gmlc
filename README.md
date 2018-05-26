
[Try Restcomm Cloud NOW for FREE!](https://www.restcomm.com/sign-up/) Zero download and install required.


All Restcomm [docs](https://www.restcomm.com/docs/) and [downloads](https://www.restcomm.com/downloads/) are now available at [Restcomm.com](https://www.restcomm.com).


Restcomm Location Server (GMLC)
============

[![Join the chat at https://gitter.im/RestComm/gmlc](https://badges.gitter.im/RestComm/gmlc.svg)](https://gitter.im/RestComm/gmlc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
The Gateway Mobile Location Centre enables offering Location Based Services (LBS) involving mobile subscribers roaming accross either legacy GSM or UMTS/HSPA+ networks, or Next Generation Networks like LTE/LTE-Advanced.

In one PLMN (Public Land Mobile Network), there may be more than one GMLC. The GMLC is the first node an external LCS client accesses in a GSM, UMTS or LTE network. The GMLC may request routing information from the HLR (Home Location register) or HSS (Home Subscriber Server). After performing registration authorization, it sends positioning requests to either the VMSC (Visited Mobile Switching Centre), SGSN (Serving GPRS Support Node) MSC (Mobile Switching Centre) Server or MME (Mobility Management Entity) and receives final location estimates from the corresponding network node. Furthermore, it may receive mobile subscriber location reports on event based deferred location requests.

GMLC is built on [RestComm jSS7](https://github.com/RestComm/jSS7) and [RestComm jDiameter](https://github.com/RestComm/jdiameter).
