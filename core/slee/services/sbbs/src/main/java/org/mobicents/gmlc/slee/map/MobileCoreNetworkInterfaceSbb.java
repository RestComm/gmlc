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

package org.mobicents.gmlc.slee.map;

import java.io.Serializable;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.*;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;

import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.joda.time.DateTime;
import org.mobicents.gmlc.slee.GMLCBaseSbb;
import org.mobicents.gmlc.slee.mlp.MLPException;
import org.mobicents.gmlc.slee.mlp.MLPRequest;
import org.mobicents.gmlc.slee.mlp.MLPResponse;
import org.mobicents.gmlc.GmlcPropertiesManagement;
import org.mobicents.gmlc.slee.cdr.GMLCCDRState;
import org.mobicents.gmlc.slee.cdr.RecordStatus;
import org.mobicents.gmlc.slee.cdr.CDRInterface;
import org.mobicents.gmlc.slee.cdr.CDRInterfaceParent;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdFixedLength;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;

import org.mobicents.protocols.ss7.map.api.service.lsm.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.DomainType;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.slee.resource.map.events.ErrorComponent;
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
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;

/**
 * @author <a href="mailto:abhayani@gmail.com"> Amit Bhayani </a>
 * @author <a href="mailto:serg.vetyutnev@gmail.com"> Sergey Vetyutnev </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 * @author <a href="mailto:nhanth87@gmail.com"> Tran Huu Nhan </a>
 * @author <a href="mailto:eross@locatrix.com"> Andrew Eross </a>
 * @author <a href="mailto:lucas@locatrix.com"> Lucas Brown </a>
 * @modify <a href="mailto:l.dawoud@mobiadd.co.uk"> Loay Dawoud </a>
 */
public abstract class MobileCoreNetworkInterfaceSbb extends GMLCBaseSbb implements Sbb, CDRInterfaceParent {

  protected SbbContextExt sbbContext;

  protected Tracer logger;

  protected MAPContextInterfaceFactory mapAcif;
  protected MAPProvider mapProvider;
  protected MAPParameterFactory mapParameterFactory;
  protected ParameterFactory sccpParameterFact;

  protected static final ResourceAdaptorTypeID mapRATypeID = new ResourceAdaptorTypeID("MAPResourceAdaptorType",
      "org.mobicents", "2.0");
  protected static final String mapRaLink = "MAPRA";

  private static final GmlcPropertiesManagement gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance();

  private SccpAddress gmlcSCCPAddress = null;
  private MobileCoreNetworkGeneration MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.GSM;
  private MAPApplicationContext anyTimeEnquiryContext = null;
  private MAPApplicationContext locationSvcGatewayContext = null;
  private MAPApplicationContext locationSvcEnquiryContext = null;

  private TimerFacility timerFacility = null;

  /**
   * Mobile Core Network generation classification, this will affect MAP Application Context for MAP ATI,
   * or more sophisticated positioning methods, involving SMLC and the UTRAN, or Diameter-based location procedures,
   * involving E-SMLC and the E-UTRAN.
   */
  private enum MobileCoreNetworkGeneration {
    GSM,
    UMTS,
    LTE,
  }

  /**
   * Creates a new instance of CallSbb
   */
  public MobileCoreNetworkInterfaceSbb() {
    super("MobileCoreNetworkInterfaceSbb");
  }

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
      for (HttpRequestType type : values()) {
        if (path.equals(type.getPath())) {
          return type;
        }
      }

