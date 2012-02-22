package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds intermediate data for Mobile-ID signing.
 * 
 * @author Alar Kvell
 */
public class SignatureChallenge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sesscode;
    private final String challengeId;
    private final String signatureId;
    private final List<String> digestHexs;

    public SignatureChallenge(String sesscode, String challengeId, String signatureId, List<String> digestHexs) {
        this.sesscode = sesscode;
        this.challengeId = challengeId;
        this.signatureId = signatureId;
        this.digestHexs = Collections.unmodifiableList(new ArrayList<String>(digestHexs));
    }

    public String getSesscode() {
        return sesscode;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public List<String> getDigestHexs() {
        return digestHexs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SignatureChallenge[");
        sb.append("sesscode=").append(sesscode);
        sb.append(", challengeId=").append(challengeId);
        sb.append(", signatureId=").append(signatureId);
        sb.append(", digestHexs=").append(digestHexs);
        sb.append("]");
        return sb.toString();
    }

}
