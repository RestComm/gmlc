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

package org.mobicents.gmlc;

/**
 * @author <a href="mailto:abhayani@gmail.com"> Amit Bhayani </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 */
public interface GmlcPropertiesManagementMBean {

  String getGmlcGt();

  void setGmlcGt(String serviceCenterGt);

  int getGmlcSsn();

  void setGmlcSsn(int serviceCenterSsn);

  int getHlrSsn();

  void setHlrSsn(int hlrSsn);

  int getMscSsn();

  void setMscSsn(int mscSsn);

  int getMaxMapVersion();

  void setMaxMapVersion(int maxMapVersion);

  long getDialogTimeout();

  void setDialogTimeout(long dialogTimeout);

  int getMaxActivityCount();

  void setMaxActivityCount(int maxActivityCount);

  String getServerOverloadedMessage() ;

  void setServerOverloadedMessage(String serverOverloadedMessage);

  String getServerErrorMessage();

  void setServerErrorMessage(String serverErrorMessage);

  String getDialogTimeoutErrorMessage();

  void setDialogTimeoutErrorMessage(String dialogTimeoutErrorMessage);

}
