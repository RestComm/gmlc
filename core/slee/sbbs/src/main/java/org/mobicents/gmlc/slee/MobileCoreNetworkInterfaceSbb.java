/**
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.mobicents.gmlc.slee;

import java.io.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.mobicents.gmlc.GmlcPropertiesManagement;
import org.mobicents.gmlc.slee.mlp.MLPException;
import org.mobicents.gmlc.slee.mlp.MLPRequest;
import org.mobicents.gmlc.slee.mlp.MLPResponse;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberState;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
/*import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;*/
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;

/**
 * 
 * @author amit bhayani
 */
public abstract class MobileCoreNetworkInterfaceSbb implements Sbb {

	protected SbbContextExt sbbContext;

	private Tracer logger;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	protected static final ResourceAdaptorTypeID mapRATypeID = new ResourceAdaptorTypeID("MAPResourceAdaptorType",
			"org.mobicents", "2.0");
	protected static final String mapRaLink = "MAPRA";

	private static final GmlcPropertiesManagement gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance();

	private SccpAddress serviceCenterSCCPAddress = null;
	private MAPApplicationContext anyTimeEnquiryContext = null;

    /**
     * HTTP Request Types (GET or MLP)
     */
    private enum httpRequestTypes {
        HTTP_REQUEST_GET,
        HTTP_REQUEST_MLP
    }

    /**
     * Chosen HTTP Request Type (GET or MLP)
     */
    private httpRequestTypes httpRequestType = null;

    /**
     * MSISDN being located
     */
    private String requestingMSISDN = null;

    /**
     * Response Location
     */
    private int responseCellId = -1;
    private int responseMCC = -1;
    private int responseMNC = -1;
    private int responseLAC = -1;
    private int responseAOL = -1;
    private int responseSS = -1;


    /**
     * Response type (success, error type)
     */
    private MLPResponse.MLPResultType responseType = null;

    /**
     * For debugging - fake location data
     */
    private boolean useFakeLocation = false;
    private MLPResponse.MLPResultType fakeLocationType = MLPResponse.MLPResultType.OK;
    private String fakeLocationAdditionalInfoErrorString = "Internal positioning failure occurred";
    private int fakeCellId = 300;
    private String fakeLocationX = "27 28 25.00S";
    private String fakeLocationY = "153 01 43.00E";
    private String fakeLocationRadius = "5000";

	/** Creates a new instance of CallSbb */
	public MobileCoreNetworkInterfaceSbb() {
	}

	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;
		this.logger = sbbContext.getTracer(MobileCoreNetworkInterfaceSbb.class.getSimpleName());
		try {
			this.mapAcif = (MAPContextInterfaceFactory) this.sbbContext.getActivityContextInterfaceFactory(mapRATypeID);
			this.mapProvider = (MAPProvider) this.sbbContext.getResourceAdaptorInterface(mapRATypeID, mapRaLink);
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	public void unsetSbbContext() {
		this.sbbContext = null;
		this.logger = null;
	}

	public void sbbCreate() throws CreateException {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("Created KnowledgeBase");
		}
	}

	public void sbbPostCreate() throws CreateException {

	}

	public void sbbActivate() {
	}

	public void sbbPassivate() {
	}

	public void sbbLoad() {
	}

	public void sbbStore() {
	}

	public void sbbRemove() {
	}

	public void sbbExceptionThrown(Exception exception, Object object, ActivityContextInterface activityContextInterface) {
	}

	public void sbbRolledBack(RolledBackContext rolledBackContext) {
	}

	/**
	 * DIALOG Events
	 */

	public void onDialogTimeout(org.mobicents.slee.resource.map.events.DialogTimeout evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("Rx :  onDialogTimeout" + evt);
		}

