package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The resource for the Bulk features in the Orchestrate API.
 */
public class BulkResource extends BaseResource {
    final List<BulkOperation> bulkOperations;
    private final OrchestrateClient client;
    private Boolean isDone = false;

    public BulkResource(OrchestrateClient client, JacksonMapper jacksonMapper) {
        super(client, jacksonMapper);
        this.client = client;
        bulkOperations = Collections.synchronizedList(new ArrayList<BulkOperation>());
    }

    /**
     * Adds an operation to the list of operations to send in bulk. This API is not typically called by a consumer.
     * It is recommended that you use the associated bulk methods provided by {@link Client#bulk()}.
     *
     * @param bulkOperation The bulk operation.
     * @return This bulk resource.
     *
     * @see Client#bulk()
     */
    public BulkResource add(BulkOperation bulkOperation) {
        if (isDone)
            throw new IllegalStateException("Can not add an operation after calling 'done'");

        bulkOperations.add(bulkOperation);
        return this;
    }

    /**
     * Indicates that you are done adding bulk operations and prepares a bulk request.
     *
     * @return The prepared bulk request.
     * @throws IOException
     */
    public OrchestrateRequest<BulkResponse> done() throws IOException {
        isDone = true;
        ByteArrayOutputStream requestStream = new ByteArrayOutputStream();
        for (BulkOperation bulkOperation : this.bulkOperations) {
            requestStream.write(toJsonBytes(bulkOperation));
        }

        final String uri = client.uri();

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.POST)
                .contentType("application/orchestrate-export-stream+json")
                .uri(uri);

        httpHeaderBuilder.contentLength(requestStream.size());

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(requestStream.toByteArray())))
                .build();

        return new OrchestrateRequest<BulkResponse>(client, packet, new ResponseConverter<BulkResponse>() {
            @Override
            public BulkResponse from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                // Note: Even failed bulk requests return as a 200 with failure information
                if (status == 200)
                    return createResponse(response);
                else
                    // TODO Add basic response details to non 200 responses
                    // Returning `null` is consistent with the rest of the client, but I think
                    // this is suboptimal.
                    return null;
            }
        });
    }

    private BulkResponse createResponse(HttpContent httpResponse) throws IOException {
        final JsonNode jsonNode = toJsonNode(httpResponse);
        BulkResponse bulkResponse = new BulkResponse(
                BulkStatus.fromJson(jsonNode.get("status").asText()),
                jsonNode.get("success_count").asInt());

        addBulkResultsToBulkResponse(jsonNode, bulkResponse);

        return bulkResponse;
    }

    private void addBulkResultsToBulkResponse(JsonNode jsonNode, BulkResponse bulkResponse) throws IOException {
        final Iterator<JsonNode> results = jsonNode.get("results").elements();
        while (results.hasNext()) {
            JsonNode itemNode = results.next();
            BulkResultStatus status = BulkResultStatus.fromJson(itemNode.get("status").asText());
            if (status == BulkResultStatus.SUCCESS) {
                bulkResponse.results.add(createBulkSuccessResult(itemNode));
            } else {
                bulkResponse.results.add(createBulkErrorResult(itemNode));
            }
        }
    }

    private BulkFailureResult createBulkErrorResult(JsonNode errorNode) throws IOException {
        return new BulkFailureResult(
                errorNode.get("operation_index").asInt(),
                ResponseConverterUtil.jsonToDomainObject(mapper, errorNode.get("error"), BulkError.class));
    }

    private BulkSuccessResult createBulkSuccessResult(JsonNode successNode) throws IOException {
        BulkSuccessResult bulkSuccessResult;
        final int operation_index = successNode.get("operation_index").asInt();
        if (successNode.has("item")) {
            bulkSuccessResult = createBulkSuccessResultWithItem(successNode.get("item"), operation_index);
        } else {
            bulkSuccessResult = new BulkSuccessResult<ItemPath>(operation_index, null);
        }

        return bulkSuccessResult;
    }

    private BulkSuccessResult createBulkSuccessResultWithItem(JsonNode itemNode, int operation_index) throws IOException {
        BulkSuccessResult bulkSuccessResult;
        JsonNode pathNode = itemNode.get("path");
        ItemKind kind = ResponseConverterUtil.parseItemKind(pathNode.get("kind").asText());

        if (kind == ItemKind.ITEM) {
            bulkSuccessResult = new BulkSuccessResult<ItemPath>(
                    operation_index,
                    ResponseConverterUtil.jsonToDomainObject(mapper, pathNode, ItemPath.class));
        } else if (kind == ItemKind.EVENT) {
            bulkSuccessResult = new BulkSuccessResult<EventPath>(
                    operation_index,
                    ResponseConverterUtil.jsonToDomainObject(mapper, pathNode, EventPath.class));
        } else {
            throw new IllegalStateException(String.format("Unable to handle bulk result with kind: '%s'", kind));
        }
        return bulkSuccessResult;
    }
}