      return UNSUPPORTED;
    }
  }

  /**
   * HTTP Request
   */
  private class HttpRequest implements Serializable {
    HttpRequestType type;
    String msisdn;
    String serviceid;

    public HttpRequest(HttpRequestType type, String msisdn, String serviceid) {
      this.type = type;
      this.msisdn = msisdn;
      this.serviceid = serviceid;
    }

    public HttpRequest(HttpRequestType type) {
      this(type, "", "");
    }
  }

  /**
   * MAP ATI Response Cell Global Identification and State parameters
   */
  private class ATIResponse implements Serializable {
    /*** MLP Response ***/
    /*******************/
    String x = "-1";
    String y = "-1";
    String radius = "-1";
    /*** CGIorSAIorLAI ***/
    /********************/
    int cell = -1;
    int mcc = -1;
    int mnc = -1;
    int lac = -1;
    int aol = -1;
    String vlr = "-1";
    /*** Subscriber State ***/
    /***********************/
    String subscriberState = "unknown";
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

  ////////////////////
  // Sbb callbacks //
  //////////////////
  public void setSbbContext(SbbContext sbbContext) {
    this.sbbContext = (SbbContextExt) sbbContext;
    this.logger = sbbContext.getTracer(MobileCoreNetworkInterfaceSbb.class.getSimpleName());
    try {
      this.mapAcif = (MAPContextInterfaceFactory) this.sbbContext.getActivityContextInterfaceFactory(mapRATypeID);
      this.mapProvider = (MAPProvider) this.sbbContext.getResourceAdaptorInterface(mapRATypeID, mapRaLink);
      this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
      this.sccpParameterFact = new ParameterFactoryImpl();
      this.timerFacility = this.sbbContext.getTimerFacility();
    } catch (Exception ne) {
      logger.severe("Could not set SBB context:", ne);
    }
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

  private void forwardEvent(SbbLocalObject child, ActivityContextInterface aci) {
    try {
      aci.attach(child);
      aci.detach(sbbContext.getSbbLocalObject());
    } catch (Exception e) {
      logger.severe("Unexpected error: ", e);
    }
  }

  // //////////////////////
  // MAP Stuff handlers //
  // ////////////////////

  /**
   * Subscriber Information services
   * MAP_ANY_TIME_INTERROGATION (ATI) Events
   */

  /*
   * MAP ATI Request Event
   */
  public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest event, ActivityContextInterface aci) {
    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onAnyTimeInterrogationRequest = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.GSM;

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onAnyTimeInterrogationRequest=%s", event), e);
    }

  }

  /*
   * MAP ATI Response Event
   */
  public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse event, ActivityContextInterface aci) {

    try {
      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.GSM;
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onAnyTimeInterrogationResponse = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.GSM;
      MAPDialogMobility mapDialogMobility = event.getMAPDialog();
      SubscriberInfo subscriberInfo = event.getSubscriberInfo();
      ATIResponse atiResponse = new ATIResponse();
      MLPResponse.MLPResultType result;
      String mlpClientErrorMessage = null;

      // CDR initialization stuff
      CDRInterface cdrInterface = this.getCDRInterface();
      GMLCCDRState gmlcCdrState = cdrInterface.getState();
      if (!gmlcCdrState.isInitialized()) {
        if (this.logger.isFineEnabled()) {
          this.logger.fine("\nonAnyTimeInterrogationResponse: CDR state is NOT initialized: " + gmlcCdrState + ", initiating\n");
        }
        gmlcCdrState.init(mapDialogMobility.getLocalDialogId(), mapDialogMobility.getReceivedDestReference(), mapDialogMobility.getReceivedOrigReference(),
            subscriberInfo.getLocationInformation().getVlrNumber(), mapDialogMobility.getLocalAddress(), mapDialogMobility.getRemoteAddress());
        gmlcCdrState.setDialogStartTime(DateTime.now());
        gmlcCdrState.setRemoteDialogId(mapDialogMobility.getRemoteDialogId());
        cdrInterface.setState(gmlcCdrState);

        // attach, in case impl wants to use more of dialog.
        SbbLocalObject sbbLO = (SbbLocalObject) cdrInterface;
        aci.attach(sbbLO);
      }
      // Set timer last
      this.setTimer(aci);

      // Inquire if MAP ATI response includes subscriber's info
      if (subscriberInfo != null) {
        result = MLPResponse.MLPResultType.OK;
        // Inquire if subscriber state is included in MAP ATI response subscriber's info
        if (subscriberInfo.getSubscriberState() != null) {
          result = MLPResponse.MLPResultType.OK;
          // Subscriber state is included in MAP ATI response, get it and store it as a response parameter
          atiResponse.subscriberState = subscriberInfo.getSubscriberState().getSubscriberStateChoice().toString();
          if (gmlcCdrState.isInitialized()) {
            gmlcCdrState.setSubscriberState(atiResponse.subscriberState);
            if (subscriberInfo.getLocationInformation() == null) {
              if (this.logger.isFineEnabled()) {
                this.logger.fine("\nonAnyTimeInterrogationResponse: "
                    + "CDR state is initialized, ATI_STATE_SUCCESS");
              }
              this.createCDRRecord(RecordStatus.ATI_STATE_SUCCESS);
            }
          }
        }
        // Inquire if Location information is included in MAP ATI response subscriber's info
        if (subscriberInfo.getLocationInformation() != null) {
          result = MLPResponse.MLPResultType.OK;
          // Location information is included in MAP ATI response, then
          // Inquire if Cell Global Identity (CGI) or Service Area Identity (SAI) or Location Area Identity (LAI) are included in MAP ATI response
          if (subscriberInfo.getLocationInformation().getCellGlobalIdOrServiceAreaIdOrLAI() != null) {
            // CGI or SAI or LAI are included in MAP ATI response
            CellGlobalIdOrServiceAreaIdOrLAI cellGlobalIdOrServiceAreaIdOrLAI = subscriberInfo.getLocationInformation()
                .getCellGlobalIdOrServiceAreaIdOrLAI();
            // Inquire and get parameters of CGI or SAI or LAI included in MAP ATI response
            if (cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength() != null) {
              if (this.logger.isFineEnabled()) {
                this.logger.fine("\nonAnyTimeInterrogationResponse: "
                    + "received CellGlobalIdOrServiceAreaIdFixedLength, decoding MCC, MNC, LAC, CI");
              }
              atiResponse.mcc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                  .getMCC();
              atiResponse.mnc = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                  .getMNC();
              atiResponse.lac = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                  .getLac();
              atiResponse.cell = cellGlobalIdOrServiceAreaIdOrLAI.getCellGlobalIdOrServiceAreaIdFixedLength()
                  .getCellIdOrServiceAreaCode();
              // Inquire if Age of Location Information is included in MAP ATI response subscriber's info
              if (subscriberInfo.getLocationInformation().getAgeOfLocationInformation() != null) {
                atiResponse.aol = subscriberInfo.getLocationInformation().getAgeOfLocationInformation().intValue();
              }
              // Inquire if VLR number (Global Title) is included in MAP ATI response subscriber's info
              if (subscriberInfo.getLocationInformation().getVlrNumber() != null) {
                atiResponse.vlr = subscriberInfo.getLocationInformation().getVlrNumber().getAddress();
              }
              if (gmlcCdrState.isInitialized()) {
                if (this.logger.isFineEnabled()) {
                  this.logger.fine("\nonAnyTimeInterrogationResponse: "
                      + "CDR state is initialized, ATI_CGI_SUCCESS");
                }
                gmlcCdrState.setMcc(atiResponse.mcc);
                gmlcCdrState.setMnc(atiResponse.mnc);
                gmlcCdrState.setLac(atiResponse.lac);
                gmlcCdrState.setCi(atiResponse.cell);
                gmlcCdrState.setAol(atiResponse.aol);
                gmlcCdrState.setAtiVlrGt(atiResponse.vlr);
                if (gmlcCdrState.getSubscriberState() != null) {
                  this.createCDRRecord(RecordStatus.ATI_CGI_STATE_SUCCESS);
                } else {
                  this.createCDRRecord(RecordStatus.ATI_CGI_SUCCESS);
                }
              }
            } else if (cellGlobalIdOrServiceAreaIdOrLAI.getLAIFixedLength() != null) {
              // Case when LAI length is fixed
              if (this.logger.isFineEnabled()) {
                this.logger.fine("\nonAnyTimeInterrogationResponse: "
                    + "received laiFixedLength, decoding MCC, MNC, LAC (no CI)");
              }
              atiResponse.mcc = cellGlobalIdOrServiceAreaIdOrLAI.getLAIFixedLength().getMCC();
              atiResponse.mnc = cellGlobalIdOrServiceAreaIdOrLAI.getLAIFixedLength().getMNC();
              atiResponse.lac = cellGlobalIdOrServiceAreaIdOrLAI.getLAIFixedLength().getLac();
              if (gmlcCdrState.isInitialized()) {
                if (this.logger.isFineEnabled()) {
                  this.logger.fine("\nonAnyTimeInterrogationResponse: "
                      + "CDR state is initialized, ATI_LAI_SUCCESS");
                }
                gmlcCdrState.setMcc(atiResponse.mcc);
                gmlcCdrState.setMnc(atiResponse.mnc);
                gmlcCdrState.setLac(atiResponse.lac);
                if (gmlcCdrState.getSubscriberState() != null) {
                  this.createCDRRecord(RecordStatus.ATI_LAI_STATE_SUCCESS);
                } else {
                  this.createCDRRecord(RecordStatus.ATI_LAI_SUCCESS);
                }
              }
            }
          }
        }

      } else {
        if (gmlcCdrState.isInitialized()) {
          this.createCDRRecord(RecordStatus.ATI_CGI_OR_LAI_OR_STATE_FAILURE);
        }
        result = MLPResponse.MLPResultType.SYSTEM_FAILURE;
        mlpClientErrorMessage = "Bad AnyTimeInterrogationResponse received: " + event;
      }
      // Handle successful retrieval of subscriber's info
      this.handleLocationResponse(result, atiResponse, mlpClientErrorMessage);

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process AnyTimeInterrogationResponse=%s", event), e);
      this.createCDRRecord(RecordStatus.ATI_SYSTEM_FAILURE);
      this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
          "Internal failure occurred while processing network response: " + e.getMessage());
    }
  }

  /**
   * Location Service Management (LSM) services
   * MAP_SEND_ROUTING_INFO_FOR_LCS (SRIforLCS) Events
   */

  /*
   * MAP SRIforLCS Request Event
   */
  public void onSendRoutingInfoForLCSRequest(SendRoutingInfoForLCSRequest event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onSendRoutingInfoForLCSRequest = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onSendRoutingInfoForLCSRequest=%s", event), e);
    }
  }

  /*
   * MAP SRIforLCS Response Event
   */
  public void onSendRoutingInfoForLCSResponse(SendRoutingInfoForLCSResponse event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onSendRoutingInfoForLCSResponse = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

      /**********************************************/
      /*** DEVELOPED for commercial version only ***/
      /********************************************/

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process SendRoutingInfoForLCSResponse=%s", event), e);
      this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
          "Internal failure occurred while processing network response: " + e.getMessage());
    }
  }

  /**
   * Location Service Management (LSM) services
   * MAP_PROVIDE_SUBSCRIBER_LOCATION (PSL) Events
   */

  /*
   * MAP PSL Request Event
   */
  public void onProvideSubscriberLocationRequest(ProvideSubscriberLocationRequest event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onProvideSubscriberLocationRequest = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

      /**********************************************/
      /*** DEVELOPED for commercial version only ***/
      /********************************************/

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onProvideSubscriberLocationRequest=%s", event), e);
    }
  }

  /*
   * MAP PSL Response Event
   */
  public void onProvideSubscriberLocationResponse(ProvideSubscriberLocationResponse event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onProvideSubscriberLocationResponse = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

      /**********************************************/
      /*** DEVELOPED for commercial version only ***/
      /********************************************/

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onProvideSubscriberLocationResponse=%s", event), e);
    }
  }

  /**
   * Location Service Management (LSM) services
   * MAP_SUBSCRIBER_LOCATION_REPORT (SLR) Events
   */

  /*
   * MAP SLR Request Event
   */
  public void onSubscriberLocationReportRequest(SubscriberLocationReportRequest event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onSubscriberLocationReportRequest = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

      /**********************************************/
      /*** DEVELOPED for commercial version only ***/
      /********************************************/

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onSubscriberLocationReportRequest=%s", event), e);
    }
  }

  /*
   * MAP SLR Response Event
   */
  public void onSubscriberLocationReportResponse(SubscriberLocationReportResponse event, ActivityContextInterface aci) {

    try {
      if (this.logger.isFineEnabled()) {
        this.logger.fine("\nReceived onSubscriberLocationReportResponse = " + event);
      }

      MobileCoreNetworkGenerationLocationType = MobileCoreNetworkGeneration.UMTS;

      /**********************************************/
      /*** DEVELOPED for commercial version only ***/
      /********************************************/

    } catch (Exception e) {
      logger.severe(String.format("Error while trying to process onSubscriberLocationReportResponse=%s", event), e);
      this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null,
          "Internal failure occurred while processing network response: " + e.getMessage());
    }

  }

    /**
     * DIALOG Events
     */
  public void onDialogTimeout(DialogTimeout event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nRx :  onDialogTimeout " + event);
    }
    this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogTimeout");
  }

  public void onDialogDelimiter(DialogDelimiter event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onDialogDelimiter = " + event);
    }
  }

  public void onDialogAccept(DialogAccept event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onDialogAccept = " + event);
    }
  }

  public void onDialogReject(DialogReject event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nRx :  onDialogReject " + event);
    }
    this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogReject: " + event);
  }

  public void onDialogUserAbort(DialogUserAbort event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nRx :  onDialogUserAbort " + event);
    }
    this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogUserAbort: " + event);
  }

  public void onDialogProviderAbort(DialogProviderAbort event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nRx :  onDialogProviderAbort " + event);
    }
    this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "DialogProviderAbort: " + event);
  }

  public void onDialogClose(DialogClose event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onDialogClose = " + event);
    }
  }

  public void onDialogNotice(DialogNotice event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onDialogNotice = " + event);
    }
  }

  public void onDialogRelease(DialogRelease event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onDialogRelease = " + event);
    }
  }

  /**
   * Component Events
   */
  public void onInvokeTimeout(InvokeTimeout event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onInvokeTimeout = " + event);
    } else {
      this.logger.severe("\nReceived onInvokeTimeout = " + event);
    }
  }

  public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nReceived onErrorComponent = " + event);
    } else {
      this.logger.severe("\nReceived onErrorComponent = " + event);
    }

    MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
    long error_code = mapErrorMessage.getErrorCode().longValue();

    this.handleLocationResponse(
        (error_code == MAPErrorCode.unknownSubscriber ? MLPResponse.MLPResultType.UNKNOWN_SUBSCRIBER
            : MLPResponse.MLPResultType.SYSTEM_FAILURE), null, "ReturnError: " + String.valueOf(error_code) + " : "
            + event.getMAPErrorMessage());
  }

  public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("\nRx : onRejectComponent " + event);
    } else {
      this.logger.severe("\nRx : onRejectComponent " + event);
    }
    this.handleLocationResponse(MLPResponse.MLPResultType.SYSTEM_FAILURE, null, "RejectComponent: " + event);
  }


  /**
   * Handle HTTP POST request
   *
   * @param event
   * @param aci
   * @param eventContext
   */
  public void onPost(HttpServletRequestEvent event, ActivityContextInterface aci, EventContext eventContext) {
    onRequest(event, aci, eventContext);
  }

  /**
   * Handle HTTP GET request
   *
   * @param event
   * @param aci
   * @param eventContext
   */
  public void onGet(HttpServletRequestEvent event, ActivityContextInterface aci, EventContext eventContext) {
    onRequest(event, aci, eventContext);
  }

  /**
   * Entry point for all location lookups
   * Assigns a protocol handler to the request based on the path
   */
  private void onRequest(HttpServletRequestEvent event, ActivityContextInterface aci, EventContext eventContext) {

    setEventContext(eventContext);
    HttpServletRequest httpServletRequest = event.getRequest();
    HttpRequestType httpRequestType = HttpRequestType.fromPath(httpServletRequest.getPathInfo());
    setHttpRequest(new HttpRequest(httpRequestType));
    String requestingMLP, requestingMSISDN, serviceid;

    switch (httpRequestType) {
      case REST: {
        requestingMSISDN = httpServletRequest.getParameter("msisdn");
        serviceid = httpServletRequest.getParameter("serviceid");
      }
      break;
      case MLP:
        try {
          // Get the XML request from the POST data
          InputStream body = httpServletRequest.getInputStream();
          // Parse the request and retrieve the requested MSISDN and serviceid
          MLPRequest mlpRequest = new MLPRequest(logger);
          requestingMLP = mlpRequest.parseRequest(body);
          String[] output = requestingMLP.split(";");
          requestingMSISDN = output[0].toString();
          serviceid = output[1].toString();
        } catch (MLPException e) {
          handleLocationResponse(e.getMlpClientErrorType(), null, "System Failure: " + e.getMlpClientErrorMessage());
          return;
        } catch (IOException e) {
          e.printStackTrace();
          handleLocationResponse(MLPResponse.MLPResultType.FORMAT_ERROR, null, "System Failure: Failed to read from server input stream");
          return;
        }
        break;
      default:
        sendHTTPResult(HttpServletResponse.SC_NOT_FOUND, "Request URI unsupported");
        return;
    }

    setHttpRequest(new HttpRequest(httpRequestType, requestingMSISDN, serviceid));

    if (logger.isFineEnabled()) {
      logger.fine(String.format("Handling %s request, MSISDN: %s from %s", httpRequestType.name().toUpperCase(), requestingMSISDN, serviceid));
    }

    if (requestingMSISDN != null) {
      eventContext.suspendDelivery();
      setEventContextCMP(eventContext);
      getSingleMSISDNLocation(requestingMSISDN);
    } else {
      logger.info("MSISDN is null, sending back -1 for Global Cell Identity");
      handleLocationResponse(MLPResponse.MLPResultType.FORMAT_ERROR, null, "Invalid MSISDN specified");
    }
  }

  /**
   * CMP
   */
  public abstract void setEventContext(EventContext eventContext);

  public abstract EventContext getEventContext();

  public abstract void setEventContextCMP(EventContext eventContext);

  public abstract EventContext getEventContextCMP();

  public abstract void setHttpRequest(HttpRequest httpRequest);

  public abstract HttpRequest getHttpRequest();

  public abstract void setTimerID(TimerID value);

  public abstract TimerID getTimerID();

  public abstract void setAnyTimeInterrogationRequestInvokeId(long anyTimeInterrogationRequestInvokeId);

  public abstract long getAnyTimeInterrogationRequestInvokeId();

  public abstract void setGMLCCDRState(GMLCCDRState gmlcSdrState);

  public abstract GMLCCDRState getGMLCCDRState();

  /**
   * Private helper methods
   */

  /**
   * Retrieve the location for the specified MSISDN via ATI request to the HLR
   */
  private void getSingleMSISDNLocation(String requestingMSISDN) {

    if (!requestingMSISDN.equals(fakeNumber)) {
      try {
        AddressString addressString, addressString1;
        addressString = addressString1 = null;
        MAPDialogMobility mapDialogMobility = this.mapProvider.getMAPServiceMobility().createNewDialog(
            this.getMAPApplicationContext(), this.getGmlcSccpAddress(), addressString,
            getHlrSCCPAddress(requestingMSISDN), addressString1);

        ISDNAddressString isdnAdd = new ISDNAddressStringImpl(AddressNature.international_number,
            org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, requestingMSISDN);
        SubscriberIdentity subsId = new SubscriberIdentityImpl(isdnAdd);
        boolean locationInformation = true;
        boolean subscriberState = true;
        MAPExtensionContainer mapExtensionContainer = null;
        boolean currentLocation = false;
        DomainType requestedDomain = null;
        boolean imei = false;
        boolean msClassmark = false;
        boolean mnpRequestedInfo = false;
        RequestedInfo requestedInfo = new RequestedInfoImpl(locationInformation, subscriberState, mapExtensionContainer, currentLocation,
            requestedDomain, imei, msClassmark, mnpRequestedInfo);
        // requestedInfo (MAP ATI):
        // locationInformation: true (response includes mcc, mnc, lac, cellid, aol, vlrNumber)
        // subscriberState: true (response can be assumedIdle, camelBusy or notProvidedByVlr)
        // Rest of params are null or not requested
        ISDNAddressString gscmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
            org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
            gmlcPropertiesManagement.getGmlcGt());

        mapDialogMobility.addAnyTimeInterrogationRequest(subsId, requestedInfo, gscmSCFAddress, mapExtensionContainer);

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

    } else {
      // Handle fake location type given fake number set as MSISDN
      if (this.fakeLocationType == MLPResponse.MLPResultType.OK) {
        ATIResponse response = new ATIResponse();
        response.cell = fakeCellId;
        response.x = fakeLocationX;
        response.y = fakeLocationY;
        response.radius = fakeLocationRadius;
        String mlpClientErrorMessage = null;
        this.handleLocationResponse(MLPResponse.MLPResultType.OK, response, mlpClientErrorMessage);

      } else {
        ATIResponse response;
        response = null;
        this.handleLocationResponse(this.fakeLocationType, response, this.fakeLocationAdditionalInfoErrorString);
      }
    }
  }

  protected SccpAddress getGmlcSccpAddress() {

    if (this.gmlcSCCPAddress == null) {
      int translationType = 0; // Translation Type = 0 : Unknown
      EncodingScheme encodingScheme = null;
      GlobalTitle gt = sccpParameterFact.createGlobalTitle(gmlcPropertiesManagement.getGmlcGt(), translationType,
          NumberingPlan.ISDN_TELEPHONY, encodingScheme, NatureOfAddress.INTERNATIONAL);
      this.gmlcSCCPAddress = sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
          gt, translationType, gmlcPropertiesManagement.getGmlcSsn());

//			GlobalTitle0100 gt = new GlobalTitle0100Impl(gmlcPropertiesManagement.getGmlcGt(),0,BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY,NatureOfAddress.INTERNATIONAL);
//			this.serviceCenterSCCPAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, gmlcPropertiesManagement.getGmlcSsn());
    }
    return this.gmlcSCCPAddress;
  }

  private MAPApplicationContext getMAPApplicationContext() {

    if (this.anyTimeEnquiryContext == null) {
      this.anyTimeEnquiryContext = MAPApplicationContext.getInstance(
          MAPApplicationContextName.anyTimeEnquiryContext, MAPApplicationContextVersion.version3);
    }
    return this.anyTimeEnquiryContext;
  }

  private SccpAddress getHlrSCCPAddress(String address) {

    int translationType = 0; // Translation Type = 0 : Unknown
    EncodingScheme encodingScheme = null;
    GlobalTitle gt = sccpParameterFact.createGlobalTitle(address, translationType, NumberingPlan.ISDN_TELEPHONY, encodingScheme,
        NatureOfAddress.INTERNATIONAL);
    return sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, translationType,
        gmlcPropertiesManagement.getHlrSsn());

