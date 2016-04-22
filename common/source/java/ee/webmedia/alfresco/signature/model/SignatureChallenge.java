package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.digidoc4j.DataToSign;

/**
 * Holds intermediate data for Mobile-ID signing.
 */
public class SignatureChallenge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sesscode;
    private final String challengeId;
    private final List<String> digestHexs;
    private final String signatureId;
    private final String format;
    private final String version;
    private DataToSign dataToSign;

    public SignatureChallenge(int sesscode, String challengeId, List<String> digestHexs, String signatureId, String format, String version) {
        this.sesscode =  String.valueOf(sesscode);
        this.challengeId = challengeId;
        this.digestHexs = Collections.unmodifiableList(new ArrayList<String>(digestHexs));
        this.signatureId = signatureId;
        this.format = format;
        this.version = version;
    }
    
    public SignatureChallenge(String sesscode, String challengeId, DataToSign dataToSign) {
        this.sesscode =  sesscode;
        this.challengeId = challengeId;
        this.dataToSign = dataToSign;
        this.digestHexs = null;
        this.signatureId = null;
        this.format = null;
        this.version = null;
    }

    public String getSesscode() {
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
    
    public void setDataToSign(DataToSign dataToSign) {
    	this.dataToSign = dataToSign;
    }
    
    public DataToSign getDataToSign() {
    	return dataToSign;
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
