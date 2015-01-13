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
