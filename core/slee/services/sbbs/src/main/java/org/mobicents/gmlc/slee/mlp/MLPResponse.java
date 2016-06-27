package org.mobicents.gmlc.slee.mlp;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IXMLWriter;
import org.oma.protocols.mlp.svc_result.Pos;

import javax.slee.facilities.Tracer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by angrygreenfrogs on 3/24/2015.
 * This is a helper for generating MLP XML responses to send to a MLP client.
 * It uses the JiBX generated XML bound classes in org.oma.protocols.mlp
 * It exists to generates consistent XML output using the JiBX marshaller
 * It supports generating a result only for a single MSISDN that has a single Point (X/Y - lat/lon) result
 * In the future, it will likely be replaced by a more complex structure for handling the full variety of MLP
 * results, but I wanted to keep it simple and clear for the first version.
 */
public class MLPResponse {
    public enum MLPResultType {
        OK,
        SYSTEM_FAILURE,
        FORMAT_ERROR,
        UNKNOWN_SUBSCRIBER,
        ABSENT_SUBSCRIBER,
        QOP_NOT_ATTAINABLE,
        POSITION_METHOD_FAILURE,
    }

    /**
     * Logger from the calling SBB
     */
    private Tracer logger;

    public MLPResponse(Tracer logger) {
        this.logger = logger;
    }

    // If there's an internal exception or other error, we have to fallback to some "worst case scenario"
    // static XML return data
    private String genericErrorXML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "  <!DOCTYPE svc_result SYSTEM \"MLP_SVC_RESULT_310.dtd\">" +
            "  <svc_result xmlns=\"MLP_SVC_RESULT_310.dtd\" ver=\"3.1.0\">\n" +
            "  <slia ver=\"3.1.0\">\n" +
            "    <result resid=\"1\">SYSTEM FAILURE</result>\n" +
            "    <add_info>Internal IO or parsing error occurred</add_info>\n" +
            "  </slia>\n" +
            "</svc_result>";

    /**
     * Get the MLP result code as per OMA MLP section 5.4.1
     * @param t MLPResultType internal enum value for use in the MLP classes
     * @return MLP result code to return to the client
     */
    public static String getResultCodeForType(MLPResultType t) {
        switch(t) {
            case OK:
                return "0";
            case SYSTEM_FAILURE:
                return "1";
            case UNKNOWN_SUBSCRIBER:
                return "4";
            case ABSENT_SUBSCRIBER:
                return "5";
            case FORMAT_ERROR:
                return "105";
            case QOP_NOT_ATTAINABLE:
                return "201";
            case POSITION_METHOD_FAILURE:
                return "6";
        }

        return "1";
    }

    /**
     * Get the MLP result string as per OMA MLP section 5.4.1
     * @param t MLPResultType internal enum value for use in the MLP classes
     * @return MLP result string to return to the client
     */
    public static String getResultStringForType(MLPResultType t) {
        switch(t) {
            case OK:
                return "OK";
            case SYSTEM_FAILURE:
                return "SYSTEM FAILURE";
            case UNKNOWN_SUBSCRIBER:
                return "UNKNOWN SUBSCRIBER";
            case ABSENT_SUBSCRIBER:
                return "ABSENT SUBSCRIBER";
            case FORMAT_ERROR:
                return "FORMAT ERROR";
            case QOP_NOT_ATTAINABLE:
                return "QOP NOT ATTAINABLE";
            case POSITION_METHOD_FAILURE:
                return "POSITION METHOD FAILURE";
        }

        return "1";
    }

    /**
     * Is this error type a system error?
     * @param t MLPResultType internal enum value for use in the MLP classes
     * @return boolean true if it is a system error
     */
    public static boolean isSystemError(MLPResultType t) {
        switch(t) {
            case SYSTEM_FAILURE:
            case UNKNOWN_SUBSCRIBER:
            case ABSENT_SUBSCRIBER:
            case FORMAT_ERROR:
                return true;
        }

        return false;
    }

