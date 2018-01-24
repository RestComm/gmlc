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

package org.mobicents.gmlc.slee.mlp;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.oma.protocols.mlp.svc_init.Serviceid;

import javax.slee.facilities.Tracer;

import java.io.InputStream;

/**
 * Helper class to handle an incoming MLP XML request
 *
 * @author <a href="mailto:eross@locatrix.com"> Andrew Eross </a>
 */
public class MLPRequest {

  /**
   * Logger from the calling SBB
   */
  private Tracer logger;

  /**
   * Default constructor
   *
   * @param logger Logger from the calling SBB
   */
  public MLPRequest(Tracer logger) {
    this.logger = logger;
  }

  /**
   * Parse incoming XML request data via JiBX's unmarshaller and return only the MSISDN being requested
   *
   * @param requestStream InputStream (likely directly from the HTTP POST) of the XML input data
   * @return MSISDN of device to locate
   * @throws MLPException
   */
  public String parseRequest(InputStream requestStream) throws MLPException {
    // Result XML
    String requestingserviceid, requestingMSISDN = null;

    // Process the request
    try {
      // Create the JiBX unmarshalling object
      IBindingFactory jc = BindingDirectory.getFactory(org.oma.protocols.mlp.svc_init.SvcInit.class);
      IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();

      // Unmarshal directly from the POST input stream
      org.oma.protocols.mlp.svc_init.SvcInit svcInit = (org.oma.protocols.mlp.svc_init.SvcInit) unmarshaller.unmarshalDocument(requestStream, "UTF-8");

      // Process the location request for the specified MSISDN
      org.oma.protocols.mlp.svc_init.Msids msids = svcInit.getSlir().getMsids();
      org.oma.protocols.mlp.svc_init.Msids.Choice c = msids.getChoiceList().get(0);
      org.oma.protocols.mlp.svc_init.Msid msisdn = c.getMsid();
      requestingMSISDN = msisdn.getString();
      //Process the location request for serviceid
      org.oma.protocols.mlp.svc_init.Serviceid serviceidd = svcInit.getHdr().getClient().getServiceid();
      requestingserviceid = serviceidd.getServiceid();
      this.logger.info("Parsed location request for MSISDN: " + requestingMSISDN);
      return requestingMSISDN + ";" + requestingserviceid;
    } catch (JiBXException e) {
      e.printStackTrace();
      this.logger.info("Exception while unmarshalling XML request data: " + e.getMessage());

      // Set a custom error message for delivering directly to the client
      // and throw a new exception
      MLPException mlpException = new MLPException(e.getMessage());
      mlpException.setMlpClientErrorMessage("Invalid XML received: " + e.getMessage());
      mlpException.setMlpClientErrorType(MLPResponse.MLPResultType.FORMAT_ERROR);
      throw mlpException;
    }
  }
}
