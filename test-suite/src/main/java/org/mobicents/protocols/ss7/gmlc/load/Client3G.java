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

// import java.io.Serializable;
// import javolution.util.FastMap;

import com.google.common.primitives.Booleans;
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
import org.mobicents.protocols.ss7.map.api.primitives.*;
import org.mobicents.protocols.ss7.gmlc.load.Client3G;
import org.mobicents.protocols.ss7.gmlc.load.TestHarness3G;
import org.mobicents.protocols.ss7.map.api.service.lsm.*;
import org.mobicents.protocols.ss7.map.primitives.GSNAddressImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.map.service.lsm.LocationTypeImpl;
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
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

import com.google.common.util.concurrent.RateLimiter;

import static org.mobicents.protocols.ss7.map.api.service.lsm.LCSEvent.emergencyCallOrigination;
import static org.mobicents.protocols.ss7.map.api.service.lsm.LocationEstimateType.currentLocation;
import static sun.jdbc.odbc.JdbcOdbcObject.hexStringToByteArray;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class Client3G extends TestHarness3G implements MAPServiceLsmListener {

    private static Logger logger = Logger.getLogger(Client.class);

    // TCAP
    private TCAPStack tcapStack;

    // MAP
    private MAPStackImpl mapStack;
    private MAPProvider mapProvider;

    // SCCP
    private SccpStackImpl sccpStack;
    private SccpResource sccpResource;

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
        this.mapProvider.getMAPServiceLsm().addMAPServiceListener(this);
        this.mapProvider.getMAPServiceLsm().acivate();

        this.mapStack.start();
    }

    private void initiateMapSRIforLCS() throws MAPException {
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

            ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");

//            Long	addSendRoutingInfoForLCSRequest(ISDNAddressString gmlcNumber, SubscriberIdentity targetMS, MAPExtensionContainer extensionContainer)
            mapDialogLsm.addSendRoutingInfoForLCSRequest(gsmSCFAddress, msisdn, null);
            logger.info("SRIforLCS msisdn:" + msisdn + ", sriForLCSIsdnAddress:" + gsmSCFAddress);

            // This will initiate the TC-BEGIN with INVOKE component
            mapDialogLsm.send();
        } catch (MAPException e) {
            logger.error(String.format("Error while sending MAP SRIforLCS:" + e));
        }

    }

    private void initiateMapPSL() throws MAPException {
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
            ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");
            final LocationEstimateType locationEstimateType = currentLocation;
            // public enum LocationEstimateType {currentLocation(0), currentOrLastKnownLocation(1), initialLocation(2), activateDeferredLocation(3),
            //                                   cancelDeferredLocation(4)..
            final DeferredLocationEventType deferredLocationEventType = null;
            // DeferredLocationEventType: boolean getMsAvailable(); getEnteringIntoArea(); getLeavingFromArea(); getBeingInsideArea();
            // deferredLocationEventType.getEnteringIntoArea();
            LocationType locationType = new LocationTypeImpl(locationEstimateType, deferredLocationEventType);
            LCSClientID lcsClientID = null;
            Boolean privacyOverride = false;
            IMSI imsi = null;
            LMSI lmsi = null;
            IMEI imei = null;
            LCSPriority lcsPriority = null;
            LCSQoS lcsQoS = null;
            SupportedGADShapes supportedGADShapes = null;
            Integer lcsReferenceNumber = 379;
            Integer lcsServiceTypeID = 0;
            LCSCodeword lcsCodeword = null;
            MAPExtensionContainer extensionContainer = null;
            LCSPrivacyCheck lcsPrivacyCheck = null;
            AreaEventInfo areaEventInfo = null;
            byte[] homeGmlcAddress = hexStringToByteArray("3734383439323337");
            GSNAddress hGmlcAddress = new GSNAddressImpl(homeGmlcAddress);
            PeriodicLDRInfo periodicLDRInfo = null;
            ReportingPLMNList reportingPLMNList = null;

            mapDialogLsm.addProvideSubscriberLocationRequest(locationType, gsmSCFAddress, lcsClientID, privacyOverride, imsi, msisdn, lmsi, imei, lcsPriority,
                lcsQoS, extensionContainer, supportedGADShapes, lcsReferenceNumber, lcsServiceTypeID, lcsCodeword,
                lcsPrivacyCheck,areaEventInfo, hGmlcAddress, false, periodicLDRInfo, reportingPLMNList);
            logger.info("MAP PSL: msisdn:" + msisdn + ", gsmSCFAddress:" + gsmSCFAddress);
            // This will initiate the TC-BEGIN with INVOKE component
            mapDialogLsm.send();
        } catch (MAPException e) {
            logger.error(String.format("Error while sending MAP SRIforLCS:" + e));
        }

    }

    private void initiateMapSLR() throws MAPException {
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
            ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "222333");
            ISDNAddressString naEsrd = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "1110101");
            ISDNAddressString naEsrk = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "9889277");
            final LCSEvent lcsEvent = emergencyCallOrigination;
            // LCS-Event ::= ENUMERATED { emergencyCallOrigination (0), emergencyCallRelease (1), mo-lr (2), ..., deferredmt-lrResponse (3) } -- exception handling: --
            // a SubscriberLocationReport-Arg containing an unrecognized LCS-Event -- shall be rejected by a receiver with a return error cause of unexpected data value
            final LocationEstimateType locationEstimateType = currentLocation;
            // public enum LocationEstimateType {currentLocation(0), currentOrLastKnownLocation(1), initialLocation(2), activateDeferredLocation(3),
            //                                   cancelDeferredLocation(4)..
            LCSClientID lcsClientID = null;
            LCSLocationInfo lcsLocationInfo = null;
            IMSI imsi = null;
            IMEI imei = null;
            Integer ageOfLocationEstimate = 0;
            ExtGeographicalInformation extGeographicalInformation = null;
            SLRArgExtensionContainer slrArgExtensionContainer = null;
            byte[] addLocationEstimate = null;
            AddGeographicalInformation addGeographicalInformation = null;
            DeferredmtlrData deferredmtlrData = null;
            PositioningDataInformation positioningDataInformation = null;
            UtranPositioningDataInfo utranPositioningDataInfo = null;
            Integer lcsServiceTypeID = 1;
            boolean saiPresent = true;
            Boolean pseudonymIndicator = false;
            AccuracyFulfilmentIndicator accuracyFulfilmentIndicator = null;
            VelocityEstimate velocityEstimate = null;
            PeriodicLDRInfo periodicLDRInfo = null;
            boolean b2 = false;
            CellGlobalIdOrServiceAreaIdOrLAI cellIdOrSai = null;
            GeranGANSSpositioningData geranGanssPositioningData = null;
            UtranGANSSpositioningData utranGanssPositioningData = null;
            ServingNodeAddress servingNodeAddress = null;
            Integer lcsReferenceNumber = 379;
            Integer integer3 = 0;
            byte[] homeGmlcAddress = hexStringToByteArray("3734383439323337");
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
            logger.error(String.format("Error while sending MAP SRIforLCS:" + e));
        }

    }


    @Override
    public void onSendRoutingInfoForLCSRequest(SendRoutingInfoForLCSRequest sendRoutingInforForLCSRequestIndication) {
        /*
         * This is an error condition. Client should never receive onSendRoutingInfoForLCSRequest.
         */
        logger.error(String.format("onSendRoutingInfoForLCSRequest for Dialog=%d and invokeId=%d",
            sendRoutingInforForLCSRequestIndication.getMAPDialog().getLocalDialogId(), sendRoutingInforForLCSRequestIndication.getInvokeId()));
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
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse NetworkNodeNumber = %s " + networkNodeNumber +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse NetworkNodeNumber: "
                            + lcsLocationInfo.getNetworkNodeNumber() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad NetworkNodeNumber received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad NetworkNodeNumber received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                }

                if (lcsLocationInfo.getLMSI() != null) {
                    String lmsi = lcsLocationInfo.getLMSI().toString();
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse LMSI = %s " + lmsi +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse LMSI: "
                            + lcsLocationInfo.getLMSI() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad LMSI received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad LMSI received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                }

                if (lcsLocationInfo.getSupportedLCSCapabilitySets() != null) {
                    String supportedLCSCapabilitySets = lcsLocationInfo.getSupportedLCSCapabilitySets().toString();
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse Supported LCS Capability Sets = %s " + supportedLCSCapabilitySets +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse Supported LCS Capability Sets: "
                            + lcsLocationInfo.getSupportedLCSCapabilitySets() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Supported LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Supported LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                }

                if (lcsLocationInfo.getAdditionalLCSCapabilitySets() != null) {
                    String additionalLCSCapabilitySets = lcsLocationInfo.getAdditionalLCSCapabilitySets().toString();
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse Additional LCS Capability Sets = %s " + additionalLCSCapabilitySets +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse Additional LCS Capability Sets: "
                            + lcsLocationInfo.getAdditionalLCSCapabilitySets() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Additional LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Additional LCS Capability Sets received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                }

                if (lcsLocationInfo.getAdditionalNumber() != null) {
                    String additionalLCSCapabilitySets = lcsLocationInfo.getAdditionalNumber().toString();
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse Additional Number = %s " + additionalLCSCapabilitySets +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse Additional Number: "
                            + lcsLocationInfo.getAdditionalNumber() + "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Additional Number received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad Additional Number received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                }

                if (lcsLocationInfo.getGprsNodeIndicator() != true) {
                    String gprsNodeIndicator = "false";
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse GPRS Node Indicator = %s " + gprsNodeIndicator +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        gprsNodeIndicator = "true";
                        logger.info(String.format("Rx onSendRoutingInfoForLCSResponse GPRS Node Indicator = %s " + gprsNodeIndicator +
                            "for DialogId=%d", sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad GPRS Node Indicator received: " + lcsLocationInfo + "for DialogId=%d",
                            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad GPRS Node Indicator received: " + lcsLocationInfo + "for DialogId=%d",
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
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse LocationEstimate, latitude = %d " + latitude + ", longitude: " +
                        longitude + "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse LocationEstimate: "
                            + provideSubscriberLocationResponse.getLocationEstimate() + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad LocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad LocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getGeranPositioningData() != null) {
                PositioningDataInformation geranPositioningData = provideSubscriberLocationResponse.getGeranPositioningData();
                String geranPositioning = geranPositioningData.getData().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse GeranPositioningData = %s " + geranPositioning +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse GeranPositioningData: "
                            + provideSubscriberLocationResponse.getGeranPositioningData() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad GeranPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad GeranPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getUtranPositioningData() != null) {
                UtranPositioningDataInfo utranPositioningData = provideSubscriberLocationResponse.getUtranPositioningData();
                String utranPositioning = utranPositioningData.getData().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse UtranPositioningData = %s " + utranPositioning +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse UtranPositioningData: "
                            + provideSubscriberLocationResponse.getUtranPositioningData() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getAgeOfLocationEstimate() != null) {
                Integer ageOfLocationEstimate = provideSubscriberLocationResponse.getAgeOfLocationEstimate();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse AgeOfLocationEstimate = %d " + ageOfLocationEstimate +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse AgeOfLocationEstimate: "
                            + provideSubscriberLocationResponse.getAgeOfLocationEstimate() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AgeOfLocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AgeOfLocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getAdditionalLocationEstimate() != null) {
                AddGeographicalInformation additionalLocationEstimate = provideSubscriberLocationResponse.getAdditionalLocationEstimate();
                Double additionalLatitude = additionalLocationEstimate.getLatitude();
                Double additionalLongitude = additionalLocationEstimate.getLongitude();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse AdditionalLocationEstimate, latitude = %d "
                            + additionalLatitude + ", longitude: " +
                            additionalLongitude + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse AdditionalLocationEstimate: "
                            + provideSubscriberLocationResponse.getAdditionalLocationEstimate() + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AdditionalLocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AdditionalLocationEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getExtensionContainer() != null) {
                MAPExtensionContainer mapExtensionContainer = provideSubscriberLocationResponse.getExtensionContainer();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse MAPExtensionContainer not null" +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse MAPExtensionContainer: "
                            + provideSubscriberLocationResponse.getExtensionContainer() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad MAPExtensionContainer received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad MAPExtensionContainer received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() == false ||
                provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() == true) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse DeferredMTLRResponseIndicator: "
                            + provideSubscriberLocationResponse.getDeferredMTLRResponseIndicator() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad DeferredMTLRResponseIndicator received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad DeferredMTLRResponseIndicator received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    }
                }
            }

            if (provideSubscriberLocationResponse.getCellIdOrSai() != null) {
                CellGlobalIdOrServiceAreaIdOrLAI cellIdOrSai = provideSubscriberLocationResponse.getCellIdOrSai();
                String cidOrSai = cellIdOrSai.getCellGlobalIdOrServiceAreaIdFixedLength().toString();
                String laiFixedLength = cellIdOrSai.getLAIFixedLength().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse CellIdOrSai, CellGlobalIdOrServiceAreaIdOrLAI = %s " + cidOrSai +
                        ", LAIFixedLength: " + laiFixedLength +
                        ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse CellIdOrSai: "
                            + provideSubscriberLocationResponse.getCellIdOrSai() + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad CellIdOrSai received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad CellIdOrSai received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getSaiPresent() == false ||
                provideSubscriberLocationResponse.getSaiPresent() == true) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse SaiPresent: "
                            + provideSubscriberLocationResponse.getSaiPresent() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad SaiPresent received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad SaiPresent received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    }
                }
            }

            if (provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator() != null) {
                AccuracyFulfilmentIndicator accuracyFulfilmentIndicator = provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator();
                int indicator = accuracyFulfilmentIndicator.getIndicator();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse AccuracyFulfilmentIndicator, indicator = %d " + indicator +
                        ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse AccuracyFulfilmentIndicator: "
                            + provideSubscriberLocationResponse.getAccuracyFulfilmentIndicator() + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AccuracyFulfilmentIndicator received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad AccuracyFulfilmentIndicator received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getVelocityEstimate() != null) {
                VelocityEstimate velocityEstimate = provideSubscriberLocationResponse.getVelocityEstimate();
                long horizontalSpeed = velocityEstimate.getHorizontalSpeed();
                long verticalSpeed = velocityEstimate.getVerticalSpeed();
                int velocityType = velocityEstimate.getVelocityType().getCode();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse VelocityEstimate, horizontal speed = %d " + horizontalSpeed +
                        ", vertical speed: " + verticalSpeed + "velocity type: " + velocityType
                        + ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse VelocityEstimate: "
                            + provideSubscriberLocationResponse.getVelocityEstimate() + "for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad VelocityEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad VelocityEstimate received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() == false ||
                provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() == true) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse SaiPresent: "
                            + provideSubscriberLocationResponse.getMoLrShortCircuitIndicator() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad MoLrShortCircuitIndicator received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    } else {
                        logger.info(String.format(
                            "Rx onSendRoutingInfoForLCSResponse, Bad MoLrShortCircuitIndicator received for DialogId=%d",
                            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                    }
                }
            }

            if (provideSubscriberLocationResponse.getGeranGANSSpositioningData() != null) {
                GeranGANSSpositioningData geranGANSSpositioningDataPositioningData = provideSubscriberLocationResponse.getGeranGANSSpositioningData();
                String geranGanssPositioning = geranGANSSpositioningDataPositioningData.getData().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse GeranGANSSPositioningData = %s " + geranGanssPositioning +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse GeranGANSSPositioningData: "
                            + provideSubscriberLocationResponse.getGeranGANSSpositioningData() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad GeranGANSSPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad GeranGANSSPositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getUtranGANSSpositioningData() != null) {
                UtranGANSSpositioningData utranGANSSpositioningDataPositioningData = provideSubscriberLocationResponse.getUtranGANSSpositioningData();
                String utranGanssPositioning = utranGANSSpositioningDataPositioningData.getData().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse UtranGANSSpositioningData = %s " + utranGanssPositioning +
                        "for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse UtranGANSSpositioningData: "
                            + provideSubscriberLocationResponse.getUtranGANSSpositioningData() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranGANSSpositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranGANSSpositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

            if (provideSubscriberLocationResponse.getTargetServingNodeForHandover() != null) {
                ServingNodeAddress servingNodeAddress = provideSubscriberLocationResponse.getTargetServingNodeForHandover();
                String mscNumber = servingNodeAddress.getMscNumber().toString();
                String sgsnNumber = servingNodeAddress.getSgsnNumber().toString();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rx onSendRoutingInfoForLCSResponse ServingNode, MSC Number = %s " + mscNumber +
                        ", SGSN Number = %s " + sgsnNumber +
                        ", for DialogId=%d", provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));

                } else {
                    logger.info(String.format("Rx onSendRoutingInfoForLCSResponse UtranGANSSpositioningData: "
                            + provideSubscriberLocationResponse.getUtranGANSSpositioningData() + ", for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranGANSSpositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSendRoutingInfoForLCSResponse, Bad UtranGANSSpositioningData received for DialogId=%d",
                        provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
                }
            }

        } catch (Exception e) {
            logger.error(String.format("Error while processing onProvideSubscriberLocationResponse for Dialog=%d",
                provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId()));
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
                        "Rx onSubscriberLocationReportResponse, Bad NaESRD received for DialogId=%d",
                        subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSubscriberLocationReportResponse, Bad NaESRD received for DialogId=%d",
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
                        "Rx onSubscriberLocationReportResponse, Bad NaESRK received for DialogId=%d",
                        subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSubscriberLocationReportResponse, Bad NaESRK received for DialogId=%d",
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
                        "Rx onSubscriberLocationReportResponse, Bad MAPExtensionContainer received for DialogId=%d",
                        subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
                } else {
                    logger.info(String.format(
                        "Rx onSubscriberLocationReportResponse, Bad MAPExtensionContainer received for DialogId=%d",
                        subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
                }
            }

        } catch (Exception e) {
            logger.error(String.format("Error while processing onProvideSubscriberLocationResponse for Dialog=%d",
                subscriberLocationReportResponse.getMAPDialog().getLocalDialogId()));
        }

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
            TestHarness3G.CLIENT_IP = args[3];
        }

        logger.info("CLIENT_IP=" + TestHarness3G.CLIENT_IP);

        if (args.length >= 5) {
            TestHarness3G.CLIENT_PORT = Integer.parseInt(args[4]);
        }

        logger.info("CLIENT_PORT=" + TestHarness3G.CLIENT_PORT);

        if (args.length >= 6) {
            TestHarness3G.SERVER_IP = args[5];
        }

        logger.info("SERVER_IP=" + TestHarness3G.SERVER_IP);

        if (args.length >= 7) {
            TestHarness3G.SERVER_PORT = Integer.parseInt(args[6]);
        }

        logger.info("SERVER_PORT=" + TestHarness3G.SERVER_PORT);

        if (args.length >= 8) {
            TestHarness3G.CLIENT_SPC = Integer.parseInt(args[7]);
        }

        logger.info("CLIENT_SPC=" + TestHarness3G.CLIENT_SPC);

        if (args.length >= 9) {
            TestHarness3G.SERVER_SPC = Integer.parseInt(args[8]);
        }

        logger.info("SERVER_SPC=" + TestHarness3G.SERVER_SPC);

        if (args.length >= 10) {
            TestHarness3G.NETWORK_INDICATOR = Integer.parseInt(args[9]);
        }

        logger.info("NETWORK_INDICATOR=" + TestHarness3G.NETWORK_INDICATOR);

        if (args.length >= 11) {
            TestHarness3G.SERVICE_INDICATOR = Integer.parseInt(args[10]);
        }

        logger.info("SERVICE_INDICATOR=" + TestHarness3G.SERVICE_INDICATOR);

        if (args.length >= 12) {
            TestHarness3G.CLIENT_SSN = Integer.parseInt(args[11]);
        }

        logger.info("SSN=" + TestHarness3G.CLIENT_SSN);

        if (args.length >= 13) {
            TestHarness3G.ROUTING_CONTEXT = Integer.parseInt(args[12]);
        }

        logger.info("ROUTING_CONTEXT=" + TestHarness3G.ROUTING_CONTEXT);

        if (args.length >= 14) {
            TestHarness3G.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = Integer.parseInt(args[13]);
        }

        logger.info("DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT=" + TestHarness3G.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);

        /*
         * logger.info("Number of calls to be completed = " + noOfCalls + " Number of concurrent calls to be maintained = " +
         * noOfConcurrentCalls);
         */

        NDIALOGS = noOfCalls;

        logger.info("NDIALOGS=" + NDIALOGS);

        MAXCONCURRENTDIALOGS = noOfConcurrentCalls;

        logger.info("MAXCONCURRENTDIALOGS=" + MAXCONCURRENTDIALOGS);

        final Client3G client = new Client3G();

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

                // client.initiateMapSRIforLCS();
                client.initiateMapPSL();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        } else {
            logger.info(String.format(
                "onDialogRequest for DialogId=%d DestinationReference=%s OriginReference=%s MAPExtensionContainer=%s",
                mapDialog.getLocalDialogId(), destReference, origReference, extensionContainer));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogRequestEricsson
     * (org.mobicents.protocols.ss7.map.api.MAPDialog, org.mobicents.protocols.ss7.map.api.primitives.AddressString,
     * org.mobicents.protocols.ss7.map.api.primitives.AddressString, org.mobicents.protocols.ss7.map.api.primitives.IMSI,
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
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogAccept( org.mobicents.protocols.ss7.map.api.MAPDialog,
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
        } else {
            logger.info(String.format("DialogClose for Dialog=%d", mapDialog.getLocalDialogId()));
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
     * @see org.mobicents.protocols.ss7.map.api.MAPDialogListener#onDialogTimeout
     * (org.mobicents.protocols.ss7.map.api.MAPDialog)
     */
    @Override
    public void onDialogTimeout(MAPDialog mapDialog) {
        logger.error(String.format("onDialogTimeout for DialogId=%d", mapDialog.getLocalDialogId()));
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

}
