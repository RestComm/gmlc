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
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 */
public abstract class MobileCoreNetworkInterfaceSbb implements Sbb {

	protected SbbContextExt sbbContext;

	private Tracer logger;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;
	protected ParameterFactory sccpParameterFact;

	protected static final ResourceAdaptorTypeID mapRATypeID = new ResourceAdaptorTypeID("MAPResourceAdaptorType",
			"org.mobicents", "2.0");
	protected static final String mapRaLink = "MAPRA";

	private static final GmlcPropertiesManagement gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance();

	private SccpAddress gmlcSCCPAddress = null;
	private MAPApplicationContext anyTimeEnquiryContext = null;

    /**
     * HTTP Request Types (GET or MLP)
     */
    private enum HttpRequestType {
        REST("rest"),
        MLP("mlp"),
        UNSUPPORTED("404");

        private String path;

        HttpRequestType(String path) {
            this.path = path;
        }

        public String getPath() {
            return String.format("/gmlc/%s", path);
        }

        public static HttpRequestType fromPath(String path) {
            for (HttpRequestType type: values()) {
                if (path.equals(type.getPath())) {
                    return type;
                }
            }

            return UNSUPPORTED;
        }
    }

    /**
     * Request
     */
    private class HttpRequest implements Serializable {
        HttpRequestType type;
        String msisdn;

        public HttpRequest(HttpRequestType type, String msisdn) {
            this.type = type;
            this.msisdn = msisdn;
        }

        public HttpRequest(HttpRequestType type) {
            this(type, "");
        }
    }

    /**
     * Response Location
     */
    private class CGIResponse implements Serializable {
        String x;
        String y;
        String radius;
        int cell = -1;
        int mcc = -1;
        int mnc = -1;
        int lac = -1;
        int aol = -1;
        String vlr = "-1";
    }

    /**
     * For debugging - fake location data
     */
    private String fakeNumber = "19395550113";
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
			this.sccpParameterFact = new ParameterFactoryImpl();
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
        this.logger.severe("\nRx :  onDialogTimeout " + evt);

