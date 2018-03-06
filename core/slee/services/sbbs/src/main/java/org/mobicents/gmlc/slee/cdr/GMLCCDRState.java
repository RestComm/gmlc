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

package org.mobicents.gmlc.slee.cdr;

import java.io.Serializable;
import java.util.UUID;

import org.joda.time.DateTime;
import org.mobicents.gmlc.slee.cdr.RecordStatus;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * Represents state associated with ongoing dialog required for proper CDR
 * generation. Data which should be used for CDR is spread across many objects.
 * So we need object which can be used to store them in one convenient place.
 *
 * @author <a href="mailto:bbaranow@redhat.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 */
public class GMLCCDRState implements Serializable {

  public static final String GMLC_STRING_SEPARATOR = ",";

  // TODO: AddressString and IMSI hashCode + equals
  protected boolean initiated;
  protected boolean generated;

  protected String id;

  protected RecordStatus recordStatus;

  // Dialog params
  protected Long localDialogId;
  protected Long remoteDialogId;
  //NB: once we fully update to JDK8, we should revert to using standard java.time package
  protected DateTime dialogStartTime;
  protected DateTime dialogEndTime;
  protected Long dialogDuration;

  // Circuit Switched Core Network / SS7 params
  protected AddressString origReference;
  protected AddressString destReference;

  protected IMSI imsi;
  protected AddressString vlrAddress;

  protected ISDNAddressString isdnAddressString;
  protected SccpAddress localAddress;
  protected SccpAddress remoteAddress;

  //MAP ATI response CGI and subscriber state parameters
  protected int ci;
  protected int lac;
  protected int mcc;
  protected int mnc;
  protected int aol;
  protected String atiVlrGt;
  protected String subscriberState;

  /*** GETTERS ***/
  /**************/

