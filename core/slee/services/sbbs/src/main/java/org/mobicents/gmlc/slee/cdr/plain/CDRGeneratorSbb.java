/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.gmlc.slee.cdr.plain;

import java.sql.Timestamp;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.SbbContext;
import javax.slee.serviceactivity.ServiceStartedEvent;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.gmlc.slee.cdr.GMLCCDRState;
import org.mobicents.gmlc.slee.cdr.RecordStatus;
import org.mobicents.protocols.ss7.indicator.AddressIndicator;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import org.mobicents.gmlc.slee.map.MobileCoreNetworkInterfaceSbb;
import org.mobicents.gmlc.slee.cdr.CDRInterface;

/**
 *
 * @author <a href="mailto:bbaranow@redhat.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 */
public abstract class CDRGeneratorSbb extends MobileCoreNetworkInterfaceSbb implements CDRInterface {

  private static final Logger cdrTracer = Logger.getLogger(CDRGeneratorSbb.class);

  private static final String CDR_GENERATED_TO = "Textfile";


  public CDRGeneratorSbb() {
    //super("CDRGeneratorSbb");
    super();
    // TODO Auto-generated constructor stub
  }

  // -------------------- SLEE Stuff -----------------------
  // --------------- CDRInterface methods ------------------
  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterface#init(boolean)
   */
  @Override
  public void init(final boolean reset) {
    super.logger.info("Setting CDR_GENERATED_TO to "+CDR_GENERATED_TO);
  }

