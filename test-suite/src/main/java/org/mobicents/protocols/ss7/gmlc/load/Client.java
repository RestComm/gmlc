/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.gmlc.load;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.ExchangeType;
import org.mobicents.protocols.ss7.m3ua.Functionality;
import org.mobicents.protocols.ss7.m3ua.IPSPType;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.parameter.RoutingContext;
import org.mobicents.protocols.ss7.m3ua.parameter.TrafficModeType;
import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.datacoding.CBSDataCodingScheme;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.*;
import org.mobicents.protocols.ss7.map.api.service.lsm.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.AuthenticationFailureReportRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.AuthenticationFailureReportResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ForwardCheckSSIndicationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ResetRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeRequest_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeResponse_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.*;
import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.primitives.*;
import org.mobicents.protocols.ss7.map.service.lsm.*;
import org.mobicents.protocols.ss7.map.service.mobility.locationManagement.SupportedLCSCapabilitySetsImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberManagement.APNImpl;
import org.mobicents.protocols.ss7.sccp.*;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.message.SccpMessage;
import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

import com.google.common.util.concurrent.RateLimiter;

import static org.mobicents.protocols.ss7.map.api.service.lsm.LCSEvent.emergencyCallOrigination;
import static org.mobicents.protocols.ss7.map.api.service.lsm.LocationEstimateType.currentLocation;

/**
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 */
public class Client extends TestHarness implements MAPServiceMobilityListener, MAPServiceLsmListener {

  private static Logger logger = Logger.getLogger(Client.class);

  // TCAP
  private TCAPStack tcapStack;

  // MAP
  private MAPStackImpl mapStack;
  private MAPProvider mapProvider;

  // SCCP
  private SccpStackImpl sccpStack;
  private SccpResource sccpResource;
  private SccpMessage sccpMessage;

  // M3UA
  private M3UAManagementImpl clientM3UAMgmt;

  // SCTP
  private NettySctpManagementImpl sctpManagement;

  // a ramp-up period is required for performance testing.
  int endCount = -100;

  // AtomicInteger nbConcurrentDialogs = new AtomicInteger(0);

  volatile long start = 0L;
  volatile long prev = 0L;

  private RateLimiter rateLimiterObj = null;

  protected void initializeStack(IpChannelType ipChannelType) throws Exception {

    this.rateLimiterObj = RateLimiter.create(MAXCONCURRENTDIALOGS); // rate

    this.initSCTP(ipChannelType);

    // Initialize M3UA first
    this.initM3UA();

    // Initialize SCCP
    this.initSCCP();

    // Initialize TCAP
    this.initTCAP();

    // Initialize MAP
    this.initMAP();

    // FInally start ASP
    // Set 5: Finally start ASP
    this.clientM3UAMgmt.startAsp("ASP1");
  }

  private void initSCTP(IpChannelType ipChannelType) throws Exception {
    this.sctpManagement = new NettySctpManagementImpl("Client");
    // this.sctpManagement.setSingleThread(false);
    this.sctpManagement.start();
    this.sctpManagement.setConnectDelay(10000);
    this.sctpManagement.removeAllResourses();

    // 1. Create SCTP Association
    sctpManagement.addAssociation(CLIENT_IP, CLIENT_PORT, SERVER_IP, SERVER_PORT, CLIENT_ASSOCIATION_NAME, ipChannelType,
        null);
  }

  private void initM3UA() throws Exception {
    this.clientM3UAMgmt = new M3UAManagementImpl("Client", null);
    this.clientM3UAMgmt.setTransportManagement(this.sctpManagement);
    this.clientM3UAMgmt.setDeliveryMessageThreadCount(DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);
    this.clientM3UAMgmt.start();
    this.clientM3UAMgmt.removeAllResourses();

    // m3ua as create rc <rc> <ras-name>
    RoutingContext rc = factory.createRoutingContext(new long[]{100L});
    TrafficModeType trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare);
    this.clientM3UAMgmt.createAs("AS1", Functionality.AS, ExchangeType.SE, IPSPType.CLIENT, rc, trafficModeType, 1, null);

    // Step 2 : Create ASP
    this.clientM3UAMgmt.createAspFactory("ASP1", CLIENT_ASSOCIATION_NAME);

    // Step3 : Assign ASP to AS
    Asp asp = this.clientM3UAMgmt.assignAspToAs("AS1", "ASP1");

