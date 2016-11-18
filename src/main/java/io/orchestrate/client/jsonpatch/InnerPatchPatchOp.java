package io.orchestrate.client.jsonpatch;

public class InnerPatchPatchOp extends JsonPatchOp {
    private final boolean conditional;

    public InnerPatchPatchOp(String path, JsonPatch patch, boolean conditional) {
        super("patch", path, patch.getOps());
        this.conditional = conditional;
    }

    public InnerPatchPatchOp(String path, JsonPatch.Builder patch, boolean conditional) {
        this(path, patch.build(), conditional);
    }

    public boolean isConditional() {
        return conditional;
    }
}
