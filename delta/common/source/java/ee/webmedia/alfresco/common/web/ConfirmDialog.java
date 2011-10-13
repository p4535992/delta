package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

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
    private String confirmMessage;
    private String confirmTitle;
    private Object confirmContext;

    public void setupConfirmDialog(Confirmable sourceObj, String confirmMessage, String confirmTitle, Object confirmContext) {
        this.sourceObj = sourceObj;
        this.confirmMessage = confirmMessage;
        this.confirmTitle = confirmTitle;
        this.confirmContext = confirmContext;
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

    public String getConfirmMessage() {
        return confirmMessage;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("continue");
    }
}