    // Step 4: Add Route. Remote point code is 2
    clientM3UAMgmt.addRoute(SERVER_SPC, -1, -1, "AS1");

  }

  private void initSCCP() throws Exception {
    this.sccpStack = new SccpStackImpl("MapLoadClientSccpStack");
    this.sccpStack.setMtp3UserPart(1, this.clientM3UAMgmt);

    // this.sccpStack.setCongControl_Algo(SccpCongestionControlAlgo.levelDepended);

    this.sccpStack.start();
    this.sccpStack.removeAllResourses();

    this.sccpStack.getSccpResource().addRemoteSpc(0, SERVER_SPC, 0, 0);
    this.sccpStack.getSccpResource().addRemoteSsn(0, SERVER_SPC, SERVER_SSN, 0, false);

    this.sccpStack.getRouter().addMtp3ServiceAccessPoint(1, 1, CLIENT_SPC, NETWORK_INDICATOR, 0);
    this.sccpStack.getRouter().addMtp3Destination(1, 1, SERVER_SPC, SERVER_SPC, 0, 255, 255);

    ParameterFactoryImpl fact = new ParameterFactoryImpl();
    EncodingScheme ec = new BCDEvenEncodingScheme();
    GlobalTitle gt1 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
        NatureOfAddress.INTERNATIONAL);
    GlobalTitle gt2 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
        NatureOfAddress.INTERNATIONAL);
    SccpAddress localAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt1, CLIENT_SPC,
        CLIENT_SSN);
    this.sccpStack.getRouter().addRoutingAddress(1, localAddress);
    SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt2, SERVER_SPC,
        SERVER_SSN);
    this.sccpStack.getRouter().addRoutingAddress(2, remoteAddress);

    GlobalTitle gt = fact.createGlobalTitle("*", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
        NatureOfAddress.INTERNATIONAL);
    SccpAddress pattern = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0);
    this.sccpStack.getRouter().addRule(1, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.REMOTE, pattern,
        "K", 1, -1, null, 0);
    this.sccpStack.getRouter().addRule(2, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.LOCAL, pattern, "K",
        2, -1, null, 0);
    this.sccpStack.getRouter().addLongMessageRule(1, 1, 16384, LongMessageRuleType.XUDT_ENABLED);
  }

  private void initTCAP() throws Exception {
    this.tcapStack = new TCAPStackImpl("Test", this.sccpStack.getSccpProvider(), CLIENT_SSN);
    this.tcapStack.start();
    this.tcapStack.setDialogIdleTimeout(60000);
    this.tcapStack.setInvokeTimeout(30000);
    this.tcapStack.setMaxDialogs(MAX_DIALOGS);
  }

  private void initMAP() throws Exception {

    // this.mapStack = new MAPStackImpl(CLIENT_ASSOCIATION_NAME, this.sccpStack.getSccpProvider(), SSN);
    this.mapStack = new MAPStackImpl("TestClient", this.tcapStack.getProvider());
    this.mapProvider = this.mapStack.getMAPProvider();

    this.mapProvider.addMAPDialogListener(this);

    this.mapProvider.getMAPServiceMobility().addMAPServiceListener(this);
    this.mapProvider.getMAPServiceMobility().acivate();

    this.mapProvider.getMAPServiceLsm().addMAPServiceListener(this);
    this.mapProvider.getMAPServiceLsm().acivate();

    this.mapStack.start();
  }

  public static void main(String[] args) {

    int noOfCalls = Integer.parseInt(args[0]);
    int noOfConcurrentCalls = Integer.parseInt(args[1]);

    IpChannelType ipChannelType = IpChannelType.SCTP;
    if (args.length >= 3 && args[2].toLowerCase().equals("tcp")) {
      ipChannelType = IpChannelType.TCP;
    } else {
      ipChannelType = IpChannelType.SCTP;
    }

    logger.info("IpChannelType=" + ipChannelType);

    if (args.length >= 4) {
      TestHarness.CLIENT_IP = args[3];
    }

    logger.info("CLIENT_IP=" + TestHarness.CLIENT_IP);

    if (args.length >= 5) {
      TestHarness.CLIENT_PORT = Integer.parseInt(args[4]);
    }

    logger.info("CLIENT_PORT=" + TestHarness.CLIENT_PORT);

    if (args.length >= 6) {
      TestHarness.SERVER_IP = args[5];
    }

    logger.info("SERVER_IP=" + TestHarness.SERVER_IP);

    if (args.length >= 7) {
      TestHarness.SERVER_PORT = Integer.parseInt(args[6]);
    }

    logger.info("SERVER_PORT=" + TestHarness.SERVER_PORT);

    if (args.length >= 8) {
      TestHarness.CLIENT_SPC = Integer.parseInt(args[7]);
    }

    logger.info("CLIENT_SPC=" + TestHarness.CLIENT_SPC);

    if (args.length >= 9) {
      TestHarness.SERVER_SPC = Integer.parseInt(args[8]);
    }

    logger.info("SERVER_SPC=" + TestHarness.SERVER_SPC);

    if (args.length >= 10) {
      TestHarness.NETWORK_INDICATOR = Integer.parseInt(args[9]);
    }

    logger.info("NETWORK_INDICATOR=" + TestHarness.NETWORK_INDICATOR);

    if (args.length >= 11) {
      TestHarness.SERVICE_INDICATOR = Integer.parseInt(args[10]);
    }

    logger.info("SERVICE_INDICATOR=" + TestHarness.SERVICE_INDICATOR);

    if (args.length >= 12) {
      TestHarness.CLIENT_SSN = Integer.parseInt(args[11]);
    }

    logger.info("SSN=" + TestHarness.CLIENT_SSN);

    if (args.length >= 13) {
      TestHarness.ROUTING_CONTEXT = Integer.parseInt(args[12]);
    }

    logger.info("ROUTING_CONTEXT=" + TestHarness.ROUTING_CONTEXT);

    if (args.length >= 14) {
      TestHarness.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = Integer.parseInt(args[13]);
    }

    logger.info("DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT=" + TestHarness.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);

    /*
     * logger.info("Number of calls to be completed = " + noOfCalls + " Number of concurrent calls to be maintained = " +
     * noOfConcurrentCalls);
     */

    NDIALOGS = noOfCalls;

    logger.info("NDIALOGS=" + NDIALOGS);

    MAXCONCURRENTDIALOGS = noOfConcurrentCalls;

    logger.info("MAXCONCURRENTDIALOGS=" + MAXCONCURRENTDIALOGS);

    final Client client = new Client();

    try {
      client.initializeStack(ipChannelType);

      Thread.sleep(20000);

      while (client.endCount < NDIALOGS) {
        /*
         * while (client.nbConcurrentDialogs.intValue() >= MAXCONCURRENTDIALOGS) {
         *
         * logger.warn("Number of concurrent MAP dialog's = " + client.nbConcurrentDialogs.intValue() +
         * " Waiting for max dialog count to go down!");
         *
         * synchronized (client) { try { client.wait(); } catch (Exception ex) { } } }// end of while
         * (client.nbConcurrentDialogs.intValue() >= MAXCONCURRENTDIALOGS)
         */

        if (client.endCount < 0) {
          client.start = System.currentTimeMillis();
          client.prev = client.start;
          // logger.warn("StartTime = " + client.start);
        }

        client.initiateMapAti();
        client.initiateMapSRIforLCS();
        client.initiateMapPSL();
        client.initiateMapSLR();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initiateMapAti() throws MAPException {
    try {
      NetworkIdState networkIdState = this.mapStack.getMAPProvider().getNetworkIdState(0);
      if (!(networkIdState == null || networkIdState.isAvailavle() && networkIdState.getCongLevel() == 0)) {
        // congestion or unavailable
        logger.warn("Outgoing congestion control: MAP load test client: networkIdState=" + networkIdState);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      this.rateLimiterObj.acquire();

      // First create Dialog
      AddressString origRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
      AddressString destRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");
      MAPDialogMobility mapDialogMobility = this.mapProvider.getMAPServiceMobility()
          .createNewDialog(
              MAPApplicationContext.getInstance(MAPApplicationContextName.anyTimeEnquiryContext,
                  MAPApplicationContextVersion.version3),
              SCCP_CLIENT_ADDRESS, origRef, SCCP_SERVER_ADDRESS, destRef);

      // Then, create parameters for concerning MAP operation
      ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "3797554321");
      SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);

      boolean locationInformation = true;
      boolean subscriberState = true;
      MAPExtensionContainer mapExtensionContainer = null;
      boolean currentLocation = false;
      DomainType requestedDomain = null;
      boolean imei = false;
      boolean msClassmark = false;
      boolean mnpRequestedInfo = false;
      RequestedInfo requestedInfo = new RequestedInfoImpl(locationInformation, subscriberState, mapExtensionContainer,
          currentLocation, requestedDomain, imei, msClassmark, mnpRequestedInfo);
      // requestedInfo (MAP ATI): last known location and state (idle or busy), no IMEI/MS Classmark/MNP

      ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");

      mapDialogMobility.addAnyTimeInterrogationRequest(msisdn, requestedInfo, gsmSCFAddress, mapExtensionContainer);
      logger.info("ATI msisdn:" + msisdn + ", requestedInfo: " + requestedInfo + ", atiIsdnAddress:" + gsmSCFAddress);

      // This will initiate the TC-BEGIN with INVOKE component
      mapDialogMobility.send();

    } catch (MAPException e) {
      logger.error(String.format("Error while sending MAP ATI:" + e));
    }

  }

  private void initiateMapSRIforLCS() throws MAPException {
    /*
        3GPP TS 29.002 MAP Specification v15.0.0 (2017-03-15)

        13A.1	MAP-SEND-ROUTING-INFO-FOR-LCS Service

        13A.1.1	Definition
            This service is used between the GMLC and the HLR to retrieve the routing information needed for routing a location service request
            to the servicing VMSC, SGSN, MME or 3GPP AAA server. The MAP-SEND-ROUTING-INFO-FOR-LCS is a confirmed service using the primitives
            from table 13A.1/1.

        13A.1.2	Service Primitives

        Table 13A.1/1: MAP-SEND-ROUTING-INFO-FOR-LCS
            Parameter name              Request     Indication  Response    Confirm
            Invoke Id                      M           M(=)        M(=)       M(=)
            MLC Number                     M           M(=)
            MSISDN                         C           C(=)        C          C(=)
            IMSI                           C           C(=)        C          C(=)
            LMSI                                                   C          C(=)
            Network Node Number                                    C          C(=)
            GPRS Node Indicator                                    C          C(=)
            Additional Number                                      C          C(=)
            Supported LCS Capability Sets                          C          C(=)
            Additional LCS Capability Sets                         C          C(=)
            MME Name                                               C          C(=)
            SGSN Name                                              C          C(=)
            AAA Server Name                                        C          C(=)
            V-GMLC Address                                         U          C(=)
            Additional V-GMLC Address                              U          C(=)
            H-GMLC Address                                         C          C(=)
            PPR Address                                            U          C(=)
            User error                                             C          C(=)
            Provider error                                                    O
    */

    try {
      NetworkIdState networkIdState = this.mapStack.getMAPProvider().getNetworkIdState(0);
      if (!(networkIdState == null || networkIdState.isAvailavle() && networkIdState.getCongLevel() == 0)) {
        // congestion or unavailable
        logger.warn("Outgoing congestion control: MAP load test client: networkIdState=" + networkIdState);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      this.rateLimiterObj.acquire();

      // First create Dialog
      AddressString origRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
      AddressString destRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");
      MAPDialogLsm mapDialogLsm = mapProvider.getMAPServiceLsm()
          .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.locationSvcGatewayContext,
              MAPApplicationContextVersion.version3),
              SCCP_CLIENT_ADDRESS, origRef, SCCP_SERVER_ADDRESS, destRef);

      // Then, create parameters for concerning MAP operation
      ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "3797554321");
      SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);

      ISDNAddressString gmlcAddress = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");
/*
            long[] oid = {0, 0, 17, 773, 1, 1, 1};
            String pExtData = "1144";
            byte[] privateExtData = pExtData.getBytes();
            MAPPrivateExtension mapPrivateExtension = new MAPPrivateExtensionImpl(oid, privateExtData);
            ArrayList<MAPPrivateExtension> mapPrivateExtensions = new ArrayList<MAPPrivateExtension>();
            mapPrivateExtensions.add(mapPrivateExtension);
            String pcsExt = "1033";
            byte[] pcsExtensions = pcsExt.getBytes();
            MAPExtensionContainer mapExtensionContainer = new MAPExtensionContainerImpl(mapPrivateExtensions, pcsExtensions);
*/
      MAPExtensionContainer mapExtensionContainer = null;

      mapDialogLsm.addSendRoutingInfoForLCSRequest(gmlcAddress, msisdn, mapExtensionContainer);
      logger.info("SRIforLCS msisdn:" + msisdn + ", sriForLCSIsdnAddress:" + gmlcAddress);

      // This will initiate the TC-BEGIN with INVOKE component
      mapDialogLsm.send();

    } catch (MAPException e) {
      logger.error(String.format("Error while sending MAP SRIforLCS:" + e));
    }

  }

  private void initiateMapPSL() throws MAPException {
    /*
        3GPP TS 29.002 MAP Specification v15.0.0 (2017-03-15)

        13A.2   MAP-PROVIDE-SUBSCRIBER-LOCATION Service

        13A.2.1 Definition
            This service is used by a GMLC to request the location of a target MS from the visited MSC or SGSN at any time.
            This is a confirmed service using the primitives from table 13A.2/1.

        13A.2.2 Service Primitives

        Table 13A.2/1: Provide_Subscriber_Location
            Parameter name                              Request         Indication        Response          Confirm
            Invoke id                                       M               M(=)            M(=)               M(=)
            Location Type                                   M               M(=)
            MLC Number                                      M               M(=)
            LCS Client ID                                   M               M(=)
            Privacy Override                                U               C(=)
            IMSI                                            C               C(=)
            MSISDN                                          C               C(=)
            LMSI                                            C               C(=)
            LCS Priority                                    C               C(=)
            LCS QoS                                         C               C(=)
            IMEI                                            U               C(=)
            Supported GAD Shapes                            C               C(=)
            LCS-Reference Number                            C               C(=)
            LCS Codeword                                    C               C(=)
            LCS Service Type Id                             C               C(=)
            LCS Privacy Check                               C               C(=)
            Area Event Info                                 C               C(=)
            H-GMLC Address                                  C               C(=)
            Reporting PLMN List                             C               C(=)
            PeriodicLDRInfo                                 C               C(=)
            MO-LR Short Circuit Indicator                   C               C(=)            C                   C(=)
            Location Estimate                                                               M                   M(=)
            GERAN Positioning Data                                                          C                   C(=)
            UTRAN Positioning Data                                                          C                   C(=)
            GERAN GANSS Positioning Data                                                    C                   C(=)
            UTRAN GANSS Positioning Data                                                    C                   C(=)
            UTRAN Additional Positioning Data                                               C                   C(=)
            UTRAN Barometric Pressure Measurement                                           C                   C(=)
            UTRAN Civic Address                                                             C                   C(=)
            Age of Location Estimate                                                        C                   C(=)
            Additional Location Estimate                                                    C                   C(=)
            Deferred MT-LR Response Indicator                                               C                   C(=)
            Cell Id Or SAI                                                                  C                   C(=)
            Accuracy Fulfilment Indicator                                                   C                   C(=)
            Target Serving Node for Handover                                                C                   C(=)
            User error                                                                      C                   C(=)
            Provider error                                                                                      O
    */

    try {
      NetworkIdState networkIdState = this.mapStack.getMAPProvider().getNetworkIdState(0);
      if (!(networkIdState == null || networkIdState.isAvailavle() && networkIdState.getCongLevel() == 0)) {
        // congestion or unavailable
        logger.warn("Outgoing congestion control: MAP load test client: networkIdState=" + networkIdState);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      this.rateLimiterObj.acquire();

      // First create Dialog
      AddressString origRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
      AddressString destRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");
      MAPDialogLsm mapDialogLsm = mapProvider.getMAPServiceLsm()
          .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.locationSvcEnquiryContext,
              MAPApplicationContextVersion.version3),
              SCCP_CLIENT_ADDRESS, origRef, SCCP_SERVER_ADDRESS, destRef);

      // Then, create parameters for concerning MAP operation
      ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "3797554321");
      // SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);
      ISDNAddressString mlcNumber = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");
      LocationEstimateType locationEstimateType = currentLocation;
      // public enum LocationEstimateType {currentLocation(0), currentOrLastKnownLocation(1), initialLocation(2),
      //                                   activateDeferredLocation(3), cancelDeferredLocation(4)..
      DeferredLocationEventType deferredLocationEventType = new DeferredLocationEventTypeImpl();
      deferredLocationEventType.getEnteringIntoArea();
      // DeferredLocationEventType: boolean getMsAvailable(); getEnteringIntoArea(); getLeavingFromArea(); getBeingInsideArea();
      LocationType locationType = new LocationTypeImpl(locationEstimateType, deferredLocationEventType);
      ISDNAddressString externalAddress = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "340444567");
      MAPExtensionContainer mapExtensionContainer = null;
      LCSClientExternalID lcsClientExternalID = new LCSClientExternalIDImpl(externalAddress, mapExtensionContainer);
      LCSClientInternalID lcsClientInternalID = LCSClientInternalID.anonymousLocation;
      String clientName = "340012";
      int cbsDataCodingSchemeCode = 15;
      CBSDataCodingScheme cbsDataCodingScheme = new CBSDataCodingSchemeImpl(cbsDataCodingSchemeCode);
      String ussdLcsString = "*911#";
      Charset gsm8Charset = Charset.defaultCharset();
      USSDString ussdString = new USSDStringImpl(ussdLcsString, cbsDataCodingScheme, gsm8Charset);
      LCSFormatIndicator lcsFormatIndicator = LCSFormatIndicator.url;
      LCSClientName lcsClientName = new LCSClientNameImpl(cbsDataCodingScheme, ussdString, lcsFormatIndicator);
      AddressString lcsClientDialedByMS = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, clientName);
      byte[] apn = new BigInteger("8877665544", 16).toByteArray();
      APN lcsAPN = new APNImpl(apn);
      LCSClientID lcsClientID = new LCSClientIDImpl(LCSClientType.valueAddedServices, lcsClientExternalID, lcsClientInternalID, lcsClientName, lcsClientDialedByMS, lcsAPN, null);
      Boolean privacyOverride = false;
      IMSI imsi = new IMSIImpl("124356871012345");
      String lmsiId = "09876543";
      byte[] lmsid = lmsiId.getBytes();
      LMSI lmsi = new LMSIImpl(lmsid);
      IMEI imei = new IMEIImpl("01171400466105");
      LCSPriority lcsPriority = LCSPriority.normalPriority;
      boolean ellipsoidPoint = true;
      boolean ellipsoidPointWithUncertaintyCircle = true;
      boolean ellipsoidPointWithUncertaintyEllipse = true;
      boolean polygon = false;
      boolean ellipsoidPointWithAltitude = false;
      boolean ellipsoidPointWithAltitudeAndUncertaintyElipsoid = false;
      boolean ellipsoidArc = false;
      SupportedGADShapes supportedGADShapes = new SupportedGADShapesImpl(ellipsoidPoint, ellipsoidPointWithUncertaintyCircle,
          ellipsoidPointWithUncertaintyEllipse, polygon, ellipsoidPointWithAltitude, ellipsoidPointWithAltitudeAndUncertaintyElipsoid, ellipsoidArc);
      Integer lcsReferenceNumber = 379;
      Integer lcsServiceTypeID = 1;
      // DataCodingScheme codingScheme = new DataCodingSchemeImpl(1);
      USSDString lcsCodewordString = new USSDStringImpl(ussdLcsString, cbsDataCodingScheme, gsm8Charset);
      LCSCodeword lcsCodeword = new LCSCodewordImpl(cbsDataCodingScheme, lcsCodewordString);
      Integer horizontalAccuracy = 100;
      Integer verticalAccuracy = 1000;
      boolean verticalCoordinateRequest = false;
      ResponseTimeCategory responseTimeCategory = ResponseTimeCategory.delaytolerant;
      ResponseTime responseTime = new ResponseTimeImpl(responseTimeCategory);
