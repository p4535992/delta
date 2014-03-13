package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.signature.model.SignatureDigest;

public class SignatureTask extends Task {
    private static final long serialVersionUID = 1L;

    private SignatureDigest signatureDigest;

    protected SignatureTask(WmNode node, Workflow parent, Integer outcomes) {
        super(node, parent, outcomes);
    }

    @Override
    protected Task copy(Workflow parent) {
        return copyImpl(new SignatureTask(getNode().copy(), parent, getOutcomes()));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        SignatureTask task = (SignatureTask) super.copyImpl(copy);
        if (signatureDigest != null) {
            task.signatureDigest = new SignatureDigest(signatureDigest.getDigestHex(), signatureDigest.getCertHex(), signatureDigest.getDate());
        }
        @SuppressWarnings("unchecked")
        T result = (T) task;
        return result;
    }

    public SignatureDigest getSignatureDigest() {
        return signatureDigest;
    }

    public void setSignatureDigest(SignatureDigest signatureDigest) {
        this.signatureDigest = signatureDigest;
    }

}
