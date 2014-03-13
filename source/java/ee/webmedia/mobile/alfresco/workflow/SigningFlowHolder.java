package ee.webmedia.mobile.alfresco.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.user.service.Cas20ProxyReceivingRedirectingTicketValidationFilter;

public class SigningFlowHolder {

    public static final String BEAN_NAME = "signingFlowHolder";
    public static final String LAST_USED_MOBILE_ID_NUMBER = "lastUsedMobileIdNumber";

    private long signingFlowCounter;
    private final Map<Long, MobileSigningFlowContainer> signingContainers = new HashMap<Long, MobileSigningFlowContainer>();

    public long addSigningFlow(MobileSigningFlowContainer mobileSigningFlowContainer, HttpSession session) {
        signingContainers.put(++signingFlowCounter, mobileSigningFlowContainer);
        String phoneNumber = null;
        String signInPhoneNumber = (String) session.getAttribute(Cas20ProxyReceivingRedirectingTicketValidationFilter.PHONE_NUMBER);
        String lastUsedPhoneNumber = (String) session.getAttribute(LAST_USED_MOBILE_ID_NUMBER);
        if (StringUtils.isNotBlank(signInPhoneNumber)) {
            phoneNumber = signInPhoneNumber;
        } else if (StringUtils.isNotBlank(lastUsedPhoneNumber)) {
            phoneNumber = lastUsedPhoneNumber;
        } else {
            phoneNumber = BeanHelper.getUserService().getUserMobilePhone(AuthenticationUtil.getFullyAuthenticatedUser());
        }
        mobileSigningFlowContainer.setPhoneNumber(StringUtils.isNotBlank(phoneNumber) ? phoneNumber : null);
        return signingFlowCounter;
    }

    public MobileSigningFlowContainer getSigningFlow(long id) {
        return signingContainers.get(id);
    }

    public void removeSigningFlow(long id) {
        signingContainers.remove(id);
    }

}