/*
            long[] oid = {0, 0, 17, 773, 1, 1, 1};
            String pExtData = "1144";
            byte[] privateExtData = pExtData.getBytes();
            MAPPrivateExtension mapPrivateExtension = new MAPPrivateExtensionImpl(oid, privateExtData);
            ArrayList<MAPPrivateExtension> mapPrivateExtensions = new ArrayList<MAPPrivateExtension>();
            mapPrivateExtensions.add(mapPrivateExtension);
            String pcsExt = "1033";
            byte[] pcsExtensions = pcsExt.getBytes();
            MAPExtensionContainer mapExtensionContainer = new MAPExtensionContainerImpl(mapPrivateExtensions, pcsExtensions);
*/
      MAPExtensionContainer extensionContainer = null;
      LCSQoS lcsQoS = new LCSQoSImpl(horizontalAccuracy, verticalAccuracy, verticalCoordinateRequest, responseTime, extensionContainer);
      PrivacyCheckRelatedAction callSessionUnrelated = PrivacyCheckRelatedAction.allowedWithNotification;
      PrivacyCheckRelatedAction callSessionRelated = PrivacyCheckRelatedAction.allowedIfNoResponse;
      LCSPrivacyCheck lcsPrivacyCheck = new LCSPrivacyCheckImpl(callSessionUnrelated, callSessionRelated);
      ArrayList<Area> areaList = new ArrayList<Area>();
      AreaType areaType = AreaType.locationAreaId;
      String aId = "102132";
      byte[] areaId = aId.getBytes();
      AreaIdentification areaIdentification = new AreaIdentificationImpl(areaId);
      Area area1 = new AreaImpl(areaType, areaIdentification);
      areaList.add(area1);
      AreaDefinition areaDefinition = new AreaDefinitionImpl(areaList);
      OccurrenceInfo occurrenceInfo = OccurrenceInfo.oneTimeEvent;
      Integer intervalTime = 10;
      AreaEventInfo areaEventInfo = new AreaEventInfoImpl(areaDefinition, occurrenceInfo, intervalTime);
      byte[] homeGmlcAddress = new BigInteger("999988887777", 16).toByteArray();
      GSNAddress hGmlcAddress = new GSNAddressImpl(homeGmlcAddress);
      boolean b1 = false;
      int reportingAmount = 3;
      int reportingInterval = 60;
      PeriodicLDRInfo periodicLDRInfo = new PeriodicLDRInfoImpl(reportingAmount, reportingInterval);
      boolean plmnListPrioritized = false;
      ArrayList<ReportingPLMN> plmnList = new ArrayList<ReportingPLMN>();
      String plmnIdstr = "885577";
      byte[] plmnID = plmnIdstr.getBytes();
      PlmnId plmnId = new PlmnIdImpl(plmnID);
      RANTechnology ranTechnology = RANTechnology.umts;
      boolean ranPeriodicLocationSupport = true;
      ReportingPLMN reportingPLMN = new ReportingPLMNImpl(plmnId, ranTechnology, ranPeriodicLocationSupport);
      plmnList.add(reportingPLMN);
      ReportingPLMNList reportingPLMNList = new ReportingPLMNListImpl(plmnListPrioritized, plmnList);

      mapDialogLsm.addProvideSubscriberLocationRequest(locationType, mlcNumber, lcsClientID, privacyOverride, imsi, msisdn, lmsi, imei, lcsPriority,
          lcsQoS, extensionContainer, supportedGADShapes, lcsReferenceNumber, lcsServiceTypeID, lcsCodeword,
          lcsPrivacyCheck, areaEventInfo, hGmlcAddress, b1, periodicLDRInfo, reportingPLMNList);
      logger.info("MAP PSL: msisdn:" + msisdn + ", MLC Number:" + mlcNumber);

      // This will initiate the TC-BEGIN with INVOKE component
      mapDialogLsm.send();

    } catch (MAPException e) {
      logger.error(String.format("Error while sending MAP PSL:" + e));
    }

  }

  private void initiateMapSLR() throws MAPException {
    /*
        3GPP TS 29.002 MAP Specification v15.0.0 (2017-03-15)

        13A.3   MAP-SUBSCRIBER-LOCATION-REPORT Service

        13A.3.1 Definition
            This service is used by a VMSC or SGSN to provide the location of a target MS to a GMLC
            when a request for location is either implicitly administered or made at some   earlier time.
            This is a confirmed service using the primitives from table 13A.3/1.

        Table 13A.3/1: Subscriber_Location_Report
            Parameter name                                      Request         Indication       Response     Confirm
            Invoke id                                               M               M(=)            M(=)        M(=)
            LCS Event                                               M               M(=)
            LCS Client ID                                           M               M(=)
            Network Node Number                                     M               M(=)
            IMSI                                                    C               C(=)
            MSISDN                                                  C               C(=)
            NA-ESRD                                                 C               C(=)            C           C(=)
            NA-ESRK                                                 C               C(=)            C           C(=)
            IMEI                                                    U               C(=)
            Location Estimate                                       C               C(=)
            GERAN Positioning Data                                  C               C(=)
            UTRAN Positioning Data                                  C               C(=)
            GERAN GANSS Positioning Data                            C               C(=)
            UTRAN GANSS Positioning Data                            C               C(=)
            UTRAN Additional Positioning Data                       C               C(=)
            UTRAN Barometric Pressure Measurement                   C               C(=)
            UTRAN Civic Address                                     C               C(=)
            Age of Location Estimate                                C               C(=)
            LMSI                                                    U               C(=)
            GPRS Node Indicator                                     C               C(=)
            Additional Location Estimate                            C               C(=)
            Deferred MT-LR Data                                     C               C(=)
            LCS-Reference Number                                    C               C(=)            C           C(=)
            NA-ESRK Request                                         C               C(=)
            Cell Id Or SAI                                          C               C(=)
            H-GMLC Address                                          C               C(=)            C           C(=)
            LCS Service Type Id                                     C               C(=)
            Pseudonym Indicator                                     C               C(=)
            Accuracy Fulfilment Indicator                           C               C(=)
            Sequence Number                                         C               C(=)
            Periodic LDR Info                                       C               C(=)
            MO-LR Short Circuit Indicator                           C               C(=)            C           C(=)
            Target Serving Node for Handover                        C               C(=)
            Reporting PLMN List                                                                     C           C(=)
            User error                                                                              C           C(=)
            Provider error                                                                                      O
    */
    try {
      NetworkIdState networkIdState = this.mapStack.getMAPProvider().getNetworkIdState(0);
      if (!(networkIdState == null || networkIdState.isAvailavle() && networkIdState.getCongLevel() == 0)) {
        // congestion or unavailable
        logger.warn("Outgoing congestion control: MAP load test client: networkIdState=" + networkIdState);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      this.rateLimiterObj.acquire();

      // First create Dialog
      AddressString origRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
      AddressString destRef = this.mapProvider.getMAPParameterFactory()
          .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");
      MAPDialogLsm mapDialogLsm = mapProvider.getMAPServiceLsm()
          .createNewDialog(MAPApplicationContext.getInstance(MAPApplicationContextName.locationSvcEnquiryContext,
              MAPApplicationContextVersion.version3),
              SCCP_CLIENT_ADDRESS, origRef, SCCP_SERVER_ADDRESS, destRef);

      // Then, create parameters for concerning MAP operation
      ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "3797554321");
      // SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);
      ISDNAddressString mscNumber = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "598990192837");
      ISDNAddressString sgsnNumber = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "598990192837");
      ISDNAddressString naEsrd = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "1110101");
      ISDNAddressString naEsrk = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "9889277");
      LCSEvent lcsEvent = emergencyCallOrigination;
      // LCS-Event ::= ENUMERATED { emergencyCallOrigination (0), emergencyCallRelease (1), mo-lr (2), ..., deferredmt-lrResponse (3) }
      // -- exception handling: --
      // a SubscriberLocationReport-Arg containing an unrecognized LCS-Event -- shall be rejected by a receiver with a return error cause of unexpected data value
      LocationEstimateType locationEstimateType = currentLocation;
      // public enum LocationEstimateType {currentLocation(0), currentOrLastKnownLocation(1), initialLocation(2), activateDeferredLocation(3),
      //                                   cancelDeferredLocation(4)..
      ISDNAddressString externalAddress = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "444567");
