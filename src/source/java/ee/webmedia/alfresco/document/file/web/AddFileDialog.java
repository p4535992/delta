package ee.webmedia.alfresco.document.file.web;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;

/**
 * @author Dmitri Melnikov
 */
public class AddFileDialog extends AddContentDialog {
    private static final long serialVersionUID = 1L;

    private static final String ERR_EXISTING_FILE = "add_file_existing_file";

    private transient UserService userService;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        try {
            return super.finishImpl(context, outcome);
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }
    }
    
    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        clearUpload();
        if (this.showOtherProperties) {
            this.browseBean.setDocument(new Node(this.createdNode));
        }
        // XXX Should probably be refactored to a single service method to add all the aspects there.
        addVersionModifiedAspect(this.createdNode);
        getNodeService().addAspect(this.createdNode, DocumentCommonModel.Aspects.FILE, null);
        NodeRef parentDocRef = getNodeService().getPrimaryParent(this.createdNode).getParentRef();
        if (getNodeService().hasAspect(parentDocRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            getNodeService().addAspect(this.createdNode, DocumentCommonModel.Aspects.SEARCHABLE, null);
        }
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    public String getFileNameWithoutExtension() {
        return FilenameUtils.removeExtension(super.getFileName());
    }

    public void setFileNameWithoutExtension(String name) {
        setFileName(name + "." + FilenameUtils.getExtension(super.getFileName()));
    }
    
    private void addVersionModifiedAspect(NodeRef nodeRef) {
        if (getNodeService().hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED) == false) {
            Map<QName, Serializable> properties = getNodeService().getProperties(nodeRef);
            
            String user = (String)properties.get(ContentModel.PROP_CREATOR);
            Map<QName, Serializable> personProps = getUserService().getUserProperties(user);
            String first = (String) personProps.get(ContentModel.PROP_FIRSTNAME);
            String last = (String) personProps.get(ContentModel.PROP_LASTNAME);
            Date modified = DefaultTypeConverter.INSTANCE.convert(Date.class, properties.get(ContentModel.PROP_CREATED));

            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, modified );
            aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, first);
            aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, last);

            getNodeService().addAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED, aspectProperties);
        }
    }
    
    // START: getters / setters
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }
    // END: getters / setters
}
