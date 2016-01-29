package io.orchestrate.client;

/**
 * The result of a failed bulk operation request.
 */
public class BulkFailureResult extends BulkResult {
    private BulkError error;

    /**
     * @param operationIndex The operation index of the bulk request.
     * @param error The error related to the failure.
     */
    public BulkFailureResult(int operationIndex, BulkError error) {
        super(BulkResultStatus.FAILURE, operationIndex);
        this.error = error;
    }

    /**
     * @return The error with details related to the failure.
     */
    public BulkError getError() { return error; }
}
