package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.signature.model.SignatureDigest;

/**
 * @author Dmitri Melnikov
 */
public class SignatureTask extends Task {
    private static final long serialVersionUID = 1L;

    private SignatureDigest signatureDigest;

    protected SignatureTask(WmNode node, Workflow parent, Integer outcomes) {
        super(node, parent, outcomes);
    }

    public SignatureDigest getSignatureDigest() {
        return signatureDigest;
    }

    public void setSignatureDigest(SignatureDigest signatureDigest) {
        this.signatureDigest = signatureDigest;
    }

}
