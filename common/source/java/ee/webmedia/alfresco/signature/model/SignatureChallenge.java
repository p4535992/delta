package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds intermediate data for Mobile-ID signing.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class SignatureChallenge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int sesscode;
    private final String challengeId;
    private final List<String> digestHexs;
    private final String signatureId;
    private final String format;
    private final String version;

    public SignatureChallenge(int sesscode, String challengeId, List<String> digestHexs, String signatureId, String format, String version) {
        this.sesscode = sesscode;
        this.challengeId = challengeId;
        this.digestHexs = Collections.unmodifiableList(new ArrayList<String>(digestHexs));
        this.signatureId = signatureId;
        this.format = format;
        this.version = version;
    }

    public int getSesscode() {
        return sesscode;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public List<String> getDigestHexs() {
        return digestHexs;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public String getFormat() {
        return format;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SignatureChallenge[");
        sb.append("sesscode=").append(sesscode);
        sb.append(", challengeId=").append(challengeId);
        sb.append(", digestHexs=").append(digestHexs);
        sb.append(", signatureId=").append(signatureId);
        sb.append(", format=").append(format);
        sb.append(", version=").append(version);
        sb.append("]");
        return sb.toString();
    }

}