		this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogTimeout");
	}

	public void onDialogDelimiter(org.mobicents.slee.resource.map.events.DialogDelimiter event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onDialogDelimiter = " + event);
        }
	}

	public void onDialogAccept(org.mobicents.slee.resource.map.events.DialogAccept event, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onDialogAccept = " + event);
        }
	}

	public void onDialogReject(org.mobicents.slee.resource.map.events.DialogReject event, ActivityContextInterface aci) {
        this.logger.severe("\nRx :  onDialogReject " + event);

        this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogReject: " + event);
	}

	public void onDialogUserAbort(org.mobicents.slee.resource.map.events.DialogUserAbort event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        this.logger.severe("\nRx :  onDialogUserAbort " + event);

        this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogUserAbort: " + event);
	}

	public void onDialogProviderAbort(org.mobicents.slee.resource.map.events.DialogProviderAbort event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        this.logger.severe("\nRx :  onDialogProviderAbort " + event);

        this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogProviderAbort: " + event);
	}

	public void onDialogClose(org.mobicents.slee.resource.map.events.DialogClose event, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onDialogClose = " + event);
        }
	}

	public void onDialogNotice(org.mobicents.slee.resource.map.events.DialogNotice event, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onDialogNotice = " + event);
        }
	}

	public void onDialogRelease(org.mobicents.slee.resource.map.events.DialogRelease event, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onDialogRelease = " + event);
        }
	}

	/**
	 * Component Events
	 */
	public void onInvokeTimeout(org.mobicents.slee.resource.map.events.InvokeTimeout event, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onInvokeTimeout = " + event);
        }
	}

	public void onErrorComponent(org.mobicents.slee.resource.map.events.ErrorComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived onErrorComponent = " + event);
        }

		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		long error_code = mapErrorMessage.getErrorCode().longValue();

        this.handleLocationResponse(
                (error_code == MAPErrorCode.unknownSubscriber ? MLPResponse.MLPResultType.UNKNOWN_SUBSCRIBER
                        : MLPResponse.MLPResultType.SYSTEM_FAILURE), null, "ReturnError: " + String.valueOf(error_code) + " : "
                        + event.getMAPErrorMessage());
	}

	public void onRejectComponent(org.mobicents.slee.resource.map.events.RejectComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        this.logger.severe("\nRx :  onRejectComponent " + event);

        this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "RejectComponent: " + event);
	}

	/**
	 * ATI Events
	 */
	public void onAnyTimeInterrogationRequest(
			org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
        this.logger.severe("\nReceived onAnyTimeInterrogationRequest = " + event);
	}

	public void onAnyTimeInterrogationResponse(
			org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse event,
			ActivityContextInterface aci/* , EventContext eventContext */) {
		try {
	        if (this.logger.isFineEnabled()) {
	            this.logger.fine("\nReceived onAnyTimeInterrogationResponse = " + event);
	        }

	        MAPDialogMobility mapDialogMobility = event.getMAPDialog();
			SubscriberInfo si = event.getSubscriberInfo();
            MLPResponse.MLPResultType result;
            CGIResponse response = new CGIResponse();
            String mlpClientErrorMessage = null;

            if (si != null) {
                if (si.getLocationInformation() != null) {
                    result = MLPResponse.MLPResultType.OK;
                    if (si.getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI() != null) {
                        CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = si.getLocationInformation()
                                .getCellGlobalIdOrServiceAreaIdOrLAI();
                        if (cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength() != null) {
                            response.mcc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                                    .getMCC();
                            response.mnc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                                    .getMNC();
                            response.lac = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                                    .getLac();
                            response.cell = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                                    .getCellIdOrServiceAreaCode();
                        }
                    }

                    if (si.getLocationInformation().getAgeOfLocationInformation() != null) {
                        response.aol = si.getLocationInformation().getAgeOfLocationInformation().intValue();
                    }

                    if (si.getLocationInformation().getVlrNumber() != null) {
                        response.vlr = si.getLocationInformation().getVlrNumber().getAddress();
                    }
                } else if (si.getSubscriberState() != null) {
                    result = MLPResponse.MLPResultType.ABSENT_SUBSCRIBER;
                    mlpClientErrorMessage = "SubscriberState: " + si.getSubscriberState();
                } else {
                    result = MLPResponse.MLPResultType.SYSTEM_FAILURE;
                    mlpClientErrorMessage = "Bad SubscriberInfo received: " + si;
                }
            } else {
                result = MLPResponse.MLPResultType.SYSTEM_FAILURE;
                mlpClientErrorMessage = "Bad AnyTimeInterrogationResponse received: " + event;
            }

            // Handle successfully having retried the device's cell-id
            this.handleLocationResponse(result, response, mlpClientErrorMessage);

		} catch (Exception e) {
            logger.severe(String.format("Error while trying to process AnyTimeInterrogationResponse=%s", event), e);
            this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
                    "Internal failure occurred while processing network response: " + e.getMessage());
		}
	}

    /**
     * Handle HTTP POST request
     * @param event
     * @param aci
     * @param eventContext
     */
    public void onPost(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
                      EventContext eventContext) {
        onRequest(event, aci, eventContext);
    }

    /**
     * Handle HTTP GET request
     * @param event
     * @param aci
     * @param eventContext
     */
	public void onGet(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
			EventContext eventContext) {
        onRequest(event, aci, eventContext);
	}

    /**
     * Entry point for all location lookups
     * Assigns a protocol handler to the request based on the path
     */
    private void onRequest(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
                           EventContext eventContext) {
        setEventContext(eventContext);
        HttpServletRequest httpServletRequest = event.getRequest();
        HttpRequestType httpRequestType = HttpRequestType.fromPath(httpServletRequest.getPathInfo());
        setHttpRequest(new HttpRequest(httpRequestType));
        String requestingMSISDN = null;

        switch (httpRequestType) {
            case REST:
                requestingMSISDN = httpServletRequest.getParameter("msisdn");
                break;
            case MLP:
                try {
                    // Get the XML request from the POST data
                    InputStream body = httpServletRequest.getInputStream();
                    // Parse the request and retrieve the requested MSISDN
                    MLPRequest mlpRequest = new MLPRequest(logger);
                    requestingMSISDN = mlpRequest.parseRequest(body);
                } catch(MLPException e) {
                    handleLocationResponse(e.getMlpClientErrorType(), null, "System Failure: " + e.getMlpClientErrorMessage());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    handleLocationResponse(MLPResponse.MLPResultType.FORMAT_ERROR, null, "System Failure: Failed to read from server input stream");
                    return;
                }
                break;
            default:
                event.getResponse().setStatus(404);
                sendHTTPResult("Request URI unsupported");
                return;
        }

        setHttpRequest(new HttpRequest(httpRequestType, requestingMSISDN));
        logger.info(String.format("Handling %s request, msisdn: %s", httpRequestType.name().toUpperCase(), requestingMSISDN));

        if (requestingMSISDN != null) {
            eventContext.suspendDelivery();
            getSingleMSISDNLocation(requestingMSISDN);
        } else {
            logger.info("MSISDN is null, sending back -1 for cellid");
            handleLocationResponse(MLPResponse.MLPResultType.FORMAT_ERROR, null, "Invalid MSISDN specified");
        }
    }

	/**
	 * CMP
	 */
	public abstract void setEventContext(EventContext cntx);

	public abstract EventContext getEventContext();

    public abstract void setHttpRequest(HttpRequest httpRequest);

    public abstract HttpRequest getHttpRequest();

	/**
	 * Private helper methods
	 */

    /**
     * Retrieve the location for the specified MSISDN via ATI request to the HLR
     */
    private void getSingleMSISDNLocation(String requestingMSISDN) {
        if (!requestingMSISDN.equals(fakeNumber)) {
            try {
                MAPDialogMobility mapDialogMobility = this.mapProvider.getMAPServiceMobility().createNewDialog(
                        this.getSRIMAPApplicationContext(), this.getGmlcSccpAddress(), null,
                        getHlrSCCPAddress(requestingMSISDN), null);

                ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
                        org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, requestingMSISDN);
                SubscriberIdentity subsId = new SubscriberIdentityImpl(isdnAdd);
                RequestedInfo requestedInfo = new RequestedInfoImpl(true, true, null, false, null, false, false, false);
                // requestedInfo (MAP ATI): last known location and state (idle or busy), no IMEI/MS Classmark/MNP
                ISDNAddressString gscmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                        org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
                        gmlcPropertiesManagement.getGmlcGt());

                mapDialogMobility.addAnyTimeInterrogationRequest(subsId, requestedInfo, gscmSCFAddress, null);

                ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogMobility);
                sriDialogACI.attach(this.sbbContext.getSbbLocalObject());
                mapDialogMobility.send();
            } catch (MAPException e) {
                this.logger.severe("MAPException while trying to send ATI request for MSISDN=" + requestingMSISDN, e);
                this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
                        "System Failure: Failed to send request to network for position: " + e.getMessage());
            } catch (Exception e) {
                this.logger.severe("Exception while trying to send ATI request for MSISDN=" + requestingMSISDN, e);
                this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
                        "System Failure: Failed to send request to network for position: " + e.getMessage());
            }
        }
        else {
            // Handle fake success
            if (this.fakeLocationType == MLPResponse.MLPResultType.OK) {
                CGIResponse response = new CGIResponse();
                response.cell = fakeCellId;
                response.x = fakeLocationX;
                response.y = fakeLocationY;
                response.radius = fakeLocationRadius;
                this.handleLocationResponse(MLPResponse.MLPResultType.OK, response, null);
            }
            else {
                this.handleLocationResponse(this.fakeLocationType, null, this.fakeLocationAdditionalInfoErrorString);
            }
        }
    }

	protected SccpAddress getGmlcSccpAddress() {
		if (this.gmlcSCCPAddress == null) {
            GlobalTitle gt = sccpParameterFact.createGlobalTitle(gmlcPropertiesManagement.getGmlcGt(), 0,
                    NumberingPlan.ISDN_TELEPHONY, null, NatureOfAddress.INTERNATIONAL);
            this.gmlcSCCPAddress = sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
                    gt, 0, gmlcPropertiesManagement.getGmlcSsn());

//			GlobalTitle0100 gt = new GlobalTitle0100Impl(gmlcPropertiesManagement.getGmlcGt(),0,BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY,NatureOfAddress.INTERNATIONAL);
//			this.serviceCenterSCCPAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, gmlcPropertiesManagement.getGmlcSsn());
		}
		return this.gmlcSCCPAddress;
	}

	private MAPApplicationContext getSRIMAPApplicationContext() {
		if (this.anyTimeEnquiryContext == null) {
			this.anyTimeEnquiryContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.anyTimeEnquiryContext, MAPApplicationContextVersion.version3);
		}
		return this.anyTimeEnquiryContext;
	}

	private SccpAddress getHlrSCCPAddress(String address) {
        GlobalTitle gt = sccpParameterFact.createGlobalTitle(address, 0, NumberingPlan.ISDN_TELEPHONY, null,
                NatureOfAddress.INTERNATIONAL);
        return sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0,
                gmlcPropertiesManagement.getHlrSsn());

