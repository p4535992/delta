package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.List;

import org.springframework.util.Assert;

public class SkLdapCertificate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String cn;
    private final String serialNumber;
    private final List<byte[]> userCertificate;
    private final byte[] userEncryptionCertificate;

    public SkLdapCertificate(String cn, String serialNumber, List<byte[]> userCertificate, byte[] userEncryptionCertificate) {
        Assert.hasText(cn);
        Assert.hasText(serialNumber);
        Assert.notNull(userCertificate);
        this.cn = cn;
        this.serialNumber = serialNumber;
        this.userCertificate = userCertificate;
        this.userEncryptionCertificate = userEncryptionCertificate;
    }

    public String getCn() {
        return cn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public byte[] getUserEncryptionCertificate(){
        return userEncryptionCertificate;
    }

    public List<byte[]> getUserCertificate() {
        return userCertificate;
    }

    @Override
    public String toString() {
        return "SkLdapCertificate[cn=" + cn + ", serialNumber=" + serialNumber + ", userCertificate listSize: " + userCertificate.size() + "]";
    }

}