		this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, "MAP Dialog timeout");
	}

	public void onDialogDelimiter(org.mobicents.slee.resource.map.events.DialogDelimiter event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	public void onDialogAccept(org.mobicents.slee.resource.map.events.DialogAccept event, ActivityContextInterface aci) {

	}

	public void onDialogReject(org.mobicents.slee.resource.map.events.DialogReject event, ActivityContextInterface aci) {

	}

	public void onDialogUserAbort(org.mobicents.slee.resource.map.events.DialogUserAbort event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	public void onDialogProviderAbort(org.mobicents.slee.resource.map.events.DialogProviderAbort event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	public void onDialogClose(org.mobicents.slee.resource.map.events.DialogClose event, ActivityContextInterface aci) {

	}

	public void onDialogNotice(org.mobicents.slee.resource.map.events.DialogNotice event, ActivityContextInterface aci) {

	}

	public void onDialogRelease(org.mobicents.slee.resource.map.events.DialogRelease event, ActivityContextInterface aci) {

	}

	/**
	 * Component Events
	 */
	public void onInvokeTimeout(org.mobicents.slee.resource.map.events.InvokeTimeout event, ActivityContextInterface aci) {

	}

	public void onErrorComponent(org.mobicents.slee.resource.map.events.ErrorComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		long error_code = mapErrorMessage.getErrorCode().longValue();

		this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, "ReturnErrorr: " + String.valueOf(error_code));
	}

	public void onRejectComponent(org.mobicents.slee.resource.map.events.RejectComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	/**
	 * ATI Events
	 */
	public void onAnyTimeInterrogationRequest(
			org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	public void onAnyTimeInterrogationResponse(
			org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
		try {
			MAPDialogMobility mapDialogMobility = event.getMAPDialog();
			SubscriberInfo si = event.getSubscriberInfo();
			CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = si.getLocationInformation()
					.getCellGlobalIdOrServiceAreaIdOrLAI();

			this.responseMCC = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getMCC();
			this.responseMNC = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getMNC();
			this.responseLAC = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getLac();		
            this.responseCellId = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getCellIdOrServiceAreaCode();
			this.responseAOL = si.getLocationInformation().getAgeOfLocationInformation().intValue();
			this.responseSS = si.getSubscriberState().getSubscriberStateChoice().ordinal();

            // Handle successfully having retried the device's cell-id
            this.handleLocationResponse(MLPResponse.MLPResultType.OK, null);

		} catch (Exception e) {
			logger.severe(String.format("Error while trying to process AnyTimeInterrogationResponse=%s", event), e);
            this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, "Internal failure occurred while processing network response");
		}
	}

    /**
     * Handle HTTP POST request, used for processing MLP location request
     * @param event
     * @param aci
     * @param eventContext
     */
    public void onPost(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
                      EventContext eventContext) {
        this.setEventContext(eventContext);
        this.httpRequestType = this.httpRequestType.HTTP_REQUEST_MLP;

        // Handle retrieving the request, parsing it, and generating the network location request
        try {
            // Get the XML request from the POST data
            HttpServletRequest httpServletRequest = event.getRequest();
            InputStream body = httpServletRequest.getInputStream();

            // Parse the request and retrieve the requested MSISDN
            MLPRequest mlpRequest = new MLPRequest(this.logger);
            this.requestingMSISDN = mlpRequest.parseRequest(body);

            // Send the network request for the MSISDN's location
            eventContext.suspendDelivery();
            this.getSingleMSISDNLocation();

            // result will be sent via sendHTTPResult(), which will be called by onAnyTimeInterrogationResponse() or
            // getSingleMSISDNLocation()
        }
        catch(MLPException e) {
            this.handleLocationResponse(e.getMlpClientErrorType(), e.getMlpClientErrorMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
            this.handleLocationResponse(MLPResponse.MLPResultType.FORMAT_ERROR, "Failed to read from server input stream");
        }
    }

    /**
     * Handle HTTP GET request for a single MSISDN location
     * Mostly just used for testing at the moment
     * @param event
     * @param aci
     * @param eventContext
     */
	public void onGet(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
			EventContext eventContext) {
		this.setEventContext(eventContext);
        this.httpRequestType = this.httpRequestType.HTTP_REQUEST_GET;

		HttpServletRequest httpServletRequest = event.getRequest();
		this.requestingMSISDN = httpServletRequest.getParameter("msisdn");
        if (this.requestingMSISDN != null) {
            eventContext.suspendDelivery();
            getSingleMSISDNLocation();
        } else {
            this.logger.info("MSISDN is null, sending back -1 for cellid");
            this.handleLocationResponse(MLPResponse.MLPResultType.UNKNOWN_SUBSCRIBER, "Invalid MSISDN specified");
        }
	}

	/**
	 * CMP
	 */
	public abstract void setEventContext(EventContext cntx);

	public abstract EventContext getEventContext();

	/**
	 * Private helper methods
	 */

    /**
     * Retrieve the location for the specified MSISDN via ATI request to the HLR
     */
    private void getSingleMSISDNLocation() {
        if (!useFakeLocation) {
            try {
                MAPDialogMobility mapDialogMobility = this.mapProvider.getMAPServiceMobility().createNewDialog(
                        this.getSRIMAPApplicationContext(), this.getServiceCenterSccpAddress(), null,
                        convertAddressFieldToSCCPAddress(this.requestingMSISDN), null);

                ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
                        org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, this.requestingMSISDN);
                SubscriberIdentity subsId = new SubscriberIdentityImpl(isdnAdd);
                RequestedInfo requestedInfo = new RequestedInfoImpl(true, true, null, false, null, false, false, false);
                ISDNAddressString gscmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                        org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
                        gmlcPropertiesManagement.getGmlcGt());

                mapDialogMobility.addAnyTimeInterrogationRequest(subsId, requestedInfo, gscmSCFAddress, null);

                ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogMobility);
                sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

                mapDialogMobility.send();
            } catch (MAPException e) {
                this.logger.severe("MAPException while trying to send ATI request for MSISDN=" + this.requestingMSISDN, e);
                this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, "Failed to send request to network for position");
            } catch (Exception e) {
                this.logger.severe("Exception while trying to send ATI request for MSISDN=" + this.requestingMSISDN, e);
                this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, "Failed to send request to network for position");
            }
        }
        else {
            // Handle fake success
            if (this.fakeLocationType == MLPResponse.MLPResultType.OK) {
                this.responseCellId = this.fakeCellId;
                this.handleLocationResponse(this.fakeLocationType, null);
            }
            else {
                this.responseCellId = -1;
                this.handleLocationResponse(this.fakeLocationType, this.fakeLocationAdditionalInfoErrorString);
            }
        }
    }

	protected SccpAddress getServiceCenterSccpAddress() {
		if (this.serviceCenterSCCPAddress == null) {
			GlobalTitle0100 gt = new GlobalTitle0100Impl(gmlcPropertiesManagement.getGmlcGt(),0,BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY,NatureOfAddress.INTERNATIONAL);
			this.serviceCenterSCCPAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0,
					gmlcPropertiesManagement.getGmlcSsn());
		}
		return this.serviceCenterSCCPAddress;
	}

	private MAPApplicationContext getSRIMAPApplicationContext() {
		if (this.anyTimeEnquiryContext == null) {
			this.anyTimeEnquiryContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.anyTimeEnquiryContext, MAPApplicationContextVersion.version3);
		}
		return this.anyTimeEnquiryContext;
	}

	private SccpAddress convertAddressFieldToSCCPAddress(String address) {
		GlobalTitle0100 gt = new GlobalTitle0100Impl(address, 0, BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL);
		return new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0,
				gmlcPropertiesManagement.getHlrSsn());
	}

    /**
     * Handle generating the appropriate HTTP response
     * We're making use of the MLPResponse class for both GET/POST requests for convenience and
     * because eventually the GET method will likely be removed
     * @param mlpResultType OK or error type to return to client
     * @param mlpClientErrorMessage Error message to send to client
     */
    private void handleLocationResponse(MLPResponse.MLPResultType mlpResultType, String mlpClientErrorMessage) {
        switch(this.httpRequestType)
        {
            case HTTP_REQUEST_GET:
                if (mlpResultType == MLPResponse.MLPResultType.OK) {
                    this.sendHTTPResult("mcc=" + this.responseMCC + ",mnc=" + this.responseMNC
                    		+ ",lac=" + this.responseLAC + ",cellid=" + this.responseCellId
                    			+ ",aol=" + this.responseAOL + ",ss=" +this.responseSS, true);
                }
                else {
                    this.sendHTTPResult(mlpClientErrorMessage, false);
                }
                break;

            case HTTP_REQUEST_MLP:
                String svcResultXml;
                MLPResponse mlpResponse = new MLPResponse(this.logger);

                if (mlpResultType == MLPResponse.MLPResultType.OK) {
                    svcResultXml = mlpResponse.getSinglePositionSuccessXML(this.fakeLocationX, this.fakeLocationY, this.fakeLocationRadius, this.requestingMSISDN);
                }
                else if (MLPResponse.isSystemError(mlpResultType)) {
                    svcResultXml = mlpResponse.getSystemErrorResponseXML(mlpResultType, mlpClientErrorMessage);
                }
                else {
                    svcResultXml = mlpResponse.getPositionErrorResponseXML(this.requestingMSISDN, mlpResultType, mlpClientErrorMessage);
                }

                this.logger.info("Generated response XML: "+svcResultXml);
                this.sendHTTPResult(svcResultXml, true);
                break;
        }
    }

    /**
     * Return the specified response data to the HTTP client
     * @param responseData Response data to send to client
     * @param resumeEventDelivery If true, ctx.resumeDelivery() will be called
     */
	private void sendHTTPResult(String responseData, boolean resumeEventDelivery) {
		try {
			EventContext ctx = this.getEventContext();
			HttpServletRequestEvent event = (HttpServletRequestEvent) ctx.getEvent();

			HttpServletResponse response = event.getResponse();
            PrintWriter w = null;
            w = response.getWriter();
            w.print(responseData);
			w.flush();
			response.flushBuffer();

			if (resumeEventDelivery) {
				ctx.resumeDelivery();
			}

			logger.info("HTTP Request received and response sent.");

			// getNullActivity().endActivity();
		} catch (Exception e) {
			logger.severe("Error while sending back HTTP response", e);
		}
	}
}
