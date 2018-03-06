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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
 *
 * @author <a href="mailto:abhayani@gmail.com"> Amit Bhayani </a>
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 */
public class GmlcPropertiesManagement implements GmlcPropertiesManagementMBean {

  private static final Logger logger = Logger.getLogger(GmlcPropertiesManagement.class);

  protected static final String GMLC_GT = "gmlcgt";
  protected static final String GMLC_SSN = "gmlcssn";
  protected static final String HLR_SSN = "hlrssn";
  protected static final String MSC_SSN = "mscssn";
  protected static final String MAX_MAP_VERSION = "maxmapv";

  private static final String TAB_INDENT = "\t";
  private static final String CLASS_ATTRIBUTE = "type";
  private static final XMLBinding binding = new XMLBinding();
  protected static final String CDR_LOGGING_TO = "cdrloggingto";
  protected static final String MAX_ACTIVITY_COUNT = "maxactivitycount";
  private static final String PERSIST_FILE_NAME = "gmlcproperties.xml";

  protected static final String SERVER_OVERLOADED_MESSAGE = "serveroverloadedmsg";
  protected static final String SERVER_ERROR_MESSAGE = "servererrmssg";
  protected static final String DIALOG_TIMEOUT_ERROR_MESSAGE = "dialogtimeouterrmssg";
  private String serverOverloadedMessage = "Server is overloaded. Please try later";
  private String serverErrorMessage = "Server error, please try again after sometime";
  private String dialogTimeoutErrorMessage = "Request timeout please try again after sometime.";

  private static GmlcPropertiesManagement instance;

  private final String name;

  private String persistDir = null;

  private long dialogTimeout = 30000;

  private final TextBuilder persistFile = TextBuilder.newInstance();

  private String gmlcGt = "000000";
  private int gmlcSsn = 145;
  private int hlrSsn = 6;
  private int mscSsn = 8;
  private int maxMapVersion = 3;

  private CdrLoggedType cdrLoggingTo = CdrLoggedType.Textfile;

  // max count of TCAP Dialogs that are possible at the same time
  private int maxActivityCount = 1000;

  private GmlcPropertiesManagement(String name) {
    this.name = name;
    binding.setClassAttribute(CLASS_ATTRIBUTE);
  }

  protected static GmlcPropertiesManagement getInstance(String name) {
    if (instance == null) {
      instance = new GmlcPropertiesManagement(name);
    }
    return instance;
  }

  public static GmlcPropertiesManagement getInstance() {
    return instance;
  }

  public String getName() {
    return name;
  }

  public String getPersistDir() {
    return persistDir;
  }

  public void setPersistDir(String persistDir) {
    this.persistDir = persistDir;
  }

  @Override
  public String getGmlcGt() {
    return gmlcGt;
  }

  @Override
  public void setGmlcGt(String gmlcGt) {
    this.gmlcGt = gmlcGt;
    this.store();
  }

  @Override
  public int getGmlcSsn() {
    return gmlcSsn;
  }

  @Override
  public void setGmlcSsn(int gmlcSsn) {
    this.gmlcSsn = gmlcSsn;
    this.store();
  }

  @Override
  public int getHlrSsn() {
    return hlrSsn;
  }

  @Override
  public void setHlrSsn(int hlrSsn) {
    this.hlrSsn = hlrSsn;
    this.store();
  }

  @Override
  public int getMscSsn() {
    return mscSsn;
  }

  @Override
  public void setMscSsn(int mscSsn) {
    this.mscSsn = mscSsn;
    this.store();
  }

  @Override
  public int getMaxMapVersion() {
    return maxMapVersion;
  }

  @Override
  public void setMaxMapVersion(int maxMapVersion) {
    this.maxMapVersion = maxMapVersion;
    this.store();
  }

  @Override
  public long getDialogTimeout() {
    return dialogTimeout;
  }