  /* (non-Javadoc)
   * @see org.mobicents.gmlc.slee.cdr.CDRInterface#createRecord(org.mobicents.gmlc.slee.cdr.Status)
   */
  @Override
  public void createRecord(RecordStatus outcome) {
    GMLCCDRState state = getState();

    if (state.isGenerated()) {
      super.logger.severe("");
    } else {
      if (super.logger.isFineEnabled()) {
        super.logger.fine("Generating record, status '" + outcome + "' for '" + state + "'");
      }
      DateTime startTime = state.getDialogStartTime();
      if (startTime != null) {
        DateTime endTime = DateTime.now();
        Long duration = endTime.getMillis() - startTime.getMillis();
        state.setDialogEndTime(endTime);
        state.setDialogDuration(duration);
      }
      state.setRecordStatus(outcome);
      state.setGenerated(true);
      this.setState(state);
      String data = this.toString(state);
      if (this.logger.isFineEnabled()) {
        this.logger.fine(data);
      } else {
        this.cdrTracer.debug(data);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterface#setState(org.mobicents.gmlc.slee.cdr.GMLCCDRState)
   */
  @Override
  public void setState(GMLCCDRState state) {
    this.setGMLCCDRState(state);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.mobicents.gmlc.slee.cdr.CDRInterface#getState()
   */
  @Override
  public GMLCCDRState getState() {
    return this.getGMLCCDRState();
  }

  // CMPs
  public abstract GMLCCDRState getGMLCCDRState();

  public abstract void setGMLCCDRState(GMLCCDRState state);

  public void onStartServiceEvent(ServiceStartedEvent event, ActivityContextInterface aci) {
    this.init(true);
  }

  // --------------- SBB callbacks ---------------

  /*
   * (non-Javadoc)
   *
   * @see javax.slee.Sbb#sbbCreate()
   */
  @Override
  public void sbbCreate() throws CreateException {
    this.setGMLCCDRState(new GMLCCDRState());
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
   */
  @Override
  public void setSbbContext(SbbContext ctx) {
    super.setSbbContext(ctx);
    super.logger = super.sbbContext.getTracer(TRACER_NAME);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.slee.Sbb#unsetSbbContext()
   */
  @Override
  public void unsetSbbContext() {
    super.unsetSbbContext();
  }

  // -------- helper methods
  private static final String SEPARATOR = ":";
  /**
   * @param gmlcCdrState
   * @return
   */
  protected String toString(GMLCCDRState gmlcCdrState) {

    final StringBuilder stringBuilder = new StringBuilder(); //StringBuilder is faster than StringBuffer

    final Timestamp time_stamp = new Timestamp(System.currentTimeMillis());

    // TIMESTAMP
    stringBuilder.append(time_stamp).append(SEPARATOR);

    // ID
    stringBuilder.append(gmlcCdrState.getId()).append(SEPARATOR);

    // RECORD STATUS
    stringBuilder.append(gmlcCdrState.getRecordStatus().toString()).append(SEPARATOR);

    // LOCAL DIALOG_ID
    stringBuilder.append(gmlcCdrState.getLocalDialogId()).append(SEPARATOR);

    // REMOTE DIALOG_ID
    stringBuilder.append(gmlcCdrState.getRemoteDialogId()).append(SEPARATOR);

    Long dialogDuration = gmlcCdrState.getDialogDuration();
    if (dialogDuration != null) {
      // TODO: output as millis or?
      // DIALOG_DURATION
      stringBuilder.append(dialogDuration).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    /*
     * LOCAL Address
     */
    SccpAddress localAddress = gmlcCdrState.getLocalAddress();
    if (localAddress != null) {
      AddressIndicator addressIndicator = localAddress.getAddressIndicator();

      // Local SPC
      if (addressIndicator.isPCPresent()) {
        stringBuilder.append(localAddress.getSignalingPointCode()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }

      // Local SSN
      if (addressIndicator.isSSNPresent()) {
        stringBuilder.append((byte) localAddress.getSubsystemNumber()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }
      // Local Routing Indicator
      if (addressIndicator.getRoutingIndicator() != null) {
        stringBuilder.append((byte) addressIndicator.getRoutingIndicator().getValue()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }

      /*
       * Local GLOBAL TITLE
       */
      GlobalTitle localAddressGlobalTitle = localAddress.getGlobalTitle();

      // Local GLOBAL TITLE INDICATOR
      if (localAddressGlobalTitle != null && localAddressGlobalTitle.getGlobalTitleIndicator() != null) {
        stringBuilder.append((byte) localAddressGlobalTitle.getGlobalTitleIndicator().getValue()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }
      // Local GLOBAL TITLE DIGITS
      if (localAddressGlobalTitle != null && localAddressGlobalTitle.getDigits() != null) {
        stringBuilder.append(localAddressGlobalTitle.getDigits()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }
    }

    /*
     * REMOTE Address
     */
    SccpAddress remoteAddress = gmlcCdrState.getRemoteAddress();
    if (remoteAddress != null) {
      AddressIndicator addressIndicator = remoteAddress.getAddressIndicator();

      // Remote SPC
      if (addressIndicator.isPCPresent()) {
        stringBuilder.append(remoteAddress.getSignalingPointCode()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }

      // Remote SSN
      if (addressIndicator.isSSNPresent()) {
        stringBuilder.append((byte) remoteAddress.getSubsystemNumber()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }

      // Remote Routing Indicator
      if (addressIndicator.getRoutingIndicator() != null) {
        stringBuilder.append((byte) addressIndicator.getRoutingIndicator().getValue()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }

      /*
       * Remote GLOBAL TITLE
       */
      GlobalTitle remoteAddressGlobalTitle = remoteAddress.getGlobalTitle();

      if (remoteAddressGlobalTitle != null && remoteAddressGlobalTitle.getGlobalTitleIndicator() != null) {
        // Remote GLOBAL TITLE INDICATOR
        stringBuilder.append((byte) remoteAddressGlobalTitle.getGlobalTitleIndicator().getValue()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }
      // Remote GLOBAL TITLE DIGITS
      if (remoteAddressGlobalTitle != null && remoteAddressGlobalTitle.getDigits() != null) {
        stringBuilder.append(remoteAddressGlobalTitle.getDigits()).append(SEPARATOR);
      } else {
        stringBuilder.append(SEPARATOR);
      }
    }

    /*
     * ORIGINATING REFERENCE Address
     */
    AddressString addressString = gmlcCdrState.getOrigReference();
    if(addressString != null){
      // Originating Reference ADDRESS NATURE
      stringBuilder.append((byte) addressString.getAddressNature().getIndicator()).append(SEPARATOR);
      // Originating Reference NUMBERING PLAN INDICATOR
      stringBuilder.append((byte) addressString.getNumberingPlan().getIndicator()).append(SEPARATOR);
      // Originating Reference ADDRESS DIGITS
      stringBuilder.append(addressString.getAddress()).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
    }

    /*
     * DESTINATION REFERENCE Address
     */
    addressString = gmlcCdrState.getDestReference();
    if(addressString != null){
      // Destination Reference ADDRESS NATURE
      stringBuilder.append((byte) addressString.getAddressNature().getIndicator()).append(SEPARATOR);
      // Destination Reference NUMBERING PLAN INDICATOR
      stringBuilder.append((byte) addressString.getNumberingPlan().getIndicator()).append(SEPARATOR);
      // Destination Reference ADDRESS DIGITS
      stringBuilder.append(addressString.getAddress()).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
    }

    /*
     * ISDN Address
     */
    ISDNAddressString isdnAddressString= gmlcCdrState.getISDNAddressString();
    if(isdnAddressString != null){
      // ISDN ADDRESS NATURE
      stringBuilder.append((byte) isdnAddressString.getAddressNature().getIndicator()).append(SEPARATOR);
      // ISDN NUMBERING PLAN INDICATOR
      stringBuilder.append((byte) isdnAddressString.getNumberingPlan().getIndicator()).append(SEPARATOR);
      // ISDN ADDRESS DIGITS
      stringBuilder.append(isdnAddressString.getAddress()).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
      stringBuilder.append(SEPARATOR);
    }

    IMSI imsi = gmlcCdrState.getImsi();
    if(imsi != null){
      // IMSI
      stringBuilder.append(imsi.getData()).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    /*
     * Cell Global Identity
     */
    int ci = gmlcCdrState.getCi();
    if(ci != -1){
      // CELL ID (CI)
      stringBuilder.append(ci).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    int lac = gmlcCdrState.getLac();
    if(lac != -1){
      // LOCATION AREA CODE (LAC)
      stringBuilder.append(lac).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    int mcc = gmlcCdrState.getMcc();
    if(mcc != -1){
      // MOBILE COUNTRY CODE (MCC)
      stringBuilder.append(mcc).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    int mnc = gmlcCdrState.getMnc();
    if(mnc != -1){
      // MOBILE NETWORK CODE (MNC)
      stringBuilder.append(mnc).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    int aol = gmlcCdrState.getAol();
    if(aol != -1){
      // AGE OF LOCATION
      stringBuilder.append(aol).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    String atiVlrGt = gmlcCdrState.getAtiVlrGt();
    if(atiVlrGt != null){
      // MAP ATI VLR GLOBAL TITLE
      stringBuilder.append(atiVlrGt).append(SEPARATOR);
    } else {
      stringBuilder.append(SEPARATOR);
    }

    String subscriberState = gmlcCdrState.getSubscriberState();
    if(subscriberState != null){
      // MAP ATI SUBSCRIBER STATE
      stringBuilder.append(subscriberState)/*.append(SEPARATOR)*/; /// Uncomment if further fields are added
    } else {
      /*stringBuilder.append(SEPARATOR)*/; /// Uncomment if further fields are added
    }

    return stringBuilder.toString();
  }


}
