package org.mobicents.gmlc.slee.mlp;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import javax.slee.facilities.Tracer;
import java.io.InputStream;

/**
 * Created by angrygreenfrogs on 3/24/2015.
 * Helper class to handle an incoming MLP XML request
 */
public class MLPRequest {

    /**
     * Logger from the calling SBB
     */
    private Tracer logger;

    /**
     * Default constructor
     * @param logger Logger from the calling SBB
     */
    public MLPRequest(Tracer logger) {
        this.logger = logger;
    }

    /**
     * Parse incoming XML request data via JiBX's unmarshaller and return only the MSISDN being requested
     * @param requestStream InputStream (likely directly from the HTTP POST) of the XML input data
     * @return MSISDN of device to locate
     * @throws MLPException
     */
    public String parseRequest(InputStream requestStream) throws MLPException {
        // Result XML
        String requestingMSISDN = null;

        // Process the request
        try {
            // Create the JiBX unmarshalling object
            IBindingFactory jc = BindingDirectory.getFactory(org.oma.protocols.mlp.svc_init.SvcInit.class);
            IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();

            // Unmarshal directly from the POST input stream
            org.oma.protocols.mlp.svc_init.SvcInit svcInit = (org.oma.protocols.mlp.svc_init.SvcInit)unmarshaller.unmarshalDocument(requestStream, "UTF-8");

            // Process the location request for the specified MSISDN
            org.oma.protocols.mlp.svc_init.Msids msids = svcInit.getSlir().getMsids();
            org.oma.protocols.mlp.svc_init.Msids.Choice c = msids.getChoiceList().get(0);
            org.oma.protocols.mlp.svc_init.Msid msisdn = c.getMsid();
            requestingMSISDN = msisdn.getString();
            this.logger.info("Parsed location request for MSISDN: " + requestingMSISDN);
            return requestingMSISDN;
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
