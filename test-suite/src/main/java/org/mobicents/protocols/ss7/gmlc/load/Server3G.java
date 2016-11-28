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
//import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
//import org.mobicents.protocols.ss7.map.api.primitives.GlobalCellId;
//import org.mobicents.protocols.ss7.map.api.primitives.LAIFixedLength;
//import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
//import org.mobicents.protocols.ss7.map.api.primitives.PlmnId;
//import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
//import org.mobicents.protocols.ss7.gmlc.load.Server;
//import org.mobicents.protocols.ss7.gmlc.load.TestHarness;
//import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.api.service.lsm.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SupportedLCSCapabilitySets;
import org.mobicents.protocols.ss7.map.primitives.GSNAddressImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
//import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
//import org.mobicents.protocols.ss7.map.primitives.MAPExtensionContainerImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.sccp.LoadSharingAlgorithm;
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

import static sun.jdbc.odbc.JdbcOdbcObject.hexStringToByteArray;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class Server3G extends TestHarness3G {

    private static Logger logger = Logger.getLogger(Server.class);

    // MAP
    private MAPStackImpl mapStack;
    private MAPProvider mapProvider;

    // TCAP
    private TCAPStack tcapStack;

    // SCCP
    private SccpStackImpl sccpStack;
    private SccpResource sccpResource;

    // M3UA
    private M3UAManagementImpl serverM3UAMgmt;

    // SCTP
    private NettySctpManagementImpl sctpManagement;

    int endCount = 0;
    volatile long start = System.currentTimeMillis();

    protected void initializeStack(IpChannelType ipChannelType) throws Exception {

        this.initSCTP(ipChannelType);

        // Initialize M3UA first
        this.initM3UA();

        // Initialize SCCP
        this.initSCCP();

        // Initialize TCAP
        this.initTCAP();

        // Initialize MAP
        this.initMAP();

        // 7. Start ASP
        serverM3UAMgmt.startAsp("RASP1");
    }

    private void initSCTP(IpChannelType ipChannelType) throws Exception {
        this.sctpManagement = new NettySctpManagementImpl("Server");
        // this.sctpManagement.setSingleThread(false);
        this.sctpManagement.start();
        this.sctpManagement.setConnectDelay(10000);
        this.sctpManagement.removeAllResourses();

        // 1. Create SCTP Server
        sctpManagement.addServer(SERVER_NAME, SERVER_IP, SERVER_PORT, ipChannelType, null);

        // 2. Create SCTP Server Association
        sctpManagement.addServerAssociation(CLIENT_IP, CLIENT_PORT, SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);

        // 3. Start Server
        sctpManagement.startServer(SERVER_NAME);
    }

    private void initM3UA() throws Exception {
        this.serverM3UAMgmt = new M3UAManagementImpl("Server", null);
        this.serverM3UAMgmt.setTransportManagement(this.sctpManagement);
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

    private void initSCCP() throws Exception {
        this.sccpStack = new SccpStackImpl("MapLoadServerSccpStack");
        this.sccpStack.setMtp3UserPart(1, this.serverM3UAMgmt);

        this.sccpStack.start();
        this.sccpStack.removeAllResourses();

        this.sccpStack.getSccpResource().addRemoteSpc(0, CLIENT_SPC, 0, 0);
        this.sccpStack.getSccpResource().addRemoteSsn(0, CLIENT_SPC, CLIENT_SSN, 0, false);

        this.sccpStack.getRouter().addMtp3ServiceAccessPoint(1, 1, SERVER_SPC, NETWORK_INDICATOR, 0);
        this.sccpStack.getRouter().addMtp3Destination(1, 1, CLIENT_SPC, CLIENT_SPC, 0, 255, 255);

        ParameterFactoryImpl fact = new ParameterFactoryImpl();
        EncodingScheme ec = new BCDEvenEncodingScheme();
        GlobalTitle gt1 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
            NatureOfAddress.INTERNATIONAL);
        GlobalTitle gt2 = fact.createGlobalTitle("-", 0, org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY, ec,
            NatureOfAddress.INTERNATIONAL);
        SccpAddress localAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt1, SERVER_SPC, SERVER_SSN);
        this.sccpStack.getRouter().addRoutingAddress(1, localAddress);
        SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt2, CLIENT_SPC, CLIENT_SSN);
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
        this.tcapStack = new TCAPStackImpl("TestServer", this.sccpStack.getSccpProvider(), SERVER_SSN);
        this.tcapStack.start();
        this.tcapStack.setDialogIdleTimeout(60000);
        this.tcapStack.setInvokeTimeout(30000);
        this.tcapStack.setMaxDialogs(MAX_DIALOGS);
    }

    private void initMAP() throws Exception {
        this.mapStack = new MAPStackImpl("TestServer", this.tcapStack.getProvider());
        this.mapProvider = this.mapStack.getMAPProvider();

        this.mapProvider.addMAPDialogListener(this);
        this.mapProvider.getMAPServiceLsm().addMAPServiceListener(this);

        this.mapProvider.getMAPServiceLsm().acivate();

        this.mapStack.start();
    }

    @Override
    public void onSendRoutingInfoForLCSRequest(SendRoutingInfoForLCSRequest sendRoutingInforForLCSRequest) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                String.format("onSendRoutingInfoForLCSRequest for DialogId=%d", sendRoutingInforForLCSRequest.getMAPDialog().getLocalDialogId()));
        } if (logger.isInfoEnabled()) {
            logger.info(String.format("onAnyTimeInterrogationRequest for DialogId=%d", sendRoutingInforForLCSRequest.getMAPDialog().getLocalDialogId()));
        }

        try {
            long invokeId = sendRoutingInforForLCSRequest.getInvokeId();
            MAPDialogLsm mapDialogLsm = sendRoutingInforForLCSRequest.getMAPDialog();
            mapDialogLsm.setUserObject(invokeId);

            // Create Routing Information parameters for concerning MAP operation
            MAPParameterFactoryImpl mapFactory = new MAPParameterFactoryImpl();
            ISDNAddressString mscNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123007");
            ISDNAddressString sgsnNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123009");
//            ISDNAddressString additionalNumber = new ISDNAddressStringImpl(AddressNature.international_number,
//                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123987");
            AdditionalNumber additionalNumber = null;
            byte[] Lmsi = hexStringToByteArray("3734383439323337343239");
            LMSI lmsi = mapFactory.createLMSI(Lmsi);
            Boolean gprsNodeIndicator = false;
            SupportedLCSCapabilitySets supportedLCSCapabilitySets = mapFactory.createSupportedLCSCapabilitySets(true, true, true, true, true);
            SupportedLCSCapabilitySets supportedAdditionalLCSCapabilitySets1 = mapFactory.createSupportedLCSCapabilitySets(false, false, false, false, false);
            MAPExtensionContainer mapExtensionContainer = null;
            DiameterIdentity mmeName = null;
            DiameterIdentity aaaServerName = null;
            ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "59899077937");
            SubscriberIdentity msisdn = new SubscriberIdentityImpl(isdnAdd);
            byte[] visitedGmlcAddress = null;
            GSNAddress vGmlcAddress = new GSNAddressImpl(visitedGmlcAddress);
            byte[] homeGmlcAddress = hexStringToByteArray("3734383439323337");
            GSNAddress hGmlcAddress = new GSNAddressImpl(homeGmlcAddress);
            byte[] pivacyProfileRegisterAddress = hexStringToByteArray("373438343932333777");
            GSNAddress pprAddress = new GSNAddressImpl(pivacyProfileRegisterAddress);
            byte[] addVGmlcAddress = null;
            GSNAddress additionalVGmlcAddress = new GSNAddressImpl(addVGmlcAddress);

            LCSLocationInfo lcsLocationInfo = mapFactory.createLCSLocationInfo(mscNumber, lmsi, mapExtensionContainer, gprsNodeIndicator,
                null, supportedLCSCapabilitySets, supportedAdditionalLCSCapabilitySets1,
                mmeName, aaaServerName);
            GSNAddress gsnAddress1 = new GSNAddressImpl();
