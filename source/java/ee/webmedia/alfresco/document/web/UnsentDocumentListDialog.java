package ee.webmedia.alfresco.document.web;

import java.util.HashSet;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;

public class UnsentDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UnsentDocumentListDialog";

    public static final Set<QName> DOC_PROPS_WITH_OWNER_STRUCT_UNIT = new HashSet<>();

    static {
        DOC_PROPS_WITH_OWNER_STRUCT_UNIT.addAll(DOC_PROPS_TO_LOAD);
        DOC_PROPS_WITH_OWNER_STRUCT_UNIT.add(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT);
    }

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documentProvider = new DocumentListDataProvider(getDocumentSearchService().searchRecipientFinishedDocuments(), true, DOC_PROPS_WITH_OWNER_STRUCT_UNIT);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_unsent_list");
    }

    @Override
    public boolean isShowOrgStructColumn() {
        return true;
    }
}
