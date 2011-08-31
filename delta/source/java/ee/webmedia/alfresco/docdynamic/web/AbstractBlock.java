package ee.webmedia.alfresco.docdynamic.web;

public abstract class AbstractBlock implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    protected DocumentDynamicDialog documentDynamicDialog;

    @Override
    public void setDocumentDynamicDialog(DocumentDynamicDialog documentDynamicDialog) {
        this.documentDynamicDialog = documentDynamicDialog;
    }

}
