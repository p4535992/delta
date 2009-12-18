package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;

public class SignatureDigest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String digestHex;
    private String certHex;
    private Date date;

    public String getDigestHex() {
        return digestHex;
    }

    public void setDigestHex(String digestHex) {
        this.digestHex = digestHex;
    }

    public String getCertHex() {
        return certHex;
    }

    public void setCertHex(String certHex) {
        this.certHex = certHex;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