  /**
   * @return the GMLC string separator
   */
  public static String getGmlcStringSeparator() {
    return GMLC_STRING_SEPARATOR;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the initiated
   */
  public boolean isInitiated() {
    return initiated;
  }

  /**
   * @return the initiated
   */
  public boolean isInitialized() {
    return this.initiated;
  }

  /**
   * @return the generated
   */
  public boolean isGenerated() {
    return generated;
  }

  /**
   * @return the origReference
   */
  public AddressString getOrigReference() {
    return origReference;
  }

  /**
   * @return the destReference
   */
  public AddressString getDestReference() {
    return destReference;
  }

  /**
   * @return the imsi
   */
  public IMSI getImsi() {
    return imsi;
  }

  /**
   * @return the vlrAddress
   */
  public AddressString getVlrAddress() {
    return vlrAddress;
  }

  /**
   * @return the ISDNAddressString
   */
  public ISDNAddressString getISDNAddressString() {
    return isdnAddressString;
  }

  /**
   * @return the ISDNAddressString
   */
  public ISDNAddressString getIsdnAddressString() {
    return isdnAddressString;
  }

  /**
   * @return the localAddress (GMLC)
   */
  public SccpAddress getLocalAddress() {
    return localAddress;
  }

  /**
   * @return the remoteAddress (Core Network Entity responding address)
   */
  public SccpAddress getRemoteAddress() {
    return remoteAddress;
  }

  /**
   * @return the localDialogId
   */
  public Long getLocalDialogId() {
    return localDialogId;
  }

  /**
   * @return the remoteDialogId
   */
  public Long getRemoteDialogId() {
    return this.remoteDialogId;
  }

  /**
   * @return the dialogStartTime
   */
  public DateTime getDialogStartTime() {
    return this.dialogStartTime;
  }

  /**
   * @return dialogEndTime
   */
  public DateTime getDialogEndTime() {
    return this.dialogEndTime;
  }

  /**
   * @return dialogDuration to set
   */
  public Long getDialogDuration() {
    return this.dialogDuration;
  }

  /**
   * @return the recordStatus
   */
  public RecordStatus getRecordStatus() {
    return recordStatus;
  }

  /**
   * @return the CGI Cell Id
   */
  public int getCi() {
    return ci;
  }

  /**
   * @return the CGI Location Area Code
   */
  public int getLac() {
    return lac;
  }

  /**
   * @return the CGI Mobile Country Code
   */
  public int getMcc() {
    return mcc;
  }

  /**
   * @return the CGI Mobile Network Code
   */
  public int getMnc() {
    return mnc;
  }

  /**
   * @return the CGI Age of Location
   */
  public int getAol() {
    return aol;
  }

  /**
   * @return the VLR Global Title since last MAP Update Location
   */
  public String getAtiVlrGt() {
    return atiVlrGt;
  }

  /**
   * @return the target subscriber state from MAP ATI
   */
  public String getSubscriberState() {
    return subscriberState;
  }

  /*** SETTERS ***/
  /**************/

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @param initiated the initiated to set
   */
  public void setInitiated(boolean initiated) {
    this.initiated = initiated;
  }

  /**
   * @param generated to set
   */
  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  /**
   * @param origReference the origReference to set
   */
  public void setOrigReference(AddressString origReference) {
    this.origReference = origReference;
  }

  /**
   * @param destReference the destReference to set
   */
  public void setDestReference(AddressString destReference) {
    this.destReference = destReference;
  }

  /**
   * @param imsi the IMSI to set
   */
  public void setImsi(IMSI imsi) {
    this.imsi = imsi;
  }

  /**
   * @param vlrAddress the VLR Address to set
   */
  public void setVlrAddress(AddressString vlrAddress) {
    this.vlrAddress = vlrAddress;
  }

  /**
   * @param iSDNString the ISDNString to set
   */
  public void setISDNAddressString(ISDNAddressString iSDNString) {
    isdnAddressString = iSDNString;
  }

  /**
   * @param isdnAddressString the ISDNString to set
   */
  public void setIsdnAddressString(ISDNAddressString isdnAddressString) {
    this.isdnAddressString = isdnAddressString;
  }

  /**
   * @param localAddress the localAddress to set
   */
  public void setLocalAddress(SccpAddress localAddress) {
    this.localAddress = localAddress;
  }

  /**
   * @param remoteAddress the remoteAddress to set
   */
  public void setRemoteAddress(SccpAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  /**
   * @param localDialogId the localDialogId to set
   */
  public void setLocalDialogId(Long localDialogId) {
    this.localDialogId = localDialogId;
  }

  /**
   * @param remoteDialogId to set
   */
  public void setRemoteDialogId(Long remoteDialogId) {
    this.remoteDialogId = remoteDialogId;
  }

  /**
   * @param dialogStartTime to set
   */
  public void setDialogStartTime(DateTime dialogStartTime) {
    this.dialogStartTime = dialogStartTime;
  }

  /**
   * @param dialogEndTime to set
   */
  public void setDialogEndTime(DateTime dialogEndTime) {
    this.dialogEndTime = dialogEndTime;
  }

  /**
   * @param dialogDuration to set
   */
  public void setDialogDuration(Long dialogDuration) {
    this.dialogDuration = dialogDuration;
  }

  /**
   * @param recordStatus the recordStatus to set
   */
  public void setRecordStatus(RecordStatus recordStatus) {
    this.recordStatus = recordStatus;
  }

  /**
   * @param ci to set
   */
  public void setCi(int ci) {
    this.ci = ci;
  }

  /**
   * @param lac to set
   */
  public void setLac(int lac) {
    this.lac = lac;
  }

  /**
   * @param mcc to set
   */
  public void setMcc(int mcc) {
    this.mcc = mcc;
  }

  /**
   * @param mnc to set
   */
  public void setMnc(int mnc) {
    this.mnc = mnc;
  }

  /**
   * @param aol to set
   */
  public void setAol(int aol) {
    this.aol = aol;
  }

  /**
   * @param atiVlrGt to set
   */
  public void setAtiVlrGt(String atiVlrGt) {
    this.atiVlrGt = atiVlrGt;
  }

  /**
   * @param subscriberState to set
   */
  public void setSubscriberState(String subscriberState) {
    this.subscriberState = subscriberState;
  }

  public void init(final Long dialogId, final AddressString destRef, final AddressString origRef, final ISDNAddressString isdnAddressString,
                   final SccpAddress localAddress, final SccpAddress remoteAddress) {
    this.localDialogId = dialogId;
    this.destReference = destRef;
    this.origReference = origRef;
    this.isdnAddressString = isdnAddressString;
    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
    // This should be enough to be unique
    this.id = UUID.randomUUID().toString();
    this.initiated = true;
    this.dialogStartTime = null;
    this.dialogEndTime = null;
    this.dialogDuration =null;
  }

  public void init(String id, boolean initiated, boolean generated, AddressString origReference, AddressString destReference,
                   IMSI imsi, AddressString vlrAddress, ISDNAddressString isdnAddressString,
                   SccpAddress localAddress, SccpAddress remoteAddress, Long localDialogId, Long remoteDialogId,
                   DateTime dialogStartTime, DateTime dialogEndTime, Long dialogDuration, RecordStatus recordStatus,
                   int ci, int lac, int mcc, int mnc, int aol, String atiVlrGt, String subscriberState) {
    // This should be enough to be unique
    this.id = UUID.randomUUID().toString();
    this.initiated = true;
    this.generated = generated;
    this.origReference = origReference;
    this.destReference = destReference;
    this.imsi = imsi;
    this.vlrAddress = vlrAddress;
    this.isdnAddressString = isdnAddressString;
    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
    this.localDialogId = localDialogId;
    this.remoteDialogId = remoteDialogId;
    this.dialogStartTime = null;
    this.dialogEndTime = null;
    this.dialogDuration = null;
    this.recordStatus = recordStatus;
    this.ci = ci;
    this.lac = lac;
    this.mcc = mcc;
    this.mnc = mnc;
    this.aol = aol;
    this.atiVlrGt = atiVlrGt;
    this.subscriberState = subscriberState;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((isdnAddressString == null) ? 0 : isdnAddressString.hashCode());
    result = prime * result + ((destReference == null) ? 0 : destReference.hashCode());
    result = prime * result + ((localDialogId == null) ? 0 : localDialogId.hashCode());
    result = prime * result + ((remoteDialogId == null) ? 0 : remoteDialogId.hashCode());
    result = prime * result + ((imsi == null) ? 0 : imsi.hashCode());
    result = prime * result + ((vlrAddress == null) ? 0 : vlrAddress.hashCode());
    result = prime * result + (generated ? 1231 : 1237);
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + (initiated ? 1231 : 1237);
    result = prime * result + ((localAddress == null) ? 0 : localAddress.hashCode());
    result = prime * result + ((origReference == null) ? 0 : origReference.hashCode());
    result = prime * result + ((recordStatus == null) ? 0 : recordStatus.hashCode());
    result = prime * result + ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {

    if (this == obj)
      return true;

    if (obj == null)
      return false;

    if (getClass() != obj.getClass())
      return false;
    GMLCCDRState other = (GMLCCDRState) obj;

    if (isdnAddressString == null) {
      if (other.isdnAddressString != null)
        return false;
    } else if (!isdnAddressString.equals(other.isdnAddressString))
      return false;

    if (destReference == null) {
      if (other.destReference != null)
        return false;
    } else if (!destReference.equals(other.destReference))
      return false;

    if (localDialogId == null) {
      if (other.localDialogId != null)
        return false;
    } else if (!localDialogId.equals(other.localDialogId))
      return false;

    if (remoteDialogId == null) {
      if (other.remoteDialogId != null)
        return false;
    } else if (!remoteDialogId.equals(other.remoteDialogId))
      return false;

    if (imsi == null) {
      if (other.imsi != null)
        return false;
    } else if (!imsi.equals(other.imsi))
      return false;

    if (vlrAddress == null) {
      if (other.vlrAddress != null)
        return false;
    } else if (!vlrAddress.equals(other.vlrAddress))
      return false;

    if (generated != other.generated)
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;

    if (initiated != other.initiated)
      return false;

    if (localAddress == null) {
      if (other.localAddress != null)
        return false;
    } else if (!localAddress.equals(other.localAddress))
      return false;

    if (origReference == null) {
      if (other.origReference != null)
        return false;
    } else if (!origReference.equals(other.origReference))
      return false;

    if (recordStatus != other.recordStatus)
      return false;

    if (remoteAddress == null) {
      if (other.remoteAddress != null)
        return false;
    } else if (!remoteAddress.equals(other.remoteAddress))
      return false;

    if (recordStatus == null) {
      if (other.recordStatus != null)
        return false;
    } else if (!recordStatus.equals(other.recordStatus))
      return false;

    if (ci == -1) {
      if (other.ci != -1)
        return false;
    } else if (ci != other.ci)
      return false;

    if (lac == -1) {
      if (other.lac != -1)
        return false;
    } else if (lac != other.lac)
      return false;

    if (mcc == -1) {
      if (other.mcc != -1)
        return false;
    } else if (mcc != other.mcc)
      return false;

    if (mnc == -1) {
      if (other.mnc != -1)
        return false;
    } else if (mnc != other.mnc)
      return false;

    if (aol == -1) {
      if (other.aol != -1)
        return false;
    } else if (aol != other.aol)
      return false;

    if (atiVlrGt == null) {
      if (other.atiVlrGt != null)
        return false;
    } else if (!atiVlrGt.equals(other.atiVlrGt))
      return false;

    if (subscriberState == null) {
      if (other.subscriberState != null)
        return false;
    } else if (!subscriberState.equals(other.subscriberState))
      return false;

    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "GMLCCDRState [initiated=" + initiated + ", generated=" + generated + ", origReference=" + origReference
        + ", destReference=" + destReference + ", imsi=" + imsi + ", vlrAddress=" + vlrAddress
        + ", ISDNString=" + isdnAddressString + ", localAddress=" + localAddress + ", remoteAddress="
        + remoteAddress + ", localDialogId=" + localDialogId  + ", remoteDialogId=" + remoteDialogId + ", id=" + id +
        ", recordStatus=" + recordStatus + ", cid=" + ci + ", lac=" + lac + ", mcc=" + mcc + ", mnc=" + mnc +
        ", aol=" + aol + ", vlrGT=" + atiVlrGt + ", subscriberState=" + subscriberState + "]@" + super.hashCode();
  }

}
