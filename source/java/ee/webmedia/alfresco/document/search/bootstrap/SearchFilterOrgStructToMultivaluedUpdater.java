package ee.webmedia.alfresco.document.search.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.bootstrap.ConvertToDynamicDocumentsUpdater;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

/**
 * Update search filter struct_unit type properties to multivalued
 * and fill appropriate data from organization properties if possible.
 */
public class SearchFilterOrgStructToMultivaluedUpdater extends AbstractNodeUpdater {

    private Map<String, List<String>> orgStructNameToPath;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentSearchModel.Types.FILTER, TaskSearchModel.Types.FILTER);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void executeUpdater() throws Exception {
        orgStructNameToPath = new HashMap<String, List<String>>();
        ConvertToDynamicDocumentsUpdater.fillOrgStructNameToPath(orgStructNameToPath, BeanHelper.getOrganizationStructureService());
        super.executeUpdater();
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName filterType = nodeService.getType(nodeRef);
        boolean isDocumentFilter = DocumentSearchModel.Types.FILTER.equals(filterType);
        if (!isDocumentFilter && !TaskSearchModel.Types.FILTER.equals(filterType)) {
            return new String[] { "not required type: " + filterType };
        }
        QName orgStructPropQName = isDocumentFilter ? QName.createQName(DocumentSearchModel.URI, "ownerOrgStructUnit") : TaskSearchModel.Props.ORGANIZATION_NAME;
        Serializable orgStructName = nodeService.getProperty(nodeRef, orgStructPropQName);
        if (!(orgStructName instanceof String)) {
            return new String[] { "not updatable, propQName=" + orgStructPropQName + "; value=" + orgStructName };
        }
        nodeService.setProperty(nodeRef, orgStructPropQName, (Serializable) ConvertToDynamicDocumentsUpdater.getStructUnitPath((String) orgStructName, orgStructNameToPath));
        return new String[] { "updated" };
    }
}
