package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.util.StringUtils;

public class SignatureItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String legalCode;
    private Date signingTime;
    private List<String> claimedRoles;

    /**
     * SignatureProductionPlace string representation.
     */
    private String address;
    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public String getRole() {
        return StringUtils.collectionToDelimitedString(claimedRoles, ", ");
    }

    public SignatureItem setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    public String getName() {
        return name;
    }

    public SignatureItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getLegalCode() {
        return legalCode;
    }

    public SignatureItem setLegalCode(String legalCode) {
        this.legalCode = legalCode;
        return this;
    }

    public Date getSigningTime() {
        return signingTime;
    }

    public SignatureItem setSigningTime(Date signingTime) {
        this.signingTime = signingTime;
        return this;
    }

    public List<String> getClaimedRoles() {
        return claimedRoles;
    }

    public SignatureItem setClaimedRoles(List<String> claimedRoles) {
        this.claimedRoles = claimedRoles;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public SignatureItem setAddress(String address) {
        this.address = address;
        return this;
    }

}
