package ee.webmedia.alfresco.docdynamic.web;

public class MetadataBlock extends AbstractBlock {
    private static final long serialVersionUID = 1L;

    public boolean isFieldChangeableIfWorkingDoc() {
        return true;
        // doccom:docStatus!=töös
        // return !DocumentStatus.WORKING.getValueName().equals(node.getProperties().get(DocumentCommonModel.Props.DOC_STATUS)) || isNotEditable();
    }

}
