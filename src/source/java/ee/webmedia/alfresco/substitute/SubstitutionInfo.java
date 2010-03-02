package ee.webmedia.alfresco.substitute;

import java.io.Serializable;

import ee.webmedia.alfresco.substitute.model.Substitute;

/**
 * @author Romet Aidla
 */
public class SubstitutionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isSubstituting = false;
    private Substitute substitution;

    public SubstitutionInfo() {
    }

    public SubstitutionInfo(Substitute substitution) {
        isSubstituting = true;
        this.substitution = substitution;
    }

    public Substitute getSubstitution() {
        return substitution;
    }

    public boolean isSubstituting() {
        return isSubstituting;
    }

    public String getSelectedSubstitution() {
        return isSubstituting
                ? substitution.getNodeRef().toString()
                : "";
    }
}
