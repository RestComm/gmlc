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

package org.mobicents.gmlc.slee;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.http.HttpServletRaActivityContextInterfaceFactory;
import net.java.slee.resource.http.HttpServletRaSbbInterface;

import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.SbbContextExt;


/**
 *
 * @author <a href="mailto:abhayani@gmail.com"> Amit Bhayani </a>
 * @author <a href="mailto:serg.vetyutnev@gmail.com"> Sergey Vetyutnev </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 * @author <a href="mailto:bbaranow@redhat.com"> Bartosz Baranowski </a>
 */
public class GMLCBaseSbb implements Sbb {

  private final String loggerName;

  // -------------------------------------------------------------
  // SLEE stuff
  // -------------------------------------------------------------
  protected SbbContextExt sbbContext;

  protected Tracer logger;

  // -------------------------------------------------------------
  // MAP RA stuff
  // -------------------------------------------------------------
  protected static final ResourceAdaptorTypeID mapRATypeID = new ResourceAdaptorTypeID("MAPResourceAdaptorType",
      "org.mobicents", "2.0");
  protected static final String mapRaLink = "MAPRA";
  protected MAPContextInterfaceFactory mapAcif;
  protected MAPProvider mapProvider;
  protected MAPParameterFactory mapParameterFactory;

  // -------------------------------------------------------------
  // HTTP Server RA stuff
  // -------------------------------------------------------------
  protected static ResourceAdaptorTypeID httpServerRATypeID;
  protected static final String httpServerRaLink = "HttpServletRA";

  protected HttpServletRaSbbInterface httpServletProvider;
  protected HttpServletRaActivityContextInterfaceFactory httpServletRaActivityContextInterfaceFactory;

  public GMLCBaseSbb(String loggerName) {
    super();
    this.loggerName = loggerName;
  }

  // -------------------------------------------------------------
  // SLEE minimal stuff
  // -------------------------------------------------------------
  public void setSbbContext(SbbContext sbbContext) {
    this.sbbContext = (SbbContextExt) sbbContext;
    this.logger = sbbContext.getTracer(this.loggerName);
  }

  public void unsetSbbContext() {
    // clean RAs
    this.mapAcif = null;
    this.mapProvider = null;
    this.mapParameterFactory = null;
    //this.gmlcStatAggregator = null;

    //this.jdbcRA = null;
    //this.jdbcACIF = null;

    this.httpServletProvider = null;
    this.httpServletRaActivityContextInterfaceFactory = null;

    // clean SLEE
    this.sbbContext = null;
    this.logger = null;
  }

  public void sbbCreate() throws CreateException {
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

  /*protected boolean checkMaxActivityCount(int maxActivityCount) {
    if (maxActivityCount <= 0)
      return true;
    return mapProvider.getCurrentDialogsCount() < maxActivityCount;
  }*/

  // -------------------------------------------------------------
  // JDBC RA stuff
  // -------------------------------------------------------------
  //*** Will be implemented on a later stage ***//
  //protected static final ResourceAdaptorTypeID JDBC_RESOURCE_ADAPTOR_ID = JdbcResourceAdaptorSbbInterface.RATYPE_ID;
  //protected static final String JDBC_RA_LINK = "JDBCRA";
  //protected JdbcResourceAdaptorSbbInterface jdbcRA;
  //protected JdbcActivityContextInterfaceFactory jdbcACIF;

  // -------------------------------------------------------------
  // Statistics stuff
  // -------------------------------------------------------------
  //*** Will be implemented on a later stage ***//
  //protected GmlcStatAggregator gmlcStatAggregator;

}
