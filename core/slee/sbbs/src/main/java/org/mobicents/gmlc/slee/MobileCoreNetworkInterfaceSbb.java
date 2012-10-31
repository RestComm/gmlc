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

import java.io.PrintWriter;

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
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
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
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
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

	}

	public void onProviderErrorComponent(org.mobicents.slee.resource.map.events.ProviderErrorComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	public void onRejectComponent(org.mobicents.slee.resource.map.events.RejectComponent event,
			ActivityContextInterface aci/* , EventContext eventContext */) {

	}

	/**
	 * ATI Events
	 */
	public void onAnyTimeInterrofationRequest(
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
			int cellId = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength().getCellId();
			SubscriberState ss = si.getSubscriberState();

			this.continueHttp(cellId, true);
		} catch (Exception e) {
			logger.severe(String.format("Error while trying to process AnyTimeInterrogationResponse=%s", event), e);
		}
	}

	/**
	 * HTTP Server Events
	 */
	public void onGet(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
			EventContext eventContext) {
		this.setEventContext(eventContext);

		HttpServletRequest httpServletRequest = event.getRequest();
		String destinationMSISDN = httpServletRequest.getParameter("msisdn");

		if (destinationMSISDN != null) {
			eventContext.suspendDelivery();
			try {
				MAPDialogMobility mapDialogMobility = this.mapProvider.getMAPServiceMobility().createNewDialog(
						this.getSRIMAPApplicationContext(), this.getServiceCenterSccpAddress(), null,
						convertAddressFieldToSCCPAddress(destinationMSISDN), null);

				ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
						org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, destinationMSISDN);
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
				this.logger.severe("MAPException while trying to send ATI request for MSISDN="+destinationMSISDN, e);
				this.continueHttp(-1, false);
			} catch(Exception e){
				this.logger.severe("Exception while trying to send ATI request for MSISDN="+destinationMSISDN, e);
				this.continueHttp(-1, false);
			}
		} else {
			this.logger.info("MSISDN is null sending back -1 for cellid");
			this.continueHttp(-1, false);
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

	protected SccpAddress getServiceCenterSccpAddress() {
		if (this.serviceCenterSCCPAddress == null) {
			GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL,
					gmlcPropertiesManagement.getGmlcGt());
			this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
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
		GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, address);
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
				gmlcPropertiesManagement.getHlrSsn());
	}

	private void continueHttp(int cellid, boolean resumeEventDelivery) {
		try {

			EventContext ctx = this.getEventContext();
			HttpServletRequestEvent event = (HttpServletRequestEvent) ctx.getEvent();

			HttpServletResponse response = event.getResponse();
			PrintWriter w = null;

			w = response.getWriter();
			w.print("cellid=" + cellid);
			w.flush();
			response.flushBuffer();

			if (resumeEventDelivery) {
				ctx.resumeDelivery();
			}

			logger.info("HttpServletRAExampleSbb: GET Request received and OK! response sent.");

			// getNullActivity().endActivity();
		} catch (Exception e) {
			logger.severe("Error whhile sending back HttpResponse", e);

		}
	}
}