    /**
     * Use JiBX marshalling to generate the MLP XML result data for a single successful position look-up
     * @param x X coordinate in WGS84 DMS format
     * @param y Y coordinate in WGS84 DMS format
     * @param radius Position radius in meters (e.g. 5000 for 5km of accuracy)
     * @param msid Location for MSISDN
     * @return String XML result to return to client
     * Example usage:
     *   svcResultXml = MLPResponse.getSinglePositionSuccessXML("27 28 25.00S", "153 01 43.00E", "+1000", "20140507082957", "61307341370");
     * Example result based on above usage:
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE svc_result SYSTEM "MLP_SVC_RESULT_310.DTD">
        <svc_result xmlns="MLP_SVC_RESULT_310.dtd" ver="3.1.0">
        <slia ver="3.1.0">
        <pos>
        <msid>61307341370</msid>
        <pd>
        <time utc_off="+1000">20140507082957</time>
        <shape>
        <Point>
        <coord>
        <X>27 28 25.00S</X>
        <Y>153 01 43.00E</Y>
        </coord>
        </Point>
        </shape>
        </pd>
        </pos>
        </slia>
        </svc_result>
     */
    public String getSinglePositionSuccessXML(String x, String y, String radius, String msid) {
        // Generate XML response
        String svcResultXml = "";

        try {
            // Eventually this timestamp should be replaced by the actual network position time
            Date requestTime = new Date();
            String date = new SimpleDateFormat("yyyyMMddHHmmss").format(requestTime);
            String utcOffset = new SimpleDateFormat("Z").format(requestTime);

            // Generate the response XML
            svcResultXml = this.generateSinglePositionSuccessXML(x, y, radius, utcOffset, date, msid);
        }
        catch (IllegalArgumentException e) {
            // Generate the error XML
            svcResultXml = this.getSystemErrorResponseXML(MLPResponse.MLPResultType.SYSTEM_FAILURE, "Failed to create request timestamp");
        }
        catch (IOException e) {
            // Generate the error XML
            svcResultXml = this.getSystemErrorResponseXML(MLPResponse.MLPResultType.SYSTEM_FAILURE, "IO failure generating XML");
        }
        catch(org.jibx.runtime.JiBXException e) {
            // Generate the error XML
            svcResultXml = this.getSystemErrorResponseXML(MLPResponse.MLPResultType.SYSTEM_FAILURE, "Failed to generate XML response from internal objects");
        }

        return svcResultXml;
    }

