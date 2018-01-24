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

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.mobicents.ss7.management.console.ShellExecutor;

/**
 *
 * @author <a href="mailto:abhayani@gmail.com"> Amit Bhayani </a>
 *
 */
public class GmlcShellExecutor implements ShellExecutor {

  private static final Logger logger = Logger.getLogger(GmlcShellExecutor.class);

  private GmlcManagement gmlcManagement;
  private GmlcPropertiesManagement gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance();

  /**
   *
   */
  public GmlcShellExecutor() {
    // TODO Auto-generated constructor stub
  }

  public GmlcManagement getGmlcManagement() {
    return gmlcManagement;
  }

  public void setGmlcManagement(GmlcManagement gmlcManagement) {
    this.gmlcManagement = gmlcManagement;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.mobicents.ss7.management.console.ShellExecutor#execute(java.lang.String[])
   */
  @Override
  public String execute(String[] commands) {

    try {
      if (commands.length < 2) {
        return GmlcOAMMessages.INVALID_COMMAND;
      }
      String command = commands[1];

      if (command.equals("set")) {
        return this.manageSet(commands);
      } else if (command.equals("get")) {
        return this.manageGet(commands);
      }
      return GmlcOAMMessages.INVALID_COMMAND;
    } catch (Exception e) {
      logger.error(String.format("Error while executing comand %s", Arrays.toString(commands)), e);
      return e.getMessage();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.mobicents.ss7.management.console.ShellExecutor#handles(java.lang.String)
   */
  @Override
  public boolean handles(String command) {
    return "gmlc".equals(command);
  }

  private String manageSet(String[] options) throws Exception {
    if (options.length < 4) {
      return GmlcOAMMessages.INVALID_COMMAND;
    }

    String parName = options[2].toLowerCase();
    if (parName.equals(GmlcPropertiesManagement.GMLC_GT)) {
      gmlcPropertiesManagement.setGmlcGt(options[3]);
    } else if (parName.equals(GmlcPropertiesManagement.GMLC_SSN)) {
      int val = Integer.parseInt(options[3]);
      gmlcPropertiesManagement.setGmlcSsn(val);
    } else if (parName.equals(GmlcPropertiesManagement.HLR_SSN)) {
      int val = Integer.parseInt(options[3]);
      gmlcPropertiesManagement.setHlrSsn(val);
    } else if (parName.equals(GmlcPropertiesManagement.MSC_SSN)) {
      int val = Integer.parseInt(options[3]);
      gmlcPropertiesManagement.setMscSsn(val);
    } else if (parName.equals(GmlcPropertiesManagement.MAX_MAP_VERSION)) {
      int val = Integer.parseInt(options[3]);
      gmlcPropertiesManagement.setMaxMapVersion(val);
    } else {
      return GmlcOAMMessages.INVALID_COMMAND;
    }

    return GmlcOAMMessages.PARAMETER_SUCCESSFULLY_SET;
  }

  private String manageGet(String[] options) throws Exception {
    if (options.length == 3) {
      String parName = options[2].toLowerCase();

      StringBuilder sb = new StringBuilder();
      sb.append(options[2]);
      sb.append(" = ");
      if (parName.equals(GmlcPropertiesManagement.GMLC_GT)) {
        sb.append(gmlcPropertiesManagement.getGmlcGt());
      } else if (parName.equals(GmlcPropertiesManagement.GMLC_SSN)) {
        sb.append(gmlcPropertiesManagement.getGmlcSsn());
      } else if (parName.equals(GmlcPropertiesManagement.HLR_SSN)) {
        sb.append(gmlcPropertiesManagement.getHlrSsn());
      } else if (parName.equals(GmlcPropertiesManagement.MSC_SSN)) {
        sb.append(gmlcPropertiesManagement.getMscSsn());
      } else if (parName.equals(GmlcPropertiesManagement.MAX_MAP_VERSION)) {
        sb.append(gmlcPropertiesManagement.getMaxMapVersion());
      } else {
        return GmlcOAMMessages.INVALID_COMMAND;
      }

      return sb.toString();
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(GmlcPropertiesManagement.GMLC_GT + " = ");
      sb.append(gmlcPropertiesManagement.getGmlcGt());
      sb.append("\n");

      sb.append(GmlcPropertiesManagement.GMLC_SSN + " = ");
      sb.append(gmlcPropertiesManagement.getGmlcSsn());
      sb.append("\n");

      sb.append(GmlcPropertiesManagement.HLR_SSN + " = ");
      sb.append(gmlcPropertiesManagement.getHlrSsn());
      sb.append("\n");

      sb.append(GmlcPropertiesManagement.MSC_SSN + " = ");
      sb.append(gmlcPropertiesManagement.getMscSsn());
      sb.append("\n");

      sb.append(GmlcPropertiesManagement.MAX_MAP_VERSION + " = ");
      sb.append(gmlcPropertiesManagement.getMaxMapVersion());
      sb.append("\n");

      return sb.toString();
    }
  }
}
