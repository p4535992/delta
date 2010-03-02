package ee.webmedia.alfresco.document.search.model;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.model.Document;

/**
 * Used to show case title in documents list
 * 
 * @author Ats Uiboupin
 */
public class FakeDocument extends Document {
    private static final String CASE = "case";
    private static final long serialVersionUID = 1L;
    private static final String CASE_TITLE = I18NUtil.getMessage("docSearch_result_case_title");

    public FakeDocument(Node document) {
        super(document, null);
    }

    @Override
    public String getDocumentTypeName() {
        return CASE_TITLE;
    }

    @Override
    public String getDocTypeLocalName() {
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
        return (String) getNode().getProperties().get(CaseModel.Props.TITLE);
    }

    @Override
    public String toString() {
        return "FakeDocument ["
                + "\n\tgetDocName()=" + getDocName() + ", "
                + "\n\tgetDocTypeLocalName()=" + getDocTypeLocalName() + ","
                + "\n\tgetDocumentTypeName()=" + getDocumentTypeName() + "\n]";
    }

}