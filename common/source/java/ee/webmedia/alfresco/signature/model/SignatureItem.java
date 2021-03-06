package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.util.StringUtils;

public class SignatureItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String legalCode;
    private final Date signingTime;
    private final List<String> claimedRoles;

    /**
     * SignatureProductionPlace string representation.
     */
    private final String address;
    private final boolean valid;
    private final String encrytionType;

    public SignatureItem(String name, String legalCode, Date signingTime, List<String> claimedRoles, String address, boolean valid, String encrytionType) {
        this.name = name;
        this.legalCode = legalCode;
        this.signingTime = signingTime;
        this.claimedRoles = claimedRoles;
        this.address = address;
        this.valid = valid;
        this.encrytionType = encrytionType;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public String getRole() {
        return StringUtils.collectionToDelimitedString(claimedRoles, ", ");
    }

    public String getName() {
        return name;
    }

    public String getLegalCode() {
        return legalCode;
    }

    public Date getSigningTime() {
        return signingTime;
    }

    public List<String> getClaimedRoles() {
        return claimedRoles;
    }

    public String getAddress() {
        return address;
    }

    public String getEncrytionType(){
        return encrytionType;
    }
}
