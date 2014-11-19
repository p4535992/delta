package ee.webmedia.alfresco.substitute.model;

import java.io.Serializable;

<<<<<<< HEAD
/**
 * @author Romet Aidla
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
