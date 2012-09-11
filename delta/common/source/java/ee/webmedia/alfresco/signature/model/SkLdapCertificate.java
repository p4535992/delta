package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * @author Alar Kvell
 */
public class SkLdapCertificate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String cn;
    private final String serialNumber;
    private final byte[] userCertificate;

    public SkLdapCertificate(String cn, String serialNumber, byte[] userCertificate) {
        Assert.hasText(cn);
        Assert.hasText(serialNumber);
        Assert.notNull(userCertificate);
        this.cn = cn;
        this.serialNumber = serialNumber;
        this.userCertificate = userCertificate;
    }

    public String getCn() {
        return cn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public byte[] getUserCertificate() {
        return userCertificate;
    }

    @Override
    public String toString() {
        return "SkLdapCertificate[cn=" + cn + ", serialNumber=" + serialNumber + ", userCertificate=byte[" + userCertificate.length + "]]";
    }

}