//	    GlobalTitle0100 gt = new GlobalTitle0100Impl(address, 0, BCDEvenEncodingScheme.INSTANCE,NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL);
//		return new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, gmlcPropertiesManagement.getHlrSsn());
  }

  /**
   * Handle generating the appropriate HTTP response
   * We're making use of the MLPResponse class for both GET/POST requests for convenience and
   * because eventually the GET method will likely be removed
   *
   * @param mlpResultType         OK or error type to return to client
   * @param atiResponse           ATIResponse on location attempt
   * @param mlpClientErrorMessage Error message to send to client
   */
  private void handleLocationResponse(MLPResponse.MLPResultType mlpResultType, ATIResponse atiResponse, String mlpClientErrorMessage) {
    HttpRequest request = getHttpRequest();
    EventContext httpEventContext = this.resumeHttpEventContext();

    switch (request.type) {
      case REST:
        if (mlpResultType == MLPResponse.MLPResultType.OK && httpEventContext != null) {

          HttpServletRequestEvent httpRequest = (HttpServletRequestEvent) httpEventContext.getEvent();
          HttpServletResponse httpServletResponse = httpRequest.getResponse();
          httpServletResponse.setStatus(HttpServletResponse.SC_OK);

          StringBuilder getResponse = new StringBuilder();
          getResponse.append("mcc=");
          getResponse.append(atiResponse.mcc);
          getResponse.append(",mnc=");
          getResponse.append(atiResponse.mnc);
          getResponse.append(",lac=");
          getResponse.append(atiResponse.lac);
          getResponse.append(",cellid=");
          getResponse.append(atiResponse.cell);
          getResponse.append(",aol=");
          getResponse.append(atiResponse.aol);
          getResponse.append(",vlrNumber=");
          getResponse.append(atiResponse.vlr);
          getResponse.append(",subscriberState=");
          getResponse.append(atiResponse.subscriberState);

          this.sendHTTPResult(httpServletResponse.SC_OK, getResponse.toString());

        } else {
          this.sendHTTPResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, mlpClientErrorMessage);
        }
        break;

      case MLP:
        String svcResultXml;
        MLPResponse mlpResponse = new MLPResponse(this.logger);

        if (mlpResultType == MLPResponse.MLPResultType.OK) {
          svcResultXml = mlpResponse.getSinglePositionSuccessXML(atiResponse.x, atiResponse.y, atiResponse.radius, request.msisdn);
        } else if (MLPResponse.isSystemError(mlpResultType)) {
          svcResultXml = mlpResponse.getSystemErrorResponseXML(mlpResultType, mlpClientErrorMessage);
        } else {
          svcResultXml = mlpResponse.getPositionErrorResponseXML(request.msisdn, mlpResultType, mlpClientErrorMessage);
        }

        this.sendHTTPResult(HttpServletResponse.SC_OK, svcResultXml);
        break;
    }
  }

  /**
   * Return the specified response data to the HTTP client
   *
   * @param responseData Response data to send to client
   */
  private void sendHTTPResult(int statusCode, String responseData) {
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
      response.setStatus(statusCode);
      PrintWriter w;
      w = response.getWriter();
      w.print(responseData);
      w.flush();
      response.flushBuffer();

      if (ctx.isSuspended()) {
        ctx.resumeDelivery();
      }

      if (logger.isFineEnabled()) {
        logger.fine("HTTP Request received and response sent, responseData=" + responseData);
      }

      // getNullActivity().endActivity();
    } catch (Exception e) {
      logger.severe("Error while sending back HTTP response", e);
    }
  }

  /**
   *
   */
  private EventContext resumeHttpEventContext() {
    EventContext httpEventContext = getEventContextCMP();

    if (httpEventContext == null) {
      logger.severe("No HTTP event context, can not resume ");
      return null;
    }

    httpEventContext.resumeDelivery();
    return httpEventContext;
  }

  // //////////////////
  // SBB LO methods //
  // ////////////////

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterfaceParent#
   * recordGenerationSucceeded(org.mobicents.gmlc.slee.cdr.CDRInterfaceParent.RecordType)
   */
  @Override
  public void recordGenerationSucceeded() {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("Generated CDR for Status: " + getCDRInterface().getState());
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterfaceParent#
   * recordGenerationFailed(java.lang.String)
   */
  @Override
  public void recordGenerationFailed(String message) {
    if (this.logger.isSevereEnabled()) {
      this.logger.severe("Failed to generate CDR! Message: '" + message + "'");
      this.logger.severe("Status: " + getCDRInterface().getState());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterfaceParent#
   * recordGenerationFailed(java.lang.String, java.lang.Throwable)
   */
  @Override
  public void recordGenerationFailed(String message, Throwable t) {
    if (this.logger.isSevereEnabled()) {
      this.logger.severe("Failed to generate CDR! Message: '" + message + "'", t);
      this.logger.severe("Status: " + getCDRInterface().getState());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.mobicents.gmlc.slee.cdr.CDRInterfaceParent#initFailed(java.lang.String, java.lang.Throwable)
   */
  @Override
  public void initFailed(String message, Throwable t) {
    if (this.logger.isSevereEnabled()) {
      this.logger.severe("Failed to initialize CDR Database! Message: '" + message + "'", t);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.mobicents.gmlc.slee.cdr.CDRInterfaceParent#initSuccessed()
   */
  @Override
  public void initSucceeded() {
    if (this.logger.isFineEnabled()) {
      this.logger.fine("CDR Database has been initialized!");
    }

  }

  //////////////////////
  //  CDR interface  //
  ////////////////////

  private static final String CDR = "CDR";

  public abstract ChildRelationExt getCDRPlainInterfaceChildRelation();

  public CDRInterface getCDRInterface() {
    GmlcPropertiesManagement gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance();
    ChildRelationExt childExt;
    if (gmlcPropertiesManagement.getCdrLoggingTo() == GmlcPropertiesManagement.CdrLoggedType.Textfile) {
      childExt = getCDRPlainInterfaceChildRelation();
    } else {
      //childExt = getCDRInterfaceChildRelation();
      childExt = null; // temporary
    }

    CDRInterface child = (CDRInterface) childExt.get(CDR);
    if (child == null) {
      try {
        child = (CDRInterface) childExt.create(CDR);
      } catch (TransactionRequiredLocalException e) {
        logger.severe("TransactionRequiredLocalException when creating CDR child", e);
      } catch (IllegalArgumentException e) {
        logger.severe("IllegalArgumentException when creating CDR child", e);
      } catch (NullPointerException e) {
        logger.severe("NullPointerException when creating CDR child", e);
      } catch (SLEEException e) {
        logger.severe("SLEEException when creating CDR child", e);
      } catch (CreateException e) {
        logger.severe("CreateException when creating CDR child", e);
      }
    }

    return child;
  }

  protected void createCDRRecord(RecordStatus recordStatus) {
    try {
      this.getCDRInterface().createRecord(recordStatus);
    } catch (Exception e) {
      logger.severe("Error while trying to create CDR Record", e);
    }
  }

  // ///////////////////////////////////////////////
  // protected child stuff, to be used in parent //
  // /////////////////////////////////////////////

  protected void cancelTimer() {
    try {
      TimerID timerID = this.getTimerID();
      if (timerID != null) {
        this.timerFacility.cancelTimer(timerID);
      }
    } catch (Exception e) {
      logger.severe("Could not cancel Timer", e);
    }
  }

  private void setTimer(ActivityContextInterface ac) {
    TimerOptions options = new TimerOptions();
    long waitingTime = gmlcPropertiesManagement.getDialogTimeout();
    // Set the timer on ACI
    TimerID timerID = this.timerFacility.setTimer(ac, null, System.currentTimeMillis() + waitingTime, options);
    this.setTimerID(timerID);
  }


}
