<<<<<<< HEAD
package ee.webmedia.alfresco.orgstructure.web;

import java.io.Serializable;

/**
 * @author Riina Tens
 */
public class RsAccessStatusBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "RsAccessStatusBean";

    private boolean canUserAccessRestrictedDelta;

    public void setCanUserAccessRestrictedDelta(boolean canUserAccessRestrictedDelta) {
        this.canUserAccessRestrictedDelta = canUserAccessRestrictedDelta;
    }

    public boolean isCanUserAccessRestrictedDelta() {
        return canUserAccessRestrictedDelta;
    }

}
=======
package ee.webmedia.alfresco.orgstructure.web;

import java.io.Serializable;

public class RsAccessStatusBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "RsAccessStatusBean";

    private boolean canUserAccessRestrictedDelta;

    public void setCanUserAccessRestrictedDelta(boolean canUserAccessRestrictedDelta) {
        this.canUserAccessRestrictedDelta = canUserAccessRestrictedDelta;
    }

    public boolean isCanUserAccessRestrictedDelta() {
        return canUserAccessRestrictedDelta;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
