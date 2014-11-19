package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;

public class SignatureTask extends Task {
    private static final long serialVersionUID = 1L;

    private SignatureDigest signatureDigest;
    private SignatureChallenge signatureChallenge;

    protected SignatureTask(WmNode node, Workflow parent, Integer outcomes) {
        super(node, parent, outcomes);
    }

    @Override
    protected Task copy(Workflow parent) {
        return copyImpl(new SignatureTask(getNode().clone(), parent, getOutcomes()));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        SignatureTask task = (SignatureTask) super.copyImpl(copy);
        if (signatureDigest != null) {
            task.signatureDigest = new SignatureDigest(signatureDigest.getDigestHex(), signatureDigest.getCertHex(), signatureDigest.getDate());
        }
        if (signatureChallenge != null) {
            task.signatureChallenge = new SignatureChallenge(signatureChallenge.getSesscode(), signatureChallenge.getChallengeId(), signatureChallenge.getDigestHexs(),
                    signatureChallenge.getSignatureId(), signatureChallenge.getFormat(), signatureChallenge.getVersion());
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

    public SignatureChallenge getSignatureChallenge() {
        return signatureChallenge;
    }

    public void setSignatureChallenge(SignatureChallenge signatureChallenge) {
        this.signatureChallenge = signatureChallenge;
    }

    @Override
    public SignatureTask clone() {
        return (SignatureTask) copy();
    }

}
