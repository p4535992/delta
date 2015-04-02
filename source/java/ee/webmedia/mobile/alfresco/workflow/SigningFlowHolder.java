package ee.webmedia.mobile.alfresco.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

public class SigningFlowHolder {

    public static final String BEAN_NAME = "signingFlowHolder";

    private long signingFlowCounter;
    private final Map<Long, MobileSigningFlowContainer> signingContainers = new HashMap<Long, MobileSigningFlowContainer>();

    public long addSigningFlow(MobileSigningFlowContainer mobileSigningFlowContainer, HttpSession session) {
        signingContainers.put(++signingFlowCounter, mobileSigningFlowContainer);
        mobileSigningFlowContainer.resolveUserPhoneNr(session);
        return signingFlowCounter;
    }

    public MobileSigningFlowContainer getSigningFlow(long id) {
        return signingContainers.get(id);
    }

    public void removeSigningFlow(long id) {
        signingContainers.remove(id);
    }

}
