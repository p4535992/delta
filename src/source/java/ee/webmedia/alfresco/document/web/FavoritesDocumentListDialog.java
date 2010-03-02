package ee.webmedia.alfresco.document.web;

import java.util.Collections;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class FavoritesDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        documents = getDocumentService().getFavorites();
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentService().getFavorites();
        Collections.sort(documents);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "documents");
    }

    public void add(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentService().addFavorite(nodeRef);
    }

    public void remove(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentService().removeFavorite(nodeRef);
    }

}
