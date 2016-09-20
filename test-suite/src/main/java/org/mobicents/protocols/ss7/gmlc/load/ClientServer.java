/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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

import org.apache.log4j.Logger;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.sctp.netty.NettySctpManagementImpl;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.AspFactory;
import org.mobicents.protocols.ss7.m3ua.ExchangeType;
import org.mobicents.protocols.ss7.m3ua.Functionality;
import org.mobicents.protocols.ss7.m3ua.IPSPType;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.parameter.RoutingContext;
import org.mobicents.protocols.ss7.m3ua.parameter.TrafficModeType;
import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdFixedLength;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.IMEI;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
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
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeRequest_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeResponse_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.GPRSMSClass;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.GeodeticInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.GeographicalInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformationEPS;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformationGPRS;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationNumberMap;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.MNPInfoRes;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.MSClassmark2;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.NotReachableReason;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.PSSubscriberState;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberState;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberStateChoice;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.UserCSGInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.DeleteSubscriberDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.DeleteSubscriberDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.LSAIdentity;
import org.mobicents.protocols.ss7.gmlc.load.TestHarness;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.sccp.LoadSharingAlgorithm;
import org.mobicents.protocols.ss7.sccp.NetworkIdState;
import org.mobicents.protocols.ss7.sccp.OriginationType;
import org.mobicents.protocols.ss7.sccp.RuleType;
import org.mobicents.protocols.ss7.sccp.SccpResource;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author Fernando Mendioroz (fernando.mendioroz@telestax.com)
 *
 */
public class ClientServer extends TestHarness implements MAPServiceMobilityListener {

    private static Logger logger = Logger.getLogger(ClientServer.class);

    // TCAP
    private TCAPStack serverTcapStack;
    private TCAPStack clientTcapStack;

    // MAP
    private MAPStackImpl serverMapStack;
    private MAPProvider serverMapProvider;
    private MAPStackImpl clientMapStack;
    private MAPProvider clientMapProvider;

    // SCCP
    private SccpStackImpl serverSccpStack;
    private SccpResource serverSccpResource;
    private SccpStackImpl clientSccpStack;
    private SccpResource clientSccpResource;

    // M3UA
    private M3UAManagementImpl serverM3UAMgmt;
    private M3UAManagementImpl clientM3UAMgmt;

    // SCTP
    private NettySctpManagementImpl serverSctpManagement;
    private NettySctpManagementImpl clientSctpManagement;

    // a ramp-up period is required for performance testing.
    int serverEndCount = 0;
    int clientEndCount = -100;

    // AtomicInteger nbConcurrentDialogs = new AtomicInteger(0);

    // volatile long start = 0L;
    volatile long serverStart = System.currentTimeMillis();
    volatile long clientStart = 0L;
    volatile long prev = 0L;

    private RateLimiter rateLimiterObj = null;

    protected void initializeServerStack(IpChannelType ipChannelType) throws Exception {

        this.initServerSCTP(ipChannelType);

        // Initialize M3UA first
        this.initServerM3UA();

        // Initialize SCCP
        this.initServerSCCP();

        // Initialize TCAP
        this.initServerTCAP();

        // Initialize MAP
        this.initServerMAP();

        // Start ASP
        this.serverM3UAMgmt.startAsp("RASP1");
    }

    protected void initializeClientStack(IpChannelType ipChannelType) throws Exception {

        this.rateLimiterObj = RateLimiter.create(MAXCONCURRENTDIALOGS); // rate

        this.initClientSCTP(ipChannelType);

        // Initialize M3UA first
        this.initClientM3UA();

        // Initialize SCCP
        this.initClientSCCP();

        // Initialize TCAP
        this.initClientTCAP();

        // Initialize MAP
        this.initClientMAP();

        // Start ASP
        this.clientM3UAMgmt.startAsp("ASP1");
    }

    private void initServerSCTP(IpChannelType ipChannelType) throws Exception {
        this.serverSctpManagement = new NettySctpManagementImpl("Server");
        // this.serverSctpManagement.setSingleThread(false);
        this.serverSctpManagement.start();
        this.serverSctpManagement.setConnectDelay(10000);
        this.serverSctpManagement.removeAllResourses();

        // 1. Create SCTP Server
        serverSctpManagement.addServer(SERVER_NAME, SERVER_IP, SERVER_PORT, ipChannelType, null);

        // 2. Create SCTP Server Association
        serverSctpManagement.addServerAssociation(CLIENT_IP, CLIENT_PORT, SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);

        // 3. Start Server
        serverSctpManagement.startServer(SERVER_NAME);
    }