    /**
     * Internal XML generation support function for above getSinglePositionSuccessXML()
     * @param x X coordinate in WGS84 DMS format
     * @param y Y coordinate in WGS84 DMS format
     * @param radius Position radius in meters (e.g. 5000 for 5km of accuracy)
     * @param utcOffSet Utc offset for location timestamp in "[+/-]HHmm" format
     * @param time Location timestamp at above UTC offset in "yyyyMMddHHmmss" format
     * @param msid Location for MSISDN
     * @return String XML result to return to client
     * @throws org.jibx.runtime.JiBXException JiBX had an internal failure of some kind while marshalling the XML
     * @throws IOException IO error occurred while generating the XML result
     */
    private String generateSinglePositionSuccessXML(String x, String y, String radius, String utcOffSet, String time, String msid)
            throws org.jibx.runtime.JiBXException, IOException {
        String lXml = null;
        String ver = "3.1.0";

        // Create all the objects we'll use to generate our svc_result XML
        org.oma.protocols.mlp.svc_result.SvcResult mlpSvcResult = new org.oma.protocols.mlp.svc_result.SvcResult();
        org.oma.protocols.mlp.svc_result.Slia mlpSlia = new org.oma.protocols.mlp.svc_result.Slia();
        org.oma.protocols.mlp.svc_result.Pos mlpPos = new org.oma.protocols.mlp.svc_result.Pos();
        org.oma.protocols.mlp.svc_result.Msid mlpMsid = new org.oma.protocols.mlp.svc_result.Msid();
        org.oma.protocols.mlp.svc_result.Pd mlpPd = new org.oma.protocols.mlp.svc_result.Pd();
        org.oma.protocols.mlp.svc_result.Time mlpTime = new org.oma.protocols.mlp.svc_result.Time();
        org.oma.protocols.mlp.svc_result.Shape mlpShape = new org.oma.protocols.mlp.svc_result.Shape();
        org.oma.protocols.mlp.svc_result.CircularArea mlpCircularArea = new org.oma.protocols.mlp.svc_result.CircularArea();
        org.oma.protocols.mlp.svc_result.Coord mlpCoord = new org.oma.protocols.mlp.svc_result.Coord();
        org.oma.protocols.mlp.svc_result.X mlpX = new org.oma.protocols.mlp.svc_result.X();
        org.oma.protocols.mlp.svc_result.Y mlpY = new org.oma.protocols.mlp.svc_result.Y();
        org.oma.protocols.mlp.svc_result.Radius mlpRadius = new org.oma.protocols.mlp.svc_result.Radius();
        List<Pos> posList = new ArrayList();

        // Set the key location data
        mlpX.setX(x);
        mlpY.setY(y);
        mlpTime.setUtcOff(utcOffSet);
        mlpMsid.setString(msid);
        mlpRadius.setRadius(radius);

        // Setup all the objects for this single position
        mlpCoord.setX(mlpX);
        mlpCoord.setY(mlpY);
        mlpCircularArea.setCoord(mlpCoord);
        mlpCircularArea.setRadius(mlpRadius);
        mlpShape.setCircularArea(mlpCircularArea);
        mlpPd.setShape(mlpShape);
        mlpTime.setString(time);
        mlpPd.setTime(mlpTime);
        mlpPos.setMsid(mlpMsid);
        mlpPos.setPd(mlpPd);

        // Add it to the position list and result
        posList.add(mlpPos);
        mlpSlia.setPoList(posList);
        mlpSlia.setVer(ver);
        mlpSvcResult.setSlia(mlpSlia);
        mlpSvcResult.setVer(ver);

        lXml = marshalMlpResult(mlpSvcResult);

        // Return our XML string result
        return lXml;
    }

    /**
     * Generate a MLP system error response
     * @param mlpClientErrorType Error type to return to client
     * @param mlpClientErrorMessage Error message to send to client
     * @return String XML result to return to client
     */
    public String getSystemErrorResponseXML(MLPResultType mlpClientErrorType, String mlpClientErrorMessage) {
        // Generate XML response
        String svcResultXml = "";

        try {
            // Generate the error XML
            svcResultXml = this.generateSystemErrorXML(mlpClientErrorType, mlpClientErrorMessage);
            return svcResultXml;
        }
        catch (IOException e) {
            // Return generic XML error response because we couldn't generate the correct response
            e.printStackTrace();
            this.logger.info("Exception while creating XML response data: " + e.getMessage());
            return genericErrorXML;
        }
        catch(org.jibx.runtime.JiBXException e) {
            // Return generic XML error response because we couldn't generate the correct response
            e.printStackTrace();
            this.logger.info("Exception while marshalling XML response data: " + e.getMessage());
            return genericErrorXML;
        }
    }

