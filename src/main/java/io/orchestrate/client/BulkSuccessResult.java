package io.orchestrate.client;

/**
 * The result of a successful bulk operation request.
 *
 * @param <TItemPath> Provides additional details. {@link ItemPath} represents a KV item and
 * {@link EventPath} represents an event.
 */
public class BulkSuccessResult<TItemPath extends ItemPath> extends BulkResult {
    private final TItemPath itemPath;

    public BulkSuccessResult(int operationIndex, TItemPath itemPath) {
        super(BulkResultStatus.SUCCESS, operationIndex);

        this.itemPath = itemPath;
    }

    /**
     * @return Provides additional details about the bulk operation if available.
     */
    public TItemPath getItemPath() {
        return itemPath;
    }
}
