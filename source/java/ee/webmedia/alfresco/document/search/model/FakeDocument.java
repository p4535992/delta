package ee.webmedia.alfresco.document.search.model;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.maais.model.MaaisModel;
import ee.webmedia.alfresco.utils.MessageUtil;

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
        if (getType().equals(MaaisModel.Types.MAAIS_CASE)) {
            return I18NUtil.getMessage("docSearch_result_maais_case_title");
        }
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
    public String getRegNumber() {
        if (getType().equals(MaaisModel.Types.MAAIS_CASE)) {
            return (String) getProperties().get(MaaisModel.Props.CASE_NUMBER);
        }
        return super.getRegNumber();
    }

    @Override
    public String getDocName() {
        // XXX: kui muid properteid ei vajata(vaja järgi uurida), võiks lazyInit asemel konkreetse property tagastada
        // return (String) getServiceRegistry().getNodeService().getProperty(nodeRef, CaseModel.Props.TITLE);
        lazyInit();
        String docName;
        if (getType().equals(MaaisModel.Types.MAAIS_CASE)) {
            docName = MessageUtil.getMessage("docSearch_result_maais_docName", getProperties().get(MaaisModel.Props.CASE_RELATED_PERSON),
                    getProperties().get(MaaisModel.Props.LAND_NUMBER), getProperties().get(MaaisModel.Props.LAND_NAME));
        } else {
            docName = (String) getProperties().get(CaseModel.Props.TITLE);
        }
        return docName;
    }

    @Override
    public String toString() {
        return "FakeDocument ["
                + "\n\tdocName()=" + getDocName() + ", "
                + "\n\tcssStyleClass()=" + getCssStyleClass() + ","
                + "\n\tdocumentTypeName()=" + getDocumentTypeName() + "\n]";
    }

}