    private void initClientSCTP(IpChannelType ipChannelType) throws Exception {
        this.clientSctpManagement = new NettySctpManagementImpl("Client");
        // this.clientSctpManagement.setSingleThread(false);
        this.clientSctpManagement.start();
        this.clientSctpManagement.setConnectDelay(10000);
        this.clientSctpManagement.removeAllResourses();

        // 1. Create SCTP Association
        clientSctpManagement.addAssociation(CLIENT_IP, CLIENT_PORT, SERVER_IP, SERVER_PORT, CLIENT_ASSOCIATION_NAME,
                ipChannelType, null);
    }

    private void initServerM3UA() throws Exception {
        this.serverM3UAMgmt = new M3UAManagementImpl("Server", null);
        this.serverM3UAMgmt.setTransportManagement(this.serverSctpManagement);
        this.serverM3UAMgmt.setDeliveryMessageThreadCount(DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);
        this.serverM3UAMgmt.start();
        this.serverM3UAMgmt.removeAllResourses();

        // Step 1 : Create App Server
        RoutingContext rc = factory.createRoutingContext(new long[] { 100L });
        TrafficModeType trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare);
        As as = this.serverM3UAMgmt.createAs("RAS1", Functionality.SGW, ExchangeType.SE, IPSPType.CLIENT, rc, trafficModeType,
                1, null);

        // Step 2 : Create ASP
        AspFactory aspFactor = this.serverM3UAMgmt.createAspFactory("RASP1", SERVER_ASSOCIATION_NAME);

        // Step3 : Assign ASP to AS
        Asp asp = this.serverM3UAMgmt.assignAspToAs("RAS1", "RASP1");

