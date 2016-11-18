package io.orchestrate.client;

import java.util.Collections;
import java.util.Map;

/**
 * The error for a failed bulk result.
 */
public class BulkError {
    private String message;
    private String code;
    private Map<String, String> details;

    /**
     * @return The error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The error code of an error.
     */
    public String getCode() {
        return code;
    }

    /**
     * The details of the error, if any.
     * @return A key/value pair of detailed error properties.
     */
    public Map<String, String> getDetails() {
        return Collections.unmodifiableMap(details);
    }
}