//	    GlobalTitle0100 gt = new GlobalTitle0100Impl(address, 0, BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL);
//		return new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, gmlcPropertiesManagement.getHlrSsn());
	}

    /**
     * Handle generating the appropriate HTTP response
     * We're making use of the MLPResponse class for both GET/POST requests for convenience and
     * because eventually the GET method will likely be removed
     * @param mlpResultType OK or error type to return to client
     * @param response CGIResponse on location attempt
     * @param mlpClientErrorMessage Error message to send to client
     */
    private void handleLocationResponse(MLPResponse.MLPResultType mlpResultType, CGIResponse response, String mlpClientErrorMessage) {
        HttpRequest request = getHttpRequest();

        switch(request.type)
        {
            case REST:
                if (mlpResultType == MLPResponse.MLPResultType.OK) {
                	StringBuilder getResponse = new StringBuilder();
					getResponse.append("mcc=");
					getResponse.append(response.mcc);
					getResponse.append(",mnc=");
					getResponse.append(response.mnc);
					getResponse.append(",lac=");
					getResponse.append(response.lac);
					getResponse.append(",cellid=");
					getResponse.append(response.cell);
					getResponse.append(",aol=");
					getResponse.append(response.aol);
					getResponse.append(",vlrNumber=");
					getResponse.append(response.vlr);

                    this.sendHTTPResult(getResponse.toString());
                }
                else {
                    this.sendHTTPResult(mlpClientErrorMessage);
                }
                break;

            case MLP:
                String svcResultXml;
                MLPResponse mlpResponse = new MLPResponse(this.logger);

                if (mlpResultType == MLPResponse.MLPResultType.OK) {
                    svcResultXml = mlpResponse.getSinglePositionSuccessXML(response.x, response.y, response.radius, request.msisdn);
                }
                else if (MLPResponse.isSystemError(mlpResultType)) {
                    svcResultXml = mlpResponse.getSystemErrorResponseXML(mlpResultType, mlpClientErrorMessage);
                }
                else {
                    svcResultXml = mlpResponse.getPositionErrorResponseXML(request.msisdn, mlpResultType, mlpClientErrorMessage);
                }

                this.sendHTTPResult(svcResultXml);
                break;
        }
    }

    /**
     * Return the specified response data to the HTTP client
     * @param responseData Response data to send to client
     */
	private void sendHTTPResult(String responseData) {
		try {
			EventContext ctx = this.getEventContext();
            if (ctx == null) {
                if (logger.isWarningEnabled()) {
                    logger.warning("When responding to HTTP no pending HTTP request is found, responseData=" + responseData);
                    return;
                }
            }

	        HttpServletRequestEvent event = (HttpServletRequestEvent) ctx.getEvent();

			HttpServletResponse response = event.getResponse();
            PrintWriter w = null;
            w = response.getWriter();
            w.print(responseData);
			w.flush();
			response.flushBuffer();

			if (ctx.isSuspended()) {
				ctx.resumeDelivery();
			}

			logger.info("HTTP Request received and response sent, responseData=" + responseData);

			// getNullActivity().endActivity();
		} catch (Exception e) {
			logger.severe("Error while sending back HTTP response", e);
		}
	}
}
