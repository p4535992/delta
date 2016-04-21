package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;

import org.antlr.grammar.v3.ANTLRv3Parser.rewrite_alternative_return;
import org.digidoc4j.DataToSign;

public class SignatureDigest implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private DataToSign dataToSign;
    private final String digestHex;
    private final String certHex;
    private final Date date;

    public SignatureDigest(String digestHex, String certHex, Date date) {
        this.digestHex = digestHex;
        this.certHex = certHex;
        this.date = date;
    }
    
    public SignatureDigest(String digestHex, String certHex, Date date, DataToSign dataToSign) {
        this.digestHex = digestHex;
        this.certHex = certHex;
        this.date = date;
        this.dataToSign = dataToSign;
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
    
    public void setDataToSign(DataToSign dataToSign) {
    	this.dataToSign = dataToSign;
    }
    
    public DataToSign getDataToSign() {
    	return dataToSign;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SignatureDigest[");
        sb.append("digestHex=").append(digestHex);
        sb.append(", certHex=");
        sb.append(certHex);
        sb.append(", date=").append(date);
        if (date != null) {
            sb.append(" ").append(date.getTime());
        }
        sb.append("]");
        return sb.toString();
    }

}
