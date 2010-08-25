package ee.webmedia.alfresco.document.search.model;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.type.model.DocumentType;

/**
 * Used to show case title in documents list
 * 
 * @author Ats Uiboupin
 */
public class FakeDocument extends Document {
    private static final String CASE = "case";
    private static final long serialVersionUID = 1L;
    private static final String CASE_TITLE = I18NUtil.getMessage("docSearch_result_case_title");

    public FakeDocument(NodeRef nodeRef) {
        super(nodeRef);
    }

    @Override
    public DocumentType getDocumentType() {
        return null;// not a real document - hence no real document type
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
        // XXX: kui muid properteid ei vajata(vaja järgi uurida), võiks lazyInit asemel konkreetse property tagastada
        // return (String) getServiceRegistry().getNodeService().getProperty(nodeRef, CaseModel.Props.TITLE);
        lazyInit();
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