package ee.webmedia.alfresco.document.search.model;

import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.model.Document;

/**
 * Used to show case title in documents list
 */
public class FakeDocument extends Document {
    private static final String CASE = "case";
    private static final long serialVersionUID = 1L;
    private static final String CASE_TITLE = I18NUtil.getMessage("docSearch_result_case_title");

    public FakeDocument(NodeRef nodeRef) {
        super(nodeRef);
    }

    public FakeDocument(NodeRef nodeRef, Map<String, Object> properties) {
        super(nodeRef, properties);
    }

    @Override
    public String getDocumentTypeName() {
        return CASE_TITLE;
    }

    @Override
    public String getCssStyleClass() {
        return CASE;
    }

    @Override
    public String getSender() {
        return "";
    }

    @Override
    public String getAllRecipients() {
        return "";
    }

    @Override
    public String getDocName() {
        return (String) getProperties().get(CaseModel.Props.TITLE);
    }

    @Override
    public String toString() {
        return "FakeDocument ["
                + "\n\tdocName()=" + getDocName() + ", "
                + "\n\tcssStyleClass()=" + getCssStyleClass() + ","
                + "\n\tdocumentTypeName()=" + getDocumentTypeName() + "\n]";
    }

}