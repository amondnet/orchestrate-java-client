package io.orchestrate.client;

/**
 * The result of a bulk operation.
 *
 * @see BulkSuccessResult
 * @see BulkFailureResult
 */
public class BulkResult {
    private BulkResultStatus status;
    private int operationIndex;

    public BulkResult(BulkResultStatus status, int operationIndex) {
        this.status = status;
        this.operationIndex = operationIndex;
    }

    /**
     * The status of the bulk operation.
     * @return {@link BulkResultStatus#SUCCESS} if the operation succeeded. Otherwise, {@link BulkResultStatus#FAILURE}.
     *
     * @see BulkSuccessResult
     * @see BulkFailureResult
     */
    public BulkResultStatus getStatus() {
        return status;
    }

    /**
     * @return The operation index of the bulk request.
     */
    public int getOperationIndex() {
        return operationIndex;
    }
}