        // Step 4: Add Route. Remote point code is 2
        this.serverM3UAMgmt.addRoute(CLIENT_SPC, -1, -1, "RAS1");
    }

    private void initClientM3UA() throws Exception {
        this.clientM3UAMgmt = new M3UAManagementImpl("Client", null);
        this.clientM3UAMgmt.setTransportManagement(this.clientSctpManagement);
        this.clientM3UAMgmt.setDeliveryMessageThreadCount(DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);
        this.clientM3UAMgmt.start();
        this.clientM3UAMgmt.removeAllResourses();

        // m3ua as create rc <rc> <ras-name>
        RoutingContext rc = factory.createRoutingContext(new long[] { 100L });
        TrafficModeType trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare);
        this.clientM3UAMgmt.createAs("AS1", Functionality.AS, ExchangeType.SE, IPSPType.CLIENT, rc, trafficModeType, 1, null);

        // Step 2 : Create ASP
        this.clientM3UAMgmt.createAspFactory("ASP1", CLIENT_ASSOCIATION_NAME);

        // Step3 : Assign ASP to AS
        Asp asp = this.clientM3UAMgmt.assignAspToAs("AS1", "ASP1");

        // Step 4: Add Route. Remote point code is 2
        clientM3UAMgmt.addRoute(SERVER_SPC, -1, -1, "AS1");

    }

    private void initServerSCCP() throws Exception {
        this.serverSccpStack = new SccpStackImpl("MapLoadServerSccpStack");
        this.serverSccpStack.setMtp3UserPart(1, this.serverM3UAMgmt);

        this.serverSccpStack.start();
        this.serverSccpStack.removeAllResourses();

        this.serverSccpStack.getSccpResource().addRemoteSpc(0, CLIENT_SPC, 0, 0);
        this.serverSccpStack.getSccpResource().addRemoteSsn(0, CLIENT_SPC, CLIENT_SSN, 0, false);

        this.serverSccpStack.getRouter().addMtp3ServiceAccessPoint(1, 1, SERVER_SPC, NETWORK_INDICATOR, 0);
        this.serverSccpStack.getRouter().addMtp3Destination(1, 1, CLIENT_SPC, CLIENT_SPC, 0, 255, 255);

        ParameterFactoryImpl fact = new ParameterFactoryImpl();
        EncodingScheme ec = new BCDEvenEncodingScheme();
        GlobalTitle gt1 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        GlobalTitle gt2 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        SccpAddress localAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt1, SERVER_SPC, SERVER_SSN);
        this.serverSccpStack.getRouter().addRoutingAddress(1, localAddress);
        SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt2, CLIENT_SPC, CLIENT_SSN);
        this.serverSccpStack.getRouter().addRoutingAddress(2, remoteAddress);

        GlobalTitle gt = fact.createGlobalTitle("*", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        SccpAddress pattern = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0);
        this.serverSccpStack.getRouter().addRule(1, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.REMOTE,
                pattern, "K", 1, -1, null, 0);
        this.serverSccpStack.getRouter().addRule(2, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.LOCAL,
                pattern, "K", 2, -1, null, 0);
    }

    private void initClientSCCP() throws Exception {
        this.clientSccpStack = new SccpStackImpl("MapLoadClientSccpStack");
        this.clientSccpStack.setMtp3UserPart(1, this.clientM3UAMgmt);

        // this.clientSccpStack.setCongControl_Algo(SccpCongestionControlAlgo.levelDepended);

        this.clientSccpStack.start();
        this.clientSccpStack.removeAllResourses();

        this.clientSccpStack.getSccpResource().addRemoteSpc(0, SERVER_SPC, 0, 0);
        this.clientSccpStack.getSccpResource().addRemoteSsn(0, SERVER_SPC,SERVER_SSN, 0, false);

        this.clientSccpStack.getRouter().addMtp3ServiceAccessPoint(1, 1, CLIENT_SPC, NETWORK_INDICATOR, 0);
        this.clientSccpStack.getRouter().addMtp3Destination(1, 1, SERVER_SPC, SERVER_SPC, 0, 255, 255);

        ParameterFactoryImpl fact = new ParameterFactoryImpl();
        EncodingScheme ec = new BCDEvenEncodingScheme();
        GlobalTitle gt1 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        GlobalTitle gt2 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        SccpAddress localAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt1, CLIENT_SPC, CLIENT_SSN);
        this.clientSccpStack.getRouter().addRoutingAddress(1, localAddress);
        SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt2, SERVER_SPC, SERVER_SSN);
        this.clientSccpStack.getRouter().addRoutingAddress(2, remoteAddress);

        GlobalTitle gt = fact.createGlobalTitle("*", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
                NatureOfAddress.INTERNATIONAL);
        SccpAddress pattern = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0);
        this.clientSccpStack.getRouter().addRule(1, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.REMOTE,
                pattern, "K", 1, -1, null, 0);
        this.clientSccpStack.getRouter().addRule(2, RuleType.SOLITARY, LoadSharingAlgorithm.Bit0, OriginationType.LOCAL,
                pattern, "K", 2, -1, null, 0);
    }

    private void initServerTCAP() throws Exception {
        this.serverTcapStack = new TCAPStackImpl("TestServer", this.serverSccpStack.getSccpProvider(), SERVER_SSN);
        this.serverTcapStack.start();
        this.serverTcapStack.setDialogIdleTimeout(60000);
        this.serverTcapStack.setInvokeTimeout(30000);
        this.serverTcapStack.setMaxDialogs(MAX_DIALOGS);
    }

    private void initClientTCAP() throws Exception {
        this.clientTcapStack = new TCAPStackImpl("Test", this.clientSccpStack.getSccpProvider(), CLIENT_SSN);
        this.clientTcapStack.start();
        this.clientTcapStack.setDialogIdleTimeout(60000);
        this.clientTcapStack.setInvokeTimeout(30000);
        this.clientTcapStack.setMaxDialogs(MAX_DIALOGS);
    }

    private void initServerMAP() throws Exception {
        this.serverMapStack = new MAPStackImpl("TestServer", this.serverTcapStack.getProvider());
        this.serverMapProvider = this.serverMapStack.getMAPProvider();

        this.serverMapProvider.addMAPDialogListener(this);
        this.serverMapProvider.getMAPServiceMobility().addMAPServiceListener(this);

        this.serverMapProvider.getMAPServiceMobility().acivate();

        this.serverMapStack.start();
    }

    private void initClientMAP() throws Exception {

        // this.clientMapStack = new MAPStackImpl(CLIENT_ASSOCIATION_NAME, this.sccpStack.getSccpProvider(), SSN);
        this.clientMapStack = new MAPStackImpl("TestClient", this.clientTcapStack.getProvider());
        this.clientMapProvider = this.clientMapStack.getMAPProvider();

        this.clientMapProvider.addMAPDialogListener(this);
        this.clientMapProvider.getMAPServiceMobility().addMAPServiceListener(this);
        this.clientMapProvider.getMAPServiceMobility().acivate();

        this.clientMapStack.start();
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

        logger.info("Client SSN=" + TestHarness.CLIENT_SSN);

        if (args.length >= 13) {
            TestHarness.SERVER_SSN = Integer.parseInt(args[12]);
        }

        logger.info("Server SSN=" + TestHarness.SERVER_SSN);
        
        if (args.length >= 14) {
            TestHarness.ROUTING_CONTEXT = Integer.parseInt(args[13]);
        }

        logger.info("ROUTING_CONTEXT=" + TestHarness.ROUTING_CONTEXT);

        if (args.length >= 15) {
            TestHarness.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = Integer.parseInt(args[14]);
        }

        logger.info("DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT=" + TestHarness.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);

        final ClientServer server = new ClientServer();
        try {
            server.initializeServerStack(ipChannelType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ClientServer client = new ClientServer();

        try {
            client.initializeClientStack(ipChannelType);

            Thread.sleep(20000);

            while (client.clientEndCount < NDIALOGS) {
                /*
                 * while (client.nbConcurrentDialogs.intValue() >= MAXCONCURRENTDIALOGS) {
                 * 
                 * logger.warn("Number of concurrent MAP dialog's = " + client.nbConcurrentDialogs.intValue() +
                 * " Waiting for max dialog count to go down!");
                 * 
                 * synchronized (client) { try { client.wait(); } catch (Exception ex) { } } }// end of while
                 * (client.nbConcurrentDialogs.intValue() >= MAXCONCURRENTDIALOGS)
                 */

                if (client.clientEndCount < 0) {
                    client.clientStart = System.currentTimeMillis();
                    client.prev = client.clientStart;
                    // logger.warn("StartTime = " + client.start);
                }

                client.initiateMapAti();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initiateMapAti() throws MAPException {
        try {
            NetworkIdState networkIdState = this.clientMapStack.getMAPProvider().getNetworkIdState(0);
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
            AddressString origRef = this.clientMapProvider.getMAPParameterFactory()
                    .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "12345");
            AddressString destRef = this.clientMapProvider.getMAPParameterFactory()
                    .createAddressString(AddressNature.international_number, NumberingPlan.ISDN, "67890");
            MAPDialogMobility mapDialogMobility = this.clientMapProvider.getMAPServiceMobility()
                    .createNewDialog(
                            MAPApplicationContext.getInstance(MAPApplicationContextName.anyTimeEnquiryContext,
                                    MAPApplicationContextVersion.version3),
                            SCCP_CLIENT_ADDRESS, origRef, SCCP_SERVER_ADDRESS, destRef);

            // Then, create parameters for concerning MAP operation
            ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "59897654321");
            SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);

            RequestedInfo requestedInfo = new RequestedInfoImpl(true, true, null, false, null, false, false, false);
            // requestedInfo (MAP ATI): last known location and state (idle or busy), no IMEI/MS Classmark/MNP

            ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "59800001");

            mapDialogMobility.addAnyTimeInterrogationRequest(msisdn, requestedInfo, gsmSCFAddress, null);

            // This will initiate the TC-BEGIN with INVOKE component
            mapDialogMobility.send();
        } catch (MAPException e) {
            logger.error(String.format("Error while sending MAP ATI:" + e));
        }

    }

    @Override
    public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest atiReq) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    String.format("onAnyTimeInterrogationRequest for DialogId=%d", atiReq.getMAPDialog().getLocalDialogId()));
        } else {
            logger.info(
                    String.format("onAnyTimeInterrogationRequest for DialogId=%d", atiReq.getMAPDialog().getLocalDialogId()));
        }

        try {
            long invokeId = atiReq.getInvokeId();
            MAPDialogMobility mapDialogMobility = atiReq.getMAPDialog();
            mapDialogMobility.setUserObject(invokeId);

            MAPParameterFactoryImpl mapFactory = new MAPParameterFactoryImpl();

            // Create Subscriber Information parameters including Location Information and Subscriber State
            // for concerning MAP operation
            CellGlobalIdOrServiceAreaIdFixedLength cellGlobalIdOrServiceAreaIdFixedLength = mapFactory
                    .createCellGlobalIdOrServiceAreaIdFixedLength(748, 1, 23, 369);
            CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = mapFactory
                    .createCellGlobalIdOrServiceAreaIdOrLAI(cellGlobalIdOrServiceAreaIdFixedLength);
            ISDNAddressString vlrNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123007");
            ISDNAddressString mscNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123007");
            Integer ageOfLocationInformation = 0; // ageOfLocationInformation
            GeographicalInformation geographicalInformation = null;
            LocationNumberMap locationNumber = null;
            MAPExtensionContainer mapExtensionContainer = null;
            LSAIdentity selectedLSAId = null;
            GeodeticInformation geodeticInformation = null;
            boolean currentLocationRetrieved = false;
            boolean saiPresent = false;
            LocationInformationEPS locationInformationEPS = null;
            UserCSGInformation userCSGInformation = null;
            LocationInformationGPRS locationInformationGPRS = null;
            PSSubscriberState psSubscriberState = null;
            IMEI imei = null;
            MSClassmark2 msClassmark2 = null;
            GPRSMSClass gprsMSClass = null;
            MNPInfoRes mnpInfoRes = null;
            SubscriberStateChoice subscriberStateChoice = SubscriberStateChoice.camelBusy; // 0=assumedIdle, 1=camelBusy,
                                                                                             // 2=notProvidedFromVLR
            NotReachableReason notReachableReason = null;

            LocationInformation locationInformation = mapFactory.createLocationInformation(ageOfLocationInformation,
                    geographicalInformation, vlrNumber, locationNumber, cellGlobalIdOrServiceAreaIdOrLAI, mapExtensionContainer,
                    selectedLSAId, mscNumber, geodeticInformation, currentLocationRetrieved, saiPresent, locationInformationEPS,
                    userCSGInformation);

            SubscriberState subscriberState = mapFactory.createSubscriberState(subscriberStateChoice, notReachableReason);

            SubscriberInfo subscriberInfo = mapFactory.createSubscriberInfo(locationInformation, subscriberState,
                    mapExtensionContainer, locationInformationGPRS, psSubscriberState, imei, msClassmark2, gprsMSClass,
                    mnpInfoRes);

            mapDialogMobility.addAnyTimeInterrogationResponse(invokeId, subscriberInfo, mapExtensionContainer);

            // This will close the TCAP dialog with TC-END initiated by TC-BEGIN with INVOKE component
            mapDialogMobility.close(false);

        } catch (MAPException mapException) {
            logger.error("MAP Exception while processing AnyTimeInterrogationRequest ", mapException);
        } catch (Exception e) {
            logger.error("Exception while processing AnyTimeInterrogationRequest ", e);
        }

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
                                "Rx onAnyTimeInterrogationResponse, Bad Subscriber State received: " + si + "for DialogId=%d",
                                atiResp.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                                "Rx onAnyTimeInterrogationResponse, Bad Subscriber State received: " + si + "for DialogId=%d",
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
                    atiResp.getMAPDialog().getLocalDialogId()));

        }
    }

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
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogRequest
     * (org.mobicents.protocols.ss7.map.api.MAPDialog, org.mobicents.protocols.ss7.map.api.primitives.AddressString,
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
        }
    }

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
            IMSI imsi, AddressString vlr) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s ",
                    mapDialog.getLocalDialogId(), destReference, origReference));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogAccept( org.mobicents.protocols.ss7.map.api.MAPDialog,
     * org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer)
     */
    @Override
    public void onDialogAccept(MAPDialog mapDialog, MAPExtensionContainer extensionContainer) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("onDialogAccept for DialogId=%d MAPExtensionContainer=%s", mapDialog.getLocalDialogId(),
                    extensionContainer));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogReject( org.mobicents.protocols.ss7.map.api.MAPDialog,
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
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogUserAbort
     * (org.mobicents.protocols.ss7.map.api.MAPDialog, org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice,
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
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogProviderAbort
     * (org.mobicents.protocols.ss7.map.api.MAPDialog, org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason,
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
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogNotice( org.mobicents.protocols.ss7.map.api.MAPDialog,
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
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogResease
     * (org.mobicents.protocols.ss7.map.api.MAPDialog)
     */
    @Override
    public void onDialogRelease(MAPDialog mapDialog) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("onDialogResease for DialogId=%d", mapDialog.getLocalDialogId()));
        }

        this.serverEndCount++;

        if (((this.serverEndCount % 10000) == 0) && serverEndCount>1) {
            long currentTime = System.currentTimeMillis();
            long processingTime = currentTime - serverStart;
            serverStart = currentTime;
            logger.warn("Completed 10000 Dialogs in=" + processingTime + " ms. Dialogs per sec: "+ + (float) (10000000 / processingTime));
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogTimeout
     * (org.mobicents.protocols.ss7.map.api.MAPDialog)
     */
    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        logger.error(String.format("onDialogTimeout for DialogId=%d", mapDialog.getLocalDialogId()));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPServiceListener#onErrorComponent
     * (org.mobicents.protocols.ss7.map.api.MAPDialog, java.lang.Long,
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

    public void onInsertSubscriberDataRequest(InsertSubscriberDataRequest insertSubscriberData) {

    }

    public void onDeleteSubscriberDataRequest(DeleteSubscriberDataRequest deleteSubsData) {

    }

    public void onErrorComponent(MAPDialog arg0, Long arg1, MAPErrorMessage arg2) {
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
}
