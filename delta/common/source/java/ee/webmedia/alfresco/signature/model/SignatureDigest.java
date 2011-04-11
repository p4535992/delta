package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

public class SignatureDigest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String digestHex;
    private final String certHex;
    private final Date date;

    public SignatureDigest(String digestHex, String certHex, Date date) {
        this.digestHex = digestHex;
        this.certHex = certHex;
        this.date = date;
    }

    public String getDigestHex() {
        return digestHex;
    }

    public String getCertHex() {
        return certHex;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SignatureDigest[");
        sb.append("digestHex=").append(digestHex);
        sb.append(", certHex=");
        if (certHex == null) {
            sb.append("null");
        } else {
            sb.append(StringUtils.left(certHex, 16)).append("...[").append(certHex.length()).append("]");
        }
        sb.append(", date=").append(date);
        sb.append("]");
        return sb.toString();
    }

}
