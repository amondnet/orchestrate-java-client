package io.orchestrate.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The response from a bulk request.
 */
public class BulkResponse {
    private final BulkStatus status;
    private final int successCount;
    protected final ArrayList<BulkResult> results;

    public BulkResponse(BulkStatus status, int successCount) {
        this.status = status;
        this.successCount = successCount;
        results = new ArrayList<BulkResult>();
    }

    /**
     * Returns a status indicating whether or not the bulk request was successful.
     * @return {@link BulkStatus#SUCCESS} if all operations succeed, {@link BulkStatus#PARTIAL} if some operations
     * succeed,and {@link BulkStatus#FAILURE} if all operations fail.
     */
    public BulkStatus getStatus() {
        return status;
    }

    /**
     * @return The number of bulk operations that were successful.
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * @return The results of each bulk operation.
     */
    public List<BulkResult> getResults() {
        return Collections.unmodifiableList(results);
    }
}
