package org.mobicents.gmlc.slee.mlp;

/**
 * Created by angrygreenfrogs on 3/24/2015.
 */
public class MLPException extends Exception {
    /**
     * Constructor with exception message
     * @param msg Exception message
     */
    public MLPException(String msg) {
        super(msg);
    }

    /**
     * Custom error message string to return to the client
     */
    private String mlpClientErrorMessage = "";

    /**
     * Custom error code to return to the client
     */
    private MLPResponse.MLPResultType mlpClientErrorType = null;

    /** Getters and setters */

    public MLPResponse.MLPResultType getMlpClientErrorType() {
        return mlpClientErrorType;
    }

    public void setMlpClientErrorType(MLPResponse.MLPResultType mlpClientErrorType) {
        this.mlpClientErrorType = mlpClientErrorType;
    }

    public String getMlpClientErrorMessage() {
        return mlpClientErrorMessage;
    }

    public void setMlpClientErrorMessage(String mlpClientErrorMessage) {
        this.mlpClientErrorMessage = mlpClientErrorMessage;
    }
}
