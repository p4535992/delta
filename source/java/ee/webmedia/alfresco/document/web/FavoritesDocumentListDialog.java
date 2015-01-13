package ee.webmedia.alfresco.document.web;

import java.util.Collections;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class FavoritesDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private NodeRef containerNodeRef;
    private String dirName;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getFavorites(containerNodeRef);
        Collections.sort(documents);
        containerNodeRef = null;
    }

    public void setup(ActionEvent event) {
        containerNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        String param = ActionUtil.getParam(event, "dirName");
        dirName = param;
    }

    @Override
    public String getListTitle() {
        if (StringUtils.isBlank(dirName)) {
            return MessageUtil.getMessage("document_my_favorites");
        }
        String title = dirName;
        dirName = null;
        return title;

    }

    public NodeRef getUserNodeRef() {
        return BeanHelper.getUserService().getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
    }

}