/*
            long[] oid = {0, 0, 17, 773, 1, 1, 1};
            byte[] privateExtData = hexStringToByteArray("1144");
            MAPPrivateExtension mapPrivateExtension = new MAPPrivateExtensionImpl(oid, privateExtData);
            ArrayList<MAPPrivateExtension> mapPrivateExtensions = new ArrayList<MAPPrivateExtension>();
            mapPrivateExtensions.add(mapPrivateExtension);
            byte[] pcsExtensions = hexStringToByteArray("1033");
            MAPExtensionContainer mapExtensionContainer = new MAPExtensionContainerImpl(mapPrivateExtensions, pcsExtensions);
*/
      MAPExtensionContainer mapExtensionContainer = null;
      LCSClientExternalID lcsClientExternalID = new LCSClientExternalIDImpl(externalAddress, mapExtensionContainer);
      LCSClientInternalID lcsClientInternalID = LCSClientInternalID.broadcastService;
      String clientName = "219023";
      int cbsDataCodingSchemeCode = 15;
      CBSDataCodingScheme cbsDataCodingScheme = new CBSDataCodingSchemeImpl(cbsDataCodingSchemeCode);
      String ussdLcsString = "911";
      Charset gsm8Charset = Charset.defaultCharset();
      USSDString ussdString = new USSDStringImpl(ussdLcsString, cbsDataCodingScheme, gsm8Charset);
      LCSFormatIndicator lcsFormatIndicator = LCSFormatIndicator.url;
      LCSClientName lcsClientName = new LCSClientNameImpl(cbsDataCodingScheme, ussdString, lcsFormatIndicator);
      AddressString lcsClientDialedByMS = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, clientName);
      byte[] apn = new BigInteger("6763a20b0890", 16).toByteArray();
      APN lcsAPN = new APNImpl(apn);
      LCSClientID lcsClientID = new LCSClientIDImpl(LCSClientType.valueAddedServices, lcsClientExternalID, lcsClientInternalID, lcsClientName, lcsClientDialedByMS, lcsAPN, null);
      ISDNAddressString networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number,
          org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5983392892");
      String lmsiStr = "0a010203";
      byte[] lmsid = lmsiStr.getBytes();
      LMSI lmsi = new LMSIImpl(lmsid);
      boolean gprsNodeIndicator = true;
      AdditionalNumber additionalNumber = new AdditionalNumberImpl(null, sgsnNumber);
      boolean lcsCapabilitySetRelease98_99 = true;
      boolean lcsCapabilitySetRelease4 = true;
      boolean lcsCapabilitySetRelease5 = true;
      boolean lcsCapabilitySetRelease6 = true;
      boolean lcsCapabilitySetRelease7 = false;
      SupportedLCSCapabilitySets supportedLCSCapabilitySets = new SupportedLCSCapabilitySetsImpl(lcsCapabilitySetRelease98_99, lcsCapabilitySetRelease4,
          lcsCapabilitySetRelease5, lcsCapabilitySetRelease6, lcsCapabilitySetRelease7);
      lcsCapabilitySetRelease7 = true;
      SupportedLCSCapabilitySets additionalLCSCapabilitySets = new SupportedLCSCapabilitySetsImpl(lcsCapabilitySetRelease98_99, lcsCapabilitySetRelease4,
          lcsCapabilitySetRelease5, lcsCapabilitySetRelease6, lcsCapabilitySetRelease7);
      byte[] mme = new BigInteger("8720c00a30a1743401000a", 16).toByteArray();
      DiameterIdentity mmeName = new DiameterIdentityImpl(mme);
      byte[] aaa = new BigInteger("8720c00a30a1743401101112", 16).toByteArray();
      DiameterIdentity aaaServerName = new DiameterIdentityImpl(aaa);
      LCSLocationInfo lcsLocationInfo = new LCSLocationInfoImpl(networkNodeNumber, lmsi, mapExtensionContainer, gprsNodeIndicator, additionalNumber,
          supportedLCSCapabilitySets, additionalLCSCapabilitySets, mmeName, aaaServerName);
      IMSI imsi = new IMSIImpl("748031234567890");
      IMEI imei = new IMEIImpl("011714004661057");
      Integer ageOfLocationEstimate = 0;
      TypeOfShape typeOfShape = TypeOfShape.EllipsoidArc;
      Double latitude = 34.789123;
      Double longitude = -124.902033;
      Double uncertainty = 100.0;
      Double uncertaintySemiMajorAxis = 40.0;
      Double uncertaintySemiMinorAxis = 20.0;
      Double angleOfMajorAxis = 30.0;
      int confidence = 0;
      int altitude = 1500;
      Double uncertaintyAltitude = 500.0;
      int innerRadius = 5;
      Double uncertaintyRadius = 1.50;
      Double offsetAngle = 20.0;
      Double includedAngle = 20.0;
      ExtGeographicalInformation extGeographicalInformation = new ExtGeographicalInformationImpl(typeOfShape, latitude, longitude, uncertainty, uncertaintySemiMajorAxis,
          uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude, innerRadius, uncertaintyRadius, offsetAngle, includedAngle);
      SLRArgExtensionContainer slrArgExtensionContainer = null;
      byte[] addLocationEstimate = new BigInteger("8090009002", 16).toByteArray();
      AddGeographicalInformation addGeographicalInformation = new AddGeographicalInformationImpl(addLocationEstimate);
      boolean msAvailable = false;
      boolean enteringIntoArea = true;
      boolean leavingFromArea = true;
      boolean beingInsideArea = true;
      DeferredLocationEventType deferredLocationEventType = new DeferredLocationEventTypeImpl(msAvailable, enteringIntoArea, leavingFromArea, beingInsideArea);
      TerminationCause terminationCause = TerminationCause.congestion;
      DeferredmtlrData deferredmtlrData = new DeferredmtlrDataImpl(deferredLocationEventType, terminationCause, lcsLocationInfo);
      byte[] data = new BigInteger("123456789012", 16).toByteArray();
      PositioningDataInformation positioningDataInformation = new PositioningDataInformationImpl(data);
      byte[] utranData = new BigInteger("00000000004c21", 16).toByteArray();
      UtranPositioningDataInfo utranPositioningDataInfo = new UtranPositioningDataInfoImpl(utranData);
      Integer lcsServiceTypeID = 1;
      boolean saiPresent = true;
      Boolean pseudonymIndicator = false;
      AccuracyFulfilmentIndicator accuracyFulfilmentIndicator = AccuracyFulfilmentIndicator.requestedAccuracyNotFulfilled;
      byte[] velEstimate = new BigInteger("4123567890", 16).toByteArray();
      VelocityEstimate velocityEstimate = new VelocityEstimateImpl(velEstimate);
      int reportingAmount = 3;
      int reportingInterval = 600;
      PeriodicLDRInfo periodicLDRInfo = new PeriodicLDRInfoImpl(reportingAmount, reportingInterval);
      boolean b2 = false;
      byte[] cidOrSaiFixedLength = new BigInteger("349a0120b04321", 16).toByteArray();
      CellGlobalIdOrServiceAreaIdFixedLength cellGlobalIdOrServiceAreaIdFixedLength = new CellGlobalIdOrServiceAreaIdFixedLengthImpl(cidOrSaiFixedLength);
      CellGlobalIdOrServiceAreaIdOrLAI cellIdOrSai = new CellGlobalIdOrServiceAreaIdOrLAIImpl(cellGlobalIdOrServiceAreaIdFixedLength);
      byte[] gGanss = new BigInteger("666601019999", 16).toByteArray();
      GeranGANSSpositioningData geranGanssPositioningData = new GeranGANSSpositioningDataImpl(gGanss);
      byte[] uGanss = new BigInteger("777701019898", 16).toByteArray();
      UtranGANSSpositioningData utranGanssPositioningData = new UtranGANSSpositioningDataImpl(uGanss);
      boolean isMsc = true;
      ServingNodeAddress servingNodeAddress = new ServingNodeAddressImpl(networkNodeNumber, isMsc);
      Integer lcsReferenceNumber = 379;
      Integer integer3 = 0;
      byte[] homeGmlcAddress = new BigInteger("3734383439323337", 16).toByteArray();
      GSNAddress hGmlcAddress = new GSNAddressImpl(homeGmlcAddress);

      mapDialogLsm.addSubscriberLocationReportRequest(lcsEvent, lcsClientID, lcsLocationInfo, msisdn, imsi, imei, naEsrd, naEsrk, extGeographicalInformation,
          ageOfLocationEstimate, slrArgExtensionContainer, addGeographicalInformation, deferredmtlrData,
          lcsReferenceNumber, positioningDataInformation, utranPositioningDataInfo, cellIdOrSai, hGmlcAddress,
          lcsServiceTypeID, saiPresent, pseudonymIndicator, accuracyFulfilmentIndicator, velocityEstimate,
          integer3, periodicLDRInfo, b2, geranGanssPositioningData, utranGanssPositioningData, servingNodeAddress);

      logger.info("MAP SLR: msisdn:" + msisdn + ", LCSEvent:" + lcsEvent);
      // This will initiate the TC-BEGIN with INVOKE component
      mapDialogLsm.send();

    } catch (MAPException e) {
      logger.error(String.format("Error while sending MAP SLR:" + e));
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest atiReq) {
    /*
     * This is an error condition. Client should never receive onAnyTimeInterrogationRequest.
     */
    logger.error(String.format("onAnyTimeInterrogationRequest for Dialog=%d and invokeId=%d",
        atiReq.getMAPDialog().getLocalDialogId(), atiReq.getInvokeId()));

  }

  @Override
  public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse atiResp) {

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onAnyTimeInterrogationResponse  for DialogId=%d",
          atiResp.getMAPDialog().getLocalDialogId()));
    } else {
      logger.info(String.format("onAnyTimeInterrogationResponse  for DialogId=%d",
          atiResp.getMAPDialog().getLocalDialogId()));
    }

    try {

      SubscriberInfo si = atiResp.getSubscriberInfo();

      if (si != null) {

        if (si.getLocationInformation() != null) {

          if (si.getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI() != null) {
            CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = si.getLocationInformation()
                .getCellGlobalIdOrServiceAreaIdOrLAI();

            if (cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength() != null) {
              int mcc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getMCC();
              int mnc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getMNC();
              int lac = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getLac();
              int cellId = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                  .getCellIdOrServiceAreaCode();
              if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                    "Rx onAnyTimeInterrogationResponse:  CI=%d, LAC=%d, MNC=%d, MCC=%d, for DialogId=%d",
                    cellId, lac, mnc, mcc, atiResp.getMAPDialog().getLocalDialogId()));
              } else {
                logger.info(String.format(
                    "Rx onAnyTimeInterrogationResponse:  CI=%d, LAC=%d, MNC=%d, MCC=%d, for DialogId=%d",
                    cellId, lac, mnc, mcc, atiResp.getMAPDialog().getLocalDialogId()));
              }
            }
          }

          if (si.getLocationInformation().getAgeOfLocationInformation() != null) {
            int aol = si.getLocationInformation().getAgeOfLocationInformation().intValue();
            if (logger.isDebugEnabled()) {
              logger.debug(String.format("Rx onAnyTimeInterrogationResponse:  AoL=%d for DialogId=%d", aol,
                  atiResp.getMAPDialog().getLocalDialogId()));
            } else {
              logger.info(String.format("Rx onAnyTimeInterrogationResponse:  AoL=%d for DialogId=%d", aol,
                  atiResp.getMAPDialog().getLocalDialogId()));
            }
          }

          if (si.getLocationInformation().getVlrNumber() != null) {
            String vlrAddress = si.getLocationInformation().getVlrNumber().getAddress();
            if (logger.isDebugEnabled()) {
              logger.debug(String.format("Rx onAnyTimeInterrogationResponse:  VLR address=%s for DialogId=%d",
                  vlrAddress, atiResp.getMAPDialog().getLocalDialogId()));
            } else {
              logger.info(String.format("Rx onAnyTimeInterrogationResponse:  VLR address=%s for DialogId=%d",
                  vlrAddress, atiResp.getMAPDialog().getLocalDialogId()));
            }
          }
        }

        if (si.getSubscriberState() != null) {

          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onAnyTimeInterrogationResponse SubscriberState: "
                + si.getSubscriberState() + "for DialogId=%d", atiResp.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx onAnyTimeInterrogationResponse SubscriberState: "
                + si.getSubscriberState() + "for DialogId=%d", atiResp.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onAnyTimeInterrogationResponse, Incorrect Subscriber State received: " + si + "for DialogId=%d",
                atiResp.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onAnyTimeInterrogationResponse, Incorrect Subscriber State received: " + si + "for DialogId=%d",
                atiResp.getMAPDialog().getLocalDialogId()));
          }
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Bad AnyTimeInterrogationResponse received: " + atiResp + "for DialogId=%d",
              atiResp.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format("Bad AnyTimeInterrogationResponse received: " + atiResp + "for DialogId=%d",
              atiResp.getMAPDialog().getLocalDialogId()));
        }
      }

    } catch (Exception e) {
      logger.error(String.format("Error while processing onAnyTimeInterrogationResponse for Dialog=%d",
          atiResp.getMAPDialog().getLocalDialogId(), ", Exception: " + e.getStackTrace()));

    }
  }

  @Override
  public void onSendRoutingInfoForLCSRequest(SendRoutingInfoForLCSRequest sendRoutingInfoForLCSRequest) {
    /*
     * This is an error condition. Client should never receive onSendRoutingInfoForLCSRequest.
     */
    logger.error(String.format("onSendRoutingInfoForLCSRequest for Dialog=%d and invokeId=%d",
        sendRoutingInfoForLCSRequest.getMAPDialog().getLocalDialogId(), sendRoutingInfoForLCSRequest.getInvokeId()));
  }

  @Override
  public void onSendRoutingInfoForLCSResponse(SendRoutingInfoForLCSResponse sendRoutingInforForLCSResponse) {

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onSendRoutingInfoForLCSResponse  for DialogId=%d",
          sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
    } else {
      logger.info(String.format("onSendRoutingInfoForLCSResponse  for DialogId=%d",
          sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
    }

    try {

      LCSLocationInfo lcsLocationInfo = sendRoutingInforForLCSResponse.getLCSLocationInfo();

      if (lcsLocationInfo != null) {

        if (lcsLocationInfo.getNetworkNodeNumber() != null) {
          String networkNodeNumber = lcsLocationInfo.getNetworkNodeNumber().toString();
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse NetworkNodeNumber = " + networkNodeNumber +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx onSendRoutingInfoForLCSResponse NetworkNodeNumber: "
                + lcsLocationInfo.getNetworkNodeNumber() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect NetworkNodeNumber received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect NetworkNodeNumber received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }

        if (lcsLocationInfo.getLMSI() != null) {
          String lmsi = lcsLocationInfo.getLMSI().toString();
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse LMSI = " + lmsi +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx onSendRoutingInfoForLCSResponse LMSI: "
                + lcsLocationInfo.getLMSI() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect LMSI received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect LMSI received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }

        if (lcsLocationInfo.getSupportedLCSCapabilitySets() != null) {
          String supportedLCSCapabilitySets = lcsLocationInfo.getSupportedLCSCapabilitySets().toString();
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse Supported LCS Capability Sets = " + supportedLCSCapabilitySets +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx onSendRoutingInfoForLCSResponse Supported LCS Capability Sets: "
                + lcsLocationInfo.getSupportedLCSCapabilitySets() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Supported LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Supported LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }

        if (lcsLocationInfo.getAdditionalLCSCapabilitySets() != null) {
          String additionalLCSCapabilitySets = lcsLocationInfo.getAdditionalLCSCapabilitySets().toString();
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse Additional LCS Capability Sets = " + additionalLCSCapabilitySets +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx onSendRoutingInfoForLCSResponse Additional LCS Capability Sets: "
                + lcsLocationInfo.getAdditionalLCSCapabilitySets() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Additional LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Additional LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }

        if (lcsLocationInfo.getAdditionalNumber() != null) {
          String sgsnNumber = lcsLocationInfo.getAdditionalNumber().getSGSNNumber().toString();
          String mscNumber = lcsLocationInfo.getAdditionalNumber().getSGSNNumber().toString();
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx Additional Number onSendRoutingInfoForLCSResponse, " +
                "SGSN Number = " + sgsnNumber + ", MSC Number: " + mscNumber +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format("Rx Additional Number onSendRoutingInfoForLCSResponse, " +
                "SGSN Number: " + sgsnNumber + ", MSC Number: " + mscNumber +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Additional Number received for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect Additional Number received for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }

        if (lcsLocationInfo.getGprsNodeIndicator() != true) {
          String gprsNodeIndicator = "false";
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse GPRS Node Indicator = " + gprsNodeIndicator +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            gprsNodeIndicator = "true";
            logger.info(String.format("Rx onSendRoutingInfoForLCSResponse GPRS Node Indicator = " + gprsNodeIndicator +
                "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect GPRS Node Indicator received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onSendRoutingInfoForLCSResponse, Incorrect GPRS Node Indicator received: " + lcsLocationInfo + "for DialogId=%d",
                sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
          }
        }
      }

    } catch (Exception e) {
      logger.error(String.format("Error while processing onSendRoutingInfoForLCSResponse for Dialog=%d",
          sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
    }
  }

  @Override
  public void onProvideSubscriberLocationRequest(ProvideSubscriberLocationRequest provideSubscriberLocationRequest) {
    /*
     * This is an error condition. Client should never receive onProvideSubscriberLocationRequest.
     */
    logger.error(String.format("onProvideSubscriberLocationRequest for Dialog=%d and invokeId=%d",
        provideSubscriberLocationRequest.getMAPDialog().getLocalDialogId(), provideSubscriberLocationRequest.getInvokeId()));

  }

  @Override
  public void onProvideSubscriberLocationResponse(ProvideSubscriberLocationResponse provideSubscriberLocationResponse) {

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onProvideSubscriberLocationResponse  for DialogId=%d",
          provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
    } else {
      logger.info(String.format("onProvideSubscriberLocationResponse  for DialogId=%d",
          provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
    }

    try {

      if (provideSubscriberLocationResponse.getLocationEstimate() != null) {
        ExtGeographicalInformation locationEstimate = provideSubscriberLocationResponse.getLocationEstimate();
        Double latitude = locationEstimate.getLatitude();
        Double longitude = locationEstimate.getLongitude();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse LocationEstimate, latitude = %d " + latitude + ", longitude: " +
              longitude + "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse LocationEstimate: "
                  + provideSubscriberLocationResponse.getLocationEstimate() + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect LocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect LocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getGeranPositioningData() != null) {
        PositioningDataInformation geranPositioningData = provideSubscriberLocationResponse.getGeranPositioningData();
        String geranPositioning = geranPositioningData.getData().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse GeranPositioningData = %s " + geranPositioning +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse GeranPositioningData: "
                  + provideSubscriberLocationResponse.getGeranPositioningData() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect GeranPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect GeranPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getUtranPositioningData() != null) {
        UtranPositioningDataInfo utranPositioningData = provideSubscriberLocationResponse.getUtranPositioningData();
        String utranPositioning = utranPositioningData.getData().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse UtranPositioningData = %s " + utranPositioning +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse UtranPositioningData: "
                  + provideSubscriberLocationResponse.getUtranPositioningData() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect UtranPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect UtranPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getAgeOfLocationEstimate() != null) {
        Integer ageOfLocationEstimate = provideSubscriberLocationResponse.getAgeOfLocationEstimate();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse AgeOfLocationEstimate = %d " + ageOfLocationEstimate +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse AgeOfLocationEstimate: "
                  + provideSubscriberLocationResponse.getAgeOfLocationEstimate() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AgeOfLocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AgeOfLocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getAdditionalLocationEstimate() != null) {
        AddGeographicalInformation additionalLocationEstimate = provideSubscriberLocationResponse.getAdditionalLocationEstimate();
        Double additionalLatitude = additionalLocationEstimate.getLatitude();
        Double additionalLongitude = additionalLocationEstimate.getLongitude();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse AdditionalLocationEstimate, latitude = %d "
                  + additionalLatitude + ", longitude: " +
                  additionalLongitude + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse AdditionalLocationEstimate: "
                  + provideSubscriberLocationResponse.getAdditionalLocationEstimate() + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AdditionalLocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AdditionalLocationEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getExtensionContainer() != null) {
        MAPExtensionContainer mapExtensionContainer = provideSubscriberLocationResponse.getExtensionContainer();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse MAPExtensionContainer not null" +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse MAPExtensionContainer: "
                  + mapExtensionContainer + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, NULL MAPExtensionContainer received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, NULL MAPExtensionContainer received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() == false ||
          provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() == true) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse DeferredMTLRResponseIndicator: "
                  + provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse DeferredMTLRResponseIndicator: "
                  + provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, INCORRECT DeferredMTLRResponseIndicator = "
                  + provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() + " " +
                  "received for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse, INCORRECT DeferredMTLRResponseIndicator = "
              + provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() + " " +
              "received for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getCellIdOrSai() != null) {
        CellGlobalIdOrServiceAreaIdOrLAI cellIdOrSai = provideSubscriberLocationResponse.getCellIdOrSai();
        String cidOrSai = cellIdOrSai.getCellGlobalIdOrServiceAreaIdFixedLength().toString();
        String laiFixedLength = cellIdOrSai.getLAIFixedLength().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse CellIdOrSai, CellGlobalIdOrServiceAreaIdOrLAI = %s " + cidOrSai +
              ", LAIFixedLength: " + laiFixedLength +
              ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse CellIdOrSai: "
                  + provideSubscriberLocationResponse.getCellIdOrSai() + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect CellIdOrSai received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect CellIdOrSai received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getSaiPresent() == false ||
          provideSubscriberLocationResponse.getSaiPresent() == true) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse SaiPresent: "
                  + provideSubscriberLocationResponse.getSaiPresent() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                "Rx onProvideSubscriberLocationResponse, Incorrect SaiPresent received for DialogId=%d",
                provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
          } else {
            logger.info(String.format(
                "Rx onProvideSubscriberLocationResponse, Incorrect SaiPresent received for DialogId=%d",
                provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
          }
        }
      }

      if (provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator() != null) {
        AccuracyFulfilmentIndicator accuracyFulfilmentIndicator = provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator();
        int indicator = accuracyFulfilmentIndicator.getIndicator();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse AccuracyFulfilmentIndicator, indicator = %d " + indicator +
              ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse AccuracyFulfilmentIndicator: "
                  + provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator() + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AccuracyFulfilmentIndicator received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect AccuracyFulfilmentIndicator received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getVelocityEstimate() != null) {
        VelocityEstimate velocityEstimate = provideSubscriberLocationResponse.getVelocityEstimate();
        long horizontalSpeed = velocityEstimate.getHorizontalSpeed();
        long verticalSpeed = velocityEstimate.getVerticalSpeed();
        int velocityType = velocityEstimate.getVelocityType().getCode();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse VelocityEstimate, horizontal speed = %d " + horizontalSpeed +
              ", vertical speed: " + verticalSpeed + "velocity type: " + velocityType
              + ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse VelocityEstimate: "
                  + provideSubscriberLocationResponse.getVelocityEstimate() + "for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect VelocityEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect VelocityEstimate received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() == false ||
          provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() == true) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse SaiPresent: "
                  + provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse SaiPresent: "
                  + provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect MoLrShortCircuitIndicator received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect MoLrShortCircuitIndicator received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getGeranGANSSpositioningData() != null) {
        GeranGANSSpositioningData geranGANSSpositioningDataPositioningData = provideSubscriberLocationResponse.getGeranGANSSpositioningData();
        String geranGanssPositioning = geranGANSSpositioningDataPositioningData.getData().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse GeranGANSSPositioningData = %s " + geranGanssPositioning +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse GeranGANSSPositioningData: "
                  + provideSubscriberLocationResponse.getGeranGANSSpositioningData() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect GeranGANSSPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect GeranGANSSPositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getUtranGANSSpositioningData() != null) {
        UtranGANSSpositioningData utranGANSSpositioningDataPositioningData = provideSubscriberLocationResponse.getUtranGANSSpositioningData();
        String utranGanssPositioning = utranGANSSpositioningDataPositioningData.getData().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse UtranGANSSpositioningData = %s " + utranGanssPositioning +
              "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse UtranGANSSpositioningData: "
                  + provideSubscriberLocationResponse.getUtranGANSSpositioningData() + ", for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect UtranGANSSpositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect UtranGANSSpositioningData received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (provideSubscriberLocationResponse.getTargetServingNodeForHandover() != null) {
        ServingNodeAddress servingNodeAddress = provideSubscriberLocationResponse.getTargetServingNodeForHandover();
        String mscNumber = servingNodeAddress.getMscNumber().toString();
        String sgsnNumber = servingNodeAddress.getSgsnNumber().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onProvideSubscriberLocationResponse ServingNode, MSC Number = %s " + mscNumber +
              ", SGSN Number = %s " + sgsnNumber +
              ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onProvideSubscriberLocationResponse ServingNode, MSC Number = %s " + mscNumber +
              ", SGSN Number = %s " + sgsnNumber +
              ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect ServingNode received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onProvideSubscriberLocationResponse, Incorrect ServingNode received for DialogId=%d",
              provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
        }
      }

    } catch (Exception e) {
      logger.info(String.format("onProvideSubscriberLocationResponse for Dialog=%d",
          provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId(), ", thrown: " + e.getStackTrace()));
    }
  }

  @Override
  public void onSubscriberLocationReportRequest(SubscriberLocationReportRequest subscriberLocationReportRequest) {
    /*
     * This is an error condition. Client should never receive onSubscriberLocationReportRequest.
     */
    logger.error(String.format("onProvideSubscriberLocationRequest for Dialog=%d and invokeId=%d",
        subscriberLocationReportRequest.getMAPDialog().getLocalDialogId(), subscriberLocationReportRequest.getInvokeId()));
  }

  @Override
  public void onSubscriberLocationReportResponse(SubscriberLocationReportResponse subscriberLocationReportResponse) {

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onSubscriberLocationReportResponse  for DialogId=%d",
          subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
    } else {
      logger.info(String.format("onSubscriberLocationReportResponse  for DialogId=%d",
          subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
    }

    try {

      if (subscriberLocationReportResponse.getNaESRD() != null) {
        ISDNAddressString naEsrd = subscriberLocationReportResponse.getNaESRD();
        String naESRDaddress = naEsrd.getAddress();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onSubscriberLocationReportResponse NaESRD = %s " + naESRDaddress +
              "for DialogId=%d", subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onSubscriberLocationReportResponse NaESRD: "
                  + subscriberLocationReportResponse.getNaESRD() + ", for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect NaESRD received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect NaESRD received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (subscriberLocationReportResponse.getNaESRK() != null) {
        ISDNAddressString naEsrk = subscriberLocationReportResponse.getNaESRD();
        String naESRKaddress = naEsrk.getAddress();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onSubscriberLocationReportResponse NaESRK = %s " + naESRKaddress +
              "for DialogId=%d", subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onSubscriberLocationReportResponse NaESRK: "
                  + subscriberLocationReportResponse.getNaESRK() + ", for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect NaESRK received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect NaESRK received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      }

      if (subscriberLocationReportResponse.getExtensionContainer() != null) {
        MAPExtensionContainer extContainer = subscriberLocationReportResponse.getExtensionContainer();
        String mapExtensionContainer = extContainer.toString();
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Rx onSubscriberLocationReportResponse MAPExtensionContainer = %s " + mapExtensionContainer +
              "for DialogId=%d", subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));

        } else {
          logger.info(String.format("Rx onSubscriberLocationReportResponse MAPExtensionContainer: "
                  + subscriberLocationReportResponse.getExtensionContainer() + ", for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect MAPExtensionContainer received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        } else {
          logger.info(String.format(
              "Rx onSubscriberLocationReportResponse, Incorrect MAPExtensionContainer received for DialogId=%d",
              subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }
      }

    } catch (Exception e) {
      logger.error(String.format("Error while processing onSubscriberLocationReportResponse for Dialog=%d",
          subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPServiceListener#onErrorComponent(org.mobicents.protocols.ss7.map.api.MAPDialog, java.lang.Long,
   * org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage)
   */
  // @Override
  // public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
  // logger.error(String.format("onErrorComponent for Dialog=%d and invokeId=%d MAPErrorMessage=%s",
  // mapDialog.getLocalDialogId(), invokeId, mapErrorMessage));
  // }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPServiceListener#onRejectComponent
   * (org.mobicents.protocols.ss7.map.api.MAPDialog, java.lang.Long, org.mobicents.protocols.ss7.tcap.asn.comp.Problem)
   */
  // @Override
  // public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
  // logger.error(String.format("onRejectComponent for Dialog=%d and invokeId=%d Problem=%s isLocalOriginated=%s",
  // mapDialog.getLocalDialogId(), invokeId, problem, isLocalOriginated));
  // }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPServiceListener#onInvokeTimeout
   * (org.mobicents.protocols.ss7.map.api.MAPDialog, java.lang.Long)
   */
  // @Override
  // public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
  // logger.error(String.format("onInvokeTimeout for Dialog=%d and invokeId=%d", mapDialog.getLocalDialogId(), invokeId));

  // }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogDelimiter
   * (org.mobicents.protocols.ss7.map.api.MAPDialog)
   */
  @Override
  public void onDialogDelimiter(MAPDialog mapDialog) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onDialogDelimiter for DialogId=%d", mapDialog.getLocalDialogId()));
    } else {
      logger.info(String.format("onDialogDelimiter for DialogId=%d", mapDialog.getLocalDialogId()));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogRequest(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.primitives.AddressString,
   * org.mobicents.protocols.ss7.map.api.primitives.AddressString,
   * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
   */
  @Override
  public void onDialogRequest(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
                              MAPExtensionContainer extensionContainer) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format(
          "onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s MAPExtensionContainer=%s",
          mapDialog.getLocalDialogId(), destReference, origReference, extensionContainer));
    } else {
      logger.info(String.format(
          "onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s MAPExtensionContainer=%s",
          mapDialog.getLocalDialogId(), destReference, origReference, extensionContainer));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogRequestEricsson(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.primitives.AddressString,
   * org.mobicents.protocols.ss7.map.api.primitives.AddressString,
   * org.mobicents.protocols.ss7.map.api.primitives.IMSI,
   * org.mobicents.protocols.ss7.map.api.primitives.AddressString)
   */
  @Override
  public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
                                      IMSI arg3, AddressString arg4) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s ",
          mapDialog.getLocalDialogId(), destReference, origReference));
    } else {
      logger.info(String.format("onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s ",
          mapDialog.getLocalDialogId(), destReference, origReference));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogAccept(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
   */
  @Override
  public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onDialogAccept for DialogId=%d MAPExtensionContainer=%s", mapDialog.getLocalDialogId(),
          extensionContainer));
    } else {
      logger.info(String.format("onDialogAccept for DialogId=%d MAPExtensionContainer=%s", mapDialog.getLocalDialogId(),
          extensionContainer));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogReject(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason, org.mobicents.protocols.ss7.map.api.dialog.MAPProviderError,
   * org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName,
   * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
   */
  @Override
  public void onDialogReject(MAPDialog mapDialog, MAPRefuseReason refuseReason,
                             ApplicationContextName alternativeApplicationContext, MAPExtensionContainer extensionContainer) {
    logger.error(String.format(
        "onDialogReject for DialogId=%d MAPRefuseReason=%s ApplicationContextName=%s MAPExtensionContainer=%s",
        mapDialog.getLocalDialogId(), refuseReason, alternativeApplicationContext, extensionContainer));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogUserAbort(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice,
   * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
   */
  @Override
  public void onDialogUserAbort(MAPDialog mapDialog, MAPUserAbortChoice userReason,
                                MAPExtensionContainer extensionContainer) {
    logger.error(String.format("onDialogUserAbort for DialogId=%d MAPUserAbortChoice=%s MAPExtensionContainer=%s",
        mapDialog.getLocalDialogId(), userReason, extensionContainer));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogProviderAbort(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason,
   * org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource,
   * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
   */
  @Override
  public void onDialogProviderAbort(MAPDialog mapDialog, MAPAbortProviderReason abortProviderReason,
                                    MAPAbortSource abortSource, MAPExtensionContainer extensionContainer) {
    logger.error(String.format(
        "onDialogProviderAbort for DialogId=%d MAPAbortProviderReason=%s MAPAbortSource=%s MAPExtensionContainer=%s",
        mapDialog.getLocalDialogId(), abortProviderReason, abortSource, extensionContainer));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogClose(org .mobicents.protocols.ss7.map.api.MAPDialog)
   */
  @Override
  public void onDialogClose(MAPDialog mapDialog) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("DialogClose for Dialog=%d", mapDialog.getLocalDialogId()));
    } else {
      logger.info(String.format("DialogClose for Dialog=%d", mapDialog.getLocalDialogId()));
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogNotice(org.mobicents.protocols.ss7.map.api.MAPDialog,
   * org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic)
   */
  @Override
  public void onDialogNotice(MAPDialog mapDialog, MAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
    logger.error(String.format("onDialogNotice for DialogId=%d MAPNoticeProblemDiagnostic=%s ",
        mapDialog.getLocalDialogId(), noticeProblemDiagnostic));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogResease(org.mobicents.protocols.ss7.map.api.MAPDialog)
   */
  @Override
  public void onDialogRelease(MAPDialog mapDialog) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("onDialogResease for DialogId=%d", mapDialog.getLocalDialogId()));
    } else {
      logger.info(String.format("onDialogResease for DialogId=%d", mapDialog.getLocalDialogId()));
    }

    this.endCount++;

    if (this.endCount < NDIALOGS) {
      if ((this.endCount % 10000) == 0) {
        long current = System.currentTimeMillis();
        float sec = (float) (current - prev) / 1000f;
        prev = current;
        logger.warn("Completed 10000 Dialogs, dlg/sec: " + (float) (10000 / sec));
      }
    } else {
      if (this.endCount == NDIALOGS) {
        long current = System.currentTimeMillis();
        logger.warn("Start Time = " + start);
        logger.warn("Current Time = " + current);
        float sec = (float) (current - start) / 1000f;

        logger.warn("Total time in sec = " + sec);
        logger.warn("Throughput = " + (float) (NDIALOGS / sec));
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogTimeout(org.mobicents.protocols.ss7.map.api.MAPDialog)
   */
  @Override
  public void onDialogTimeout(MAPDialog mapDialog) {
    logger.error(String.format("onDialogTimeout for DialogId=%d", mapDialog.getLocalDialogId()));
  }

  @Override
  public void onInsertSubscriberDataRequest(InsertSubscriberDataRequest insertSubscriberData) {

  }

  @Override
  public void onDeleteSubscriberDataRequest(DeleteSubscriberDataRequest deleteSubsData) {

  }

  @Override
  public void onErrorComponent(MAPDialog arg0, Long arg1, MAPErrorMessage arg2) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onInvokeTimeout(MAPDialog arg0, Long arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onMAPMessage(MAPMessage arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRejectComponent(MAPDialog arg0, Long arg1, Problem arg2, boolean arg3) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onActivateTraceModeRequest_Mobility(ActivateTraceModeRequest_Mobility arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onActivateTraceModeResponse_Mobility(ActivateTraceModeResponse_Mobility arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAuthenticationFailureReportRequest(AuthenticationFailureReportRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAuthenticationFailureReportResponse(AuthenticationFailureReportResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onCancelLocationRequest(CancelLocationRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onCancelLocationResponse(CancelLocationResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onCheckImeiRequest(CheckImeiRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onCheckImeiResponse(CheckImeiResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onDeleteSubscriberDataResponse(DeleteSubscriberDataResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onForwardCheckSSIndicationRequest(ForwardCheckSSIndicationRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onInsertSubscriberDataResponse(InsertSubscriberDataResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProvideSubscriberInfoRequest(ProvideSubscriberInfoRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProvideSubscriberInfoResponse(ProvideSubscriberInfoResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onPurgeMSRequest(PurgeMSRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onPurgeMSResponse(PurgeMSResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onResetRequest(ResetRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRestoreDataRequest(RestoreDataRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRestoreDataResponse(RestoreDataResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onSendAuthenticationInfoRequest(SendAuthenticationInfoRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onSendAuthenticationInfoResponse(SendAuthenticationInfoResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onSendIdentificationRequest(SendIdentificationRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onSendIdentificationResponse(SendIdentificationResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onUpdateGprsLocationRequest(UpdateGprsLocationRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onUpdateGprsLocationResponse(UpdateGprsLocationResponse arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onUpdateLocationRequest(UpdateLocationRequest arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onUpdateLocationResponse(UpdateLocationResponse arg0) {
    // TODO Auto-generated method stub

  }

}
