package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;

public class SignatureDigest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String digestHex;
    private String certHex;
    private Date date;

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

}