    /**
     * Internal XML generation support function for above getSystemErrorResponseXML()
     * @param mlpClientErrorType Error type to return to client
     * @param mlpClientErrorMessage Error message to send to client
     * @return String XML result to return to client
     * @throws org.jibx.runtime.JiBXException JiBX had an internal failure of some kind while marshalling the XML
     * @throws IOException IO error occurred while generating the XML result
     */
    private String generateSystemErrorXML(MLPResultType mlpClientErrorType, String mlpClientErrorMessage)
            throws org.jibx.runtime.JiBXException, IOException {
        String lXml = null;
        String ver = "3.1.0";

        // Create all the objects we'll use to generate our svc_result XML
        org.oma.protocols.mlp.svc_result.SvcResult mlpSvcResult = new org.oma.protocols.mlp.svc_result.SvcResult();
        org.oma.protocols.mlp.svc_result.Slia mlpSlia = new org.oma.protocols.mlp.svc_result.Slia();
        org.oma.protocols.mlp.svc_result.Result mlpResult = new org.oma.protocols.mlp.svc_result.Result();
        org.oma.protocols.mlp.svc_result.AddInfo mlpAddInfo = new org.oma.protocols.mlp.svc_result.AddInfo();

        // Set the additional data error message if one is available
        if (mlpClientErrorMessage != null)
        {
            mlpAddInfo.setAddInfo(mlpClientErrorMessage);
            mlpSlia.setAddInfo(mlpAddInfo);
        }

        mlpResult.setString(MLPResponse.getResultStringForType(mlpClientErrorType));
        mlpResult.setResid(MLPResponse.getResultCodeForType(mlpClientErrorType));
        mlpSlia.setResult(mlpResult);
        mlpSlia.setVer(ver);
        mlpSvcResult.setSlia(mlpSlia);
        mlpSvcResult.setVer(ver);

        IBindingFactory jc = BindingDirectory.getFactory(org.oma.protocols.mlp.svc_result.SvcResult.class);
        IMarshallingContext marshaller = jc.createMarshallingContext();
        ByteArrayOutputStream lOutputStream = new ByteArrayOutputStream();
        marshaller.setOutput(lOutputStream, "UTF-8");
        IXMLWriter ix = marshaller.getXmlWriter();

        // Add XML and DOCTYPE headers
        ix.writeXMLDecl("1.0", "UTF-8", null);
        ix.writeDocType("svc_result", "MLP_SVC_RESULT_310.DTD", null, null);

        // Set 4 spaces as the default indenting
        marshaller.setIndent(4);

        // Generate the XML
        marshaller.marshalDocument(mlpSvcResult);

        // Convert the stream to a string
        lXml = new String(lOutputStream.toByteArray(), "UTF-8");

        // Return our XML string result
        return lXml;
    }

    /**
     * Generate a MLP error response for a position
     * @param msid Device MSISDN
     * @param mlpClientErrorType Error type to return to client
     * @param mlpClientErrorMessage Error message to send to client
     * @return String XML result to return to client
     */
    public String getPositionErrorResponseXML(String msid, MLPResultType mlpClientErrorType, String mlpClientErrorMessage) {
        // Generate XML response
        String svcResultXml = "";

        try {
            // Eventually this timestamp should be replaced by the actual network position time
            Date requestTime = new Date();
            String date = new SimpleDateFormat("yyyyMMddHHmmss").format(requestTime);
            String utcOffset = new SimpleDateFormat("Z").format(requestTime);

            // Generate the error XML
            this.logger.info("Creating error XML response for type: "+MLPResponse.getResultCodeForType(mlpClientErrorType)+" message: "+mlpClientErrorMessage);
            svcResultXml = this.generatePositionErrorXML(utcOffset, date, msid, mlpClientErrorType, mlpClientErrorMessage);
            return svcResultXml;
        }
        catch (IllegalArgumentException e) {
            // Return generic XML error response because we couldn't generate the correct response
            e.printStackTrace();
            this.logger.info("Exception while creating timestamp: " + e.getMessage());
            return genericErrorXML;
        }
        catch (IOException e) {
            // Return generic XML error response because we couldn't generate the correct response
            e.printStackTrace();
            this.logger.info("Exception while creating XML response data: " + e.getMessage());
            return genericErrorXML;
        }
        catch(org.jibx.runtime.JiBXException e) {
            // Return generic XML error response because we couldn't generate the correct response
            e.printStackTrace();
            this.logger.info("Exception while marshalling XML response data: " + e.getMessage());
            return genericErrorXML;
        }
    }

