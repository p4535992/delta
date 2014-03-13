package ee.webmedia.alfresco.sharepoint;

import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

/**
 * This is a callback listener for document saving event. This class is needed to customize how imported documents are saved
 */
public class DocumentImportListener implements SaveListener {

    public static final String BEAN_NAME = "sharepointDocsListener";

    @Override
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        // do nothing
    }

    @Override
    public void save(DynamicBase document) {
        if (document instanceof DocumentDynamic) {
            ((DocumentDynamic) document).setDraft(true);
            ((DocumentDynamic) document).setDraftOrImapOrDvk(true);
        }
    }

    @Override
    public String getBeanName() {
        return BEAN_NAME;
    }
}