  @Override
  public void setDialogTimeout(long dialogTimeout) {
    this.dialogTimeout = dialogTimeout;
    this.store();
  }

  public CdrLoggedType getCdrLoggingTo() {
    return cdrLoggingTo;
  }

  public void setCdrLoggingTo(CdrLoggedType cdrLoggingTo) {
    this.cdrLoggingTo = cdrLoggingTo;
    this.store();
  }

  public int getMaxActivityCount() {
    return maxActivityCount;
  }

  public void setMaxActivityCount(int maxActivityCount) {
    this.maxActivityCount = maxActivityCount;
    this.store();
  }

  public String getServerOverloadedMessage() {
    return this.serverOverloadedMessage;
  }

  public void setServerOverloadedMessage(String serverOverloadedMessage) {
    this.serverOverloadedMessage = serverOverloadedMessage;
    this.store();
  }

  public String getServerErrorMessage() {
    return this.serverErrorMessage;
  }

  public void setServerErrorMessage(String serverErrorMessage) {
    this.serverErrorMessage = serverErrorMessage;
    this.store();
  }

  public String getDialogTimeoutErrorMessage() {
    return this.dialogTimeoutErrorMessage;
  }

  public void setDialogTimeoutErrorMessage(String dialogTimeoutErrorMessage) {
    this.dialogTimeoutErrorMessage = dialogTimeoutErrorMessage;
    this.store();
  }

  public void start() throws Exception {

    this.persistFile.clear();

    if (persistDir != null) {
      this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
          .append(PERSIST_FILE_NAME);
    } else {
      persistFile
          .append(System.getProperty(GmlcManagement.GMLC_PERSIST_DIR_KEY,
              System.getProperty(GmlcManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
          .append("_").append(PERSIST_FILE_NAME);
    }

    logger.info(String.format("Loading GMLC Properties from %s", persistFile.toString()));

    try {
      this.load();
    } catch (FileNotFoundException e) {
      logger.warn(String.format("Failed to load the GMLC configuration file. \n%s", e.getMessage()));
    }

  }

  public void stop() throws Exception {
  }

  /**
   * Persist
   */
  public void store() {

    // TODO : Should we keep reference to Objects rather than recreating
    // everytime?
    try {
      XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
      writer.setBinding(binding);
      // Enables cross-references.
      // writer.setReferenceResolver(new XMLReferenceResolver());
      writer.setIndentation(TAB_INDENT);

      writer.write(this.gmlcGt, GMLC_GT, String.class);
      writer.write(this.gmlcSsn, GMLC_SSN, Integer.class);
      writer.write(this.hlrSsn, HLR_SSN, Integer.class);
      writer.write(this.mscSsn, MSC_SSN, Integer.class);
      writer.write(this.maxMapVersion, MAX_MAP_VERSION, Integer.class);
      writer.write(this.cdrLoggingTo.toString(), CDR_LOGGING_TO, String.class);
      writer.write(this.maxActivityCount, MAX_ACTIVITY_COUNT, Integer.class);
      writer.close();
    } catch (Exception e) {
      logger.error("Error while persisting the Rule state in file", e);
    }
  }

  /**
   * Load and create LinkSets and Link from persisted file
   *
   * @throws Exception
   */
  public void load() throws FileNotFoundException {

    XMLObjectReader reader = null;
    try {
      reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

      reader.setBinding(binding);
      this.gmlcGt = reader.read(GMLC_GT, String.class);
      this.gmlcSsn = reader.read(GMLC_SSN, Integer.class);
      this.hlrSsn = reader.read(HLR_SSN, Integer.class);
      this.mscSsn = reader.read(MSC_SSN, Integer.class);
      this.maxMapVersion = reader.read(MAX_MAP_VERSION, Integer.class);

      reader.close();
    } catch (XMLStreamException ex) {
      // this.logger.info(
      // "Error while re-creating Linksets from persisted file", ex);
    }
  }

  public enum CdrLoggedType {
    Database, Textfile,
  }

}