    /**
     * Internal XML generation support function for above getSystemErrorResponseXML()
     * @param utcOffSet Utc offset for location timestamp in "[+/-]HHmm" format
     * @param time Location timestamp at above UTC offset in "yyyyMMddHHmmss" format
     * @param msid Device MSISDN
     * @param mlpClientErrorType Error type to return to client
     * @param mlpClientErrorMessage Error message to send to client
     * @return String XML result to return to client
     * @throws org.jibx.runtime.JiBXException JiBX had an internal failure of some kind while marshalling the XML
     * @throws IOException IO error occurred while generating the XML result
     */
    private String generatePositionErrorXML(String utcOffSet, String time, String msid, MLPResultType mlpClientErrorType, String mlpClientErrorMessage)
            throws org.jibx.runtime.JiBXException, IOException {
        String lXml = null;
        String ver = "3.1.0";

        // Create all the objects we'll use to generate our svc_result XML
        org.oma.protocols.mlp.svc_result.SvcResult mlpSvcResult = new org.oma.protocols.mlp.svc_result.SvcResult();
        org.oma.protocols.mlp.svc_result.Slia mlpSlia = new org.oma.protocols.mlp.svc_result.Slia();
        org.oma.protocols.mlp.svc_result.Pos mlpPos = new org.oma.protocols.mlp.svc_result.Pos();
        org.oma.protocols.mlp.svc_result.Msid mlpMsid = new org.oma.protocols.mlp.svc_result.Msid();
        org.oma.protocols.mlp.svc_result.Result mlpResult = new org.oma.protocols.mlp.svc_result.Result();
        org.oma.protocols.mlp.svc_result.AddInfo mlpAddInfo = new org.oma.protocols.mlp.svc_result.AddInfo();
        org.oma.protocols.mlp.svc_result.Poserr mlpPosErr = new org.oma.protocols.mlp.svc_result.Poserr();
        org.oma.protocols.mlp.svc_result.Time mlpTime = new org.oma.protocols.mlp.svc_result.Time();
        List<Pos> posList = new ArrayList();

        // Set the data
        mlpTime.setUtcOff(utcOffSet);
        mlpTime.setString(time);
        mlpMsid.setString(msid);

        // Set the additional data error message if one is available
        if (mlpClientErrorMessage != null)
        {
            mlpAddInfo.setAddInfo(mlpClientErrorMessage);
            mlpPosErr.setAddInfo(mlpAddInfo);
        }

        mlpResult.setString(MLPResponse.getResultStringForType(mlpClientErrorType));
        mlpResult.setResid(MLPResponse.getResultCodeForType(mlpClientErrorType));
        mlpPosErr.setResult(mlpResult);
        mlpPosErr.setTime(mlpTime);
        mlpPos.setMsid(mlpMsid);
        mlpPos.setPoserr(mlpPosErr);
        posList.add(mlpPos);
        mlpSlia.setPoList(posList);
        mlpSlia.setVer(ver);
        mlpSvcResult.setSlia(mlpSlia);
        mlpSvcResult.setVer(ver);

        lXml = marshalMlpResult(mlpSvcResult);

        // Return our XML string result
        return lXml;
    }

    /**
     * Create the svc_result XML result for any type of result (error or success)
     * @param mlpSvcResult Fully filled in SvcResult object to marshal (convert to XML)
     * @return String of XML result to send to client
     * @throws org.jibx.runtime.JiBXException JiBX had an internal failure of some kind while marshalling the XML
     * @throws IOException IO error occurred while generating the XML result
     */
    private String marshalMlpResult(org.oma.protocols.mlp.svc_result.SvcResult mlpSvcResult)
            throws org.jibx.runtime.JiBXException, IOException {
        String lXml = null;

        IBindingFactory jc = BindingDirectory.getFactory(org.oma.protocols.mlp.svc_result.SvcResult.class);
        IMarshallingContext marshaller = jc.createMarshallingContext();
        ByteArrayOutputStream lOutputStream = new ByteArrayOutputStream();
        marshaller.setOutput(lOutputStream, "UTF-8");
        IXMLWriter ix = marshaller.getXmlWriter();

        // Add XML and DOCTYPE headers
        ix.writeXMLDecl("1.0", "UTF-8", null);
        ix.writeDocType("svc_result", "MLP_SVC_RESULT_310.DTD", null, null);

        // Set 4 spaces as the default indenting
        marshaller.setIndent(4);

        // Generate the XML
        marshaller.marshalDocument(mlpSvcResult);

        // Convert the stream to a string
        lXml = new String(lOutputStream.toByteArray(), "UTF-8");

        // Return our XML string result
        return lXml;
    }
}