//            addSendRoutingInfoForLCSResponse(long invokeId, SubscriberIdentity targetMS, LCSLocationInfo lcsLocationInfo, MAPExtensionContainer extensionContainer,
//                                            byte[] vgmlcAddress, byte[] hGmlcAddress, byte[] pprAddress, byte[] additionalVGmlcAddress)
            mapDialogLsm.addSendRoutingInfoForLCSResponse(invokeId, msisdn, lcsLocationInfo, mapExtensionContainer, vGmlcAddress, hGmlcAddress, pprAddress, additionalVGmlcAddress);
            // This will initiate the TC-BEGIN with INVOKE component
            mapDialogLsm.close(false);

        } catch (MAPException mapException) {
            logger.error("MAP Exception while processing onSendRoutingInfoForLCSRequest ", mapException);
        } catch (Exception e) {
            logger.error("Exception while processing onSendRoutingInfoForLCSRequest ", e);
        }
    }

    @Override
    public void onSendRoutingInfoForLCSResponse(SendRoutingInfoForLCSResponse sendRoutingInforForLCSResponse) {
        /*
         * This is an error condition. Server should never receive onSendRoutingInfoForLCSResponse.
         */
        logger.error(String.format("onAnyTimeInterrogationRequest for Dialog=%d and invokeId=%d",
            sendRoutingInforForLCSResponse.getMAPDialog().getLocalDialogId(), sendRoutingInforForLCSResponse.getInvokeId()));

    }

    @Override
    public void onProvideSubscriberLocationRequest(ProvideSubscriberLocationRequest provideSubscriberLocationRequest) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                String.format("onProvideSubscriberLocationRequest for DialogId=%d", provideSubscriberLocationRequest.getMAPDialog().getLocalDialogId()));
        } if (logger.isInfoEnabled()) {
            logger.info(String.format("onProvideSubscriberLocationResponse for DialogId=%d", provideSubscriberLocationRequest.getMAPDialog().getLocalDialogId()));
        }

        try {
            long invokeId = provideSubscriberLocationRequest.getInvokeId();
            MAPDialogLsm mapDialogLsm = provideSubscriberLocationRequest.getMAPDialog();
            mapDialogLsm.setUserObject(invokeId);

            // Create Routing Information parameters for concerning MAP operation
            MAPParameterFactoryImpl mapFactory = new MAPParameterFactoryImpl();
            ISDNAddressString mscNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123007");
            ISDNAddressString sgsnNumber = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "5982123009");
            byte[] Lmsi = hexStringToByteArray("3734383439323337343239");
            LMSI lmsi = mapFactory.createLMSI(Lmsi);
            byte[] eGeographicalInformation = hexStringToByteArray("3333b034322736322e3830272753");
            ExtGeographicalInformation extGeographicalInformation = mapFactory.createExtGeographicalInformation(eGeographicalInformation);
            byte[] posDatanformation = hexStringToByteArray("5533b034322736322e383027278a");
            PositioningDataInformation positioningDataInformation = mapFactory.createPositioningDataInformation(posDatanformation);
            UtranPositioningDataInfo utranPositioningDataInfo = null;
            Integer ageOfLocationEstimate = 1;
            byte[] addLocationEstimate = hexStringToByteArray("5533b034322736322e383027278a");
            AddGeographicalInformation additionalLocationEstimate = mapFactory.createAddGeographicalInformation(addLocationEstimate);
            MAPExtensionContainer mapExtensionContainer = null;
            boolean deferredMTLRResponseIndicator = false;
            CellGlobalIdOrServiceAreaIdFixedLength cellGlobalIdOrServiceAreaIdFixedLength = mapFactory
                .createCellGlobalIdOrServiceAreaIdFixedLength(748, 1, 23, 369);
            CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = mapFactory
                .createCellGlobalIdOrServiceAreaIdOrLAI(cellGlobalIdOrServiceAreaIdFixedLength);
            boolean saiPresent = true;
            final AccuracyFulfilmentIndicator accuracyFulfilmentIndicator = null;
            // AccuracyFulfilmentIndicator ::= ENUMERATED { requestedAccuracyFulfilled (0), requestedAccuracyNotFulfilled (1), ... }
            accuracyFulfilmentIndicator.getAccuracyFulfilmentIndicator(1);
            byte[] velEst = hexStringToByteArray("0");
            VelocityEstimate velocityEstimate = mapFactory.createVelocityEstimate(velEst);
            boolean moLrShortCircuitIndicator = false;
            GeranGANSSpositioningData geranGANSSpositioningData = null;
            UtranGANSSpositioningData utranGANSSpositioningData = null;
            ServingNodeAddress servingNodeAddress = mapFactory.createServingNodeAddressMscNumber(mscNumber);

            mapDialogLsm.addProvideSubscriberLocationResponse(invokeId, extGeographicalInformation, positioningDataInformation, utranPositioningDataInfo,
                ageOfLocationEstimate, additionalLocationEstimate, mapExtensionContainer, deferredMTLRResponseIndicator, cellGlobalIdOrServiceAreaIdOrLAI,
                saiPresent, accuracyFulfilmentIndicator, velocityEstimate, moLrShortCircuitIndicator, geranGANSSpositioningData, utranGANSSpositioningData,
                servingNodeAddress);

        } catch (MAPException mapException) {
            logger.error("MAP Exception while processing onSendRoutingInfoForLCSRequest ", mapException);
        } catch (Exception e) {
            logger.error("Exception while processing onSendRoutingInfoForLCSRequest ", e);
        }

    }

    @Override
    public void onProvideSubscriberLocationResponse(ProvideSubscriberLocationResponse provideSubscriberLocationResponse) {

        /*
         * This is an error condition. Server should never receive onProvideSubscriberLocationResponse.
         */
        logger.error(String.format("onProvideSubscriberLocationResponse for Dialog=%d and invokeId=%d",
            provideSubscriberLocationResponse.getMAPDialog().getLocalDialogId(), provideSubscriberLocationResponse.getInvokeId()));

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

    @Override
    public void onDialogRequestEricsson(MAPDialog mapDialog, AddressString destReference, AddressString origReference,
                                        IMSI imsi, AddressString vlr) {
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

        if (((this.endCount % 10000) == 0) && endCount>1) {
            long currentTime = System.currentTimeMillis();
            long processingTime = currentTime - start;
            start = currentTime;
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

    public static void main(String[] args) {
        IpChannelType ipChannelType = IpChannelType.SCTP;
        if (args.length >= 1 && args[0].toLowerCase().equals("tcp")) {
            ipChannelType = IpChannelType.TCP;
        } else {
            ipChannelType = IpChannelType.SCTP;
        }
        logger.info("IpChannelType=" + ipChannelType);

        if (args.length >= 2) {
            TestHarness3G.CLIENT_IP = args[1];
        }
        logger.info("CLIENT_IP=" + TestHarness3G.CLIENT_IP);

        if (args.length >= 3) {
            TestHarness3G.CLIENT_PORT = Integer.parseInt(args[2]);
        }
        logger.info("CLIENT_PORT=" + TestHarness3G.CLIENT_PORT);

        if (args.length >= 4) {
            TestHarness3G.SERVER_IP = args[3];
        }
        logger.info("SERVER_IP=" + TestHarness3G.SERVER_IP);

        if (args.length >= 5) {
            TestHarness3G.SERVER_PORT = Integer.parseInt(args[4]);
        }
        logger.info("SERVER_PORT=" + TestHarness3G.SERVER_PORT);

        if (args.length >= 6) {
            TestHarness3G.CLIENT_SPC = Integer.parseInt(args[5]);
        }
        logger.info("CLIENT_SPC=" + TestHarness3G.CLIENT_SPC);

        if (args.length >= 7) {
            TestHarness3G.SERVER_SPC = Integer.parseInt(args[6]);
        }
        logger.info("SERVET_SPC=" + TestHarness3G.SERVER_SPC);

        if (args.length >= 8) {
            TestHarness3G.NETWORK_INDICATOR = Integer.parseInt(args[7]);
        }
        logger.info("NETWORK_INDICATOR=" + TestHarness3G.NETWORK_INDICATOR);

        if (args.length >= 9) {
            TestHarness3G.SERVICE_INDICATOR = Integer.parseInt(args[8]);
        }
        logger.info("SERVICE_INDICATOR=" + TestHarness3G.SERVICE_INDICATOR);

        if (args.length >= 10) {
            TestHarness3G.SERVER_SSN = Integer.parseInt(args[9]);
        }
        logger.info("SSN=" + TestHarness3G.SERVER_SSN);

        if (args.length >= 11) {
            TestHarness3G.ROUTING_CONTEXT = Integer.parseInt(args[10]);
        }
        logger.info("ROUTING_CONTEXT=" + TestHarness3G.ROUTING_CONTEXT);

        if (args.length >= 12) {
            TestHarness3G.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT = Integer.parseInt(args[11]);
        }
        logger.info("DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT=" + TestHarness3G.DELIVERY_TRANSFER_MESSAGE_THREAD_COUNT);

        final Server server = new Server();
        try {
            server.initializeStack(ipChannelType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onErrorComponent(MAPDialog arg0, Long arg1, MAPErrorMessage arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long aLong, Problem problem, boolean b) {

    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long aLong) {

    }

    @Override
    public void onMAPMessage(MAPMessage mapMessage) {

    }

    @Override
    public void onSubscriberLocationReportRequest(SubscriberLocationReportRequest subscriberLocationReportRequestIndication) {

    }

    @Override
    public void onSubscriberLocationReportResponse(SubscriberLocationReportResponse subscriberLocationReportResponseIndication) {

    }

}
