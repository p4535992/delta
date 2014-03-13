package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * This updater should run only in SIM 3.13 environment to add missing delivererName values to instrumentOfDeliveryAndReceipt documents
 * which were not migrated during during 2.5 -> 3.13 migration. See cl task 215711 for details.
 */
public class UpdateInstrumentOfDeliveryAndReceiptVersion extends AbstractNodeUpdater {
    private final Log log = LogFactory.getLog(getClass());
    private Integer latestDocumentTypeVersion = -1;

    @Override
    protected void executeUpdater() throws Exception {
        if (isEnabled()) {
            DocumentType documentType = BeanHelper.getDocumentAdminService().getDocumentType("instrumentOfDeliveryAndReceipt", null);
            if (documentType == null) {
                log.info("Document of type instrumentOfDeliveryAndReceipt is not present, skipping updater");
                return;
            }
            DocumentTypeVersion latestVersion = documentType.getLatestDocumentTypeVersion();
            Field delivererNameField = latestVersion.getFieldsDeeplyById().get("delivererName");
            if (delivererNameField == null) {
                log.info("Latest version of document of type instrumentOfDeliveryAndReceipt does not contain delivererName field, skipping updater");
                return;
            }
            latestDocumentTypeVersion = latestVersion.getVersionNr();
            Assert.isTrue(latestDocumentTypeVersion != null && latestDocumentTypeVersion >= 0);
        }
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                generateStringExactQuery("instrumentOfDeliveryAndReceipt", DocumentAdminModel.Props.OBJECT_TYPE_ID));
        List<ResultSet> result = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR, latestDocumentTypeVersion);
        return null;
    }

}
