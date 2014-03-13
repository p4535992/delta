package ee.webmedia.alfresco.maais;

import static ee.webmedia.alfresco.common.web.BeanHelper.getMaaisService;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;

public class MaaisSessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "MaaisSessionBean";

    private Date sessionExpiryDate;
    private Long period = null;
    private String docTypeGroupName = null;

    public boolean isAuthExpired() {
        if (period == null) {
            period = BeanHelper.getParametersService().getLongParameter(Parameters.MAAIS_RENEW_SESSIONS_PERIOD);
        }
        if (sessionExpiryDate == null || (sessionExpiryDate.getTime() - System.currentTimeMillis()) <= (period.intValue() * 60000)) {
            Date expiry = getMaaisService().getUserSessionExpiry(BeanHelper.getUserService().getCurrentUserName());
            if (expiry == null) {
                sessionExpiryDate = null;
                return true;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(expiry);
            sessionExpiryDate = cal.getTime();
        }
        if (sessionExpiryDate.after(new Date())) {
            return false;
        }
        return true;
    }

    public boolean isMaaisLinkHidden(@SuppressWarnings("unused") String menuItemId) {
        if (getMaaisService().isServiceAvailable()) {
            return isAuthExpired();
        }
        return true;
    }

    public boolean isMaaisDocsLinkHidden(@SuppressWarnings("unused") String menuItemId) {
        return !getMaaisService().isServiceAvailable();
    }

    public void setSessionExpiryDate(Date date) {
        sessionExpiryDate = date;
    }

    public Date getSessionExpiryDate() {
        return sessionExpiryDate;
    }

    public String getDocTypeGroupName() {
        if (docTypeGroupName == null) {
            docTypeGroupName = BeanHelper.getParametersService().getStringParameter(Parameters.MAAIS_DOC_TYPE_GROUP_NAME);
        }
        return docTypeGroupName;
    }

}
