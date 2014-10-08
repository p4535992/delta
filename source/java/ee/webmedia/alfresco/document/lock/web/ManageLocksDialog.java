<<<<<<< HEAD
package ee.webmedia.alfresco.document.lock.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.lock.model.Lock;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;

/**
 * Dialog or releasing locked documents and files
 * 
 * @author Kaarel Jõgeva
 */
public class ManageLocksDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<Lock> locks = null;

    public void release(ActionEvent event) {
        final NodeRef object = ActionUtil.getParam(event, "object", NodeRef.class);
        final Boolean releaseAll = ActionUtil.getParam(event, "all", Boolean.class);
        int indexOf = getLocks().indexOf(new Lock(object));
        if (indexOf < 0) {
            return;
        }
        Lock l = getLocks().get(indexOf);
        if (!ActionUtil.hasParam(event, "confirmed")) {
            String msgKey = releaseAll ? "lock_release_all_confirm" : "lock_release_confirm";
            String objectName = releaseAll ? l.getDocName() : l.getFileName();

            Map<String, String> params = new HashMap<String, String>(2);
            params.put("confirmed", Boolean.TRUE.toString());
            params.put("object", object.toString());
            params.put("all", releaseAll.toString());
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl(msgKey, objectName), null, "#{ManageLocksDialog.release}",
                    params, null, null, null);
            return;
        }
        // Unlock if confirmed under System rights
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                BeanHelper.getDocLockService().unlock(object, releaseAll);
                return null;
            }

        }, AuthenticationUtil.getSystemUserName());
        reload();
    }

    @Override
    public void init(Map<String, String> parameters) {
        reload();
        super.init(parameters);
    }

    private void reload() {
        locks = BeanHelper.getDocLockService().getDocumentAndFileLocks();
        Collections.sort(locks);
    }

    @Override
    public String cancel() {
        locks = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public List<Lock> getLocks() {
        return locks;
    }

}
=======
package ee.webmedia.alfresco.document.lock.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.lock.model.Lock;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;

/**
 * Dialog or releasing locked documents and files
 */
public class ManageLocksDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<Lock> locks = null;

    public void release(ActionEvent event) {
        final NodeRef object = ActionUtil.getParam(event, "object", NodeRef.class);
        final Boolean releaseAll = ActionUtil.getParam(event, "all", Boolean.class);
        int indexOf = getLocks().indexOf(new Lock(object));
        if (indexOf < 0) {
            return;
        }
        Lock l = getLocks().get(indexOf);
        if (!ActionUtil.hasParam(event, "confirmed")) {
            String msgKey = releaseAll ? "lock_release_all_confirm" : "lock_release_confirm";
            String objectName = releaseAll ? l.getDocName() : l.getFileName();

            Map<String, String> params = new HashMap<String, String>(2);
            params.put("confirmed", Boolean.TRUE.toString());
            params.put("object", object.toString());
            params.put("all", releaseAll.toString());
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl(msgKey, objectName), null, "#{ManageLocksDialog.release}",
                    params, null, null, null);
            return;
        }
        // Unlock if confirmed under System rights
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                BeanHelper.getDocLockService().unlock(object, releaseAll);
                return null;
            }

        }, AuthenticationUtil.getSystemUserName());
        reload();
    }

    @Override
    public void init(Map<String, String> parameters) {
        reload();
        super.init(parameters);
    }

    private void reload() {
        locks = BeanHelper.getDocLockService().getDocumentAndFileLocks();
        Collections.sort(locks);
    }

    @Override
    public String cancel() {
        locks = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public List<Lock> getLocks() {
        return locks;
    }

}
>>>>>>> develop-5.1
