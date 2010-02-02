package ee.webmedia.alfresco.document.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DvkDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public void setup(ActionEvent event) {
        documents = getDocumentService().getAllDocumentFromDvk();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_fromDvk");
    }

}
