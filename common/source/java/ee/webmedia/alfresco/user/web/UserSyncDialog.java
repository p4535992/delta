<<<<<<< HEAD
package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class UserSyncDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserSyncDialog";

    private String userIdCodes;

    public void syncUsers(@SuppressWarnings("unused") ActionEvent event) {
        if (StringUtils.isBlank(userIdCodes)) {
            MessageUtil.addErrorMessage("user_sync_idCodes_blank");
        }
        UserRegistrySynchronizer userRegistrySynchronizer = BeanHelper.getSpringBean(UserRegistrySynchronizer.class, "UserRegistrySynchronizer");
        String[] idCodes = StringUtils.split(StringUtils.deleteWhitespace(userIdCodes), ',');
        for (String idCode : idCodes) {
            String result = userRegistrySynchronizer.createOrUpdatePersonByIdCode(idCode);
            if (StringUtils.isNotBlank(result)) {
                MessageUtil.addInfoMessage("user_sync_success", idCode);
            } else {
                MessageUtil.addInfoMessage("user_sync_fail", idCode);
            }
        }
        userIdCodes = null;
    }

    @Override
    public String getContainerTitle() {
        String messageKey = getUserService().isGroupsEditingAllowed() ? "user_sync_title_amr" : "user_sync_title_ad";
        return MessageUtil.getMessage(messageKey);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null; // Not used
    }

    public String getUserIdCodes() {
        return userIdCodes;
    }

    public void setUserIdCodes(String userIdCodes) {
        this.userIdCodes = userIdCodes;
    }

}
=======
package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

public class UserSyncDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "UserSyncDialog";

    private String userIdCodes;

    public void syncUsers(@SuppressWarnings("unused") ActionEvent event) {
        if (StringUtils.isBlank(userIdCodes)) {
            MessageUtil.addErrorMessage("user_sync_idCodes_blank");
        }
        UserRegistrySynchronizer userRegistrySynchronizer = BeanHelper.getSpringBean(UserRegistrySynchronizer.class, "UserRegistrySynchronizer");
        String[] idCodes = StringUtils.split(StringUtils.deleteWhitespace(userIdCodes), ',');
        for (String idCode : idCodes) {
            String result = userRegistrySynchronizer.createOrUpdatePersonByIdCode(idCode);
            if (StringUtils.isNotBlank(result)) {
                MessageUtil.addInfoMessage("user_sync_success", idCode);
            } else {
                MessageUtil.addInfoMessage("user_sync_fail", idCode);
            }
        }
        userIdCodes = null;
    }

    @Override
    public String getContainerTitle() {
        String messageKey = getUserService().isGroupsEditingAllowed() ? "user_sync_title_amr" : "user_sync_title_ad";
        return MessageUtil.getMessage(messageKey);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null; // Not used
    }

    public String getUserIdCodes() {
        return userIdCodes;
    }

    public void setUserIdCodes(String userIdCodes) {
        this.userIdCodes = userIdCodes;
    }

}
>>>>>>> develop-5.1
