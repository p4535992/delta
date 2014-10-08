<<<<<<< HEAD
package ee.webmedia.alfresco.common.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Vladimir Drozdik
 *         Ask confirmation from user. Can be setup by giving message title and confirmable object,
 *         which means that object has methods to execute when ok button pushed;
 *         also confirmContext as additional parameter can be used when same class calls confirmDialog several times,
 *         confirmContext then defines which action to perform.
 */
public class ConfirmDialog extends BaseDialogBean {

    public static final String BEAN_NAME = "ConfirmDialog";
    private static final long serialVersionUID = 1L;
    private Confirmable sourceObj;
    private String confirmTitle;
    private Object confirmContext;

    public void setupConfirmDialog(Confirmable sourceObj, List<MessageData> messageDataList, String confirmTitle, Object confirmContext) {
        this.sourceObj = sourceObj;
        this.confirmTitle = confirmTitle;
        this.confirmContext = confirmContext;
        for (MessageData messageData : messageDataList) {
            MessageUtil.addStatusMessage(messageData);
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        sourceObj.afterConfirmationAction(confirmContext);
        return outcome;
    }

    @Override
    public String getContainerTitle() {
        return confirmTitle;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("continue");
    }
}
=======
package ee.webmedia.alfresco.common.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 *         Ask confirmation from user. Can be setup by giving message title and confirmable object,
 *         which means that object has methods to execute when ok button pushed;
 *         also confirmContext as additional parameter can be used when same class calls confirmDialog several times,
 *         confirmContext then defines which action to perform.
 */
public class ConfirmDialog extends BaseDialogBean {

    public static final String BEAN_NAME = "ConfirmDialog";
    private static final long serialVersionUID = 1L;
    private Confirmable sourceObj;
    private String confirmTitle;
    private Object confirmContext;

    public void setupConfirmDialog(Confirmable sourceObj, List<MessageData> messageDataList, String confirmTitle, Object confirmContext) {
        this.sourceObj = sourceObj;
        this.confirmTitle = confirmTitle;
        this.confirmContext = confirmContext;
        for (MessageData messageData : messageDataList) {
            MessageUtil.addStatusMessage(messageData);
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        sourceObj.afterConfirmationAction(confirmContext);
        return outcome;
    }

    @Override
    public String getContainerTitle() {
        return confirmTitle;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("continue");
    }
}
>>>>>>> develop-5.1
