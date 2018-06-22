package ee.smit.digisign;

import java.util.Date;

public class SignCertificate {
    private String cn;
    private String serialNumber;
    private byte[] data;
    private boolean isEncryptionSupported;
    private String issuerCn;
    private Date notAfter;
    private Date notBefore;

    public boolean isEncryptionSupported() {
        return isEncryptionSupported;
    }

    public void setEncryptionSupported(boolean encryptionSupported) {
        isEncryptionSupported = encryptionSupported;
    }

    public String getIssuerCn() {
        return issuerCn;
    }

    public void setIssuerCn(String issuerCn) {
        this.issuerCn = issuerCn;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getSerialnumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }
}
