package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

<<<<<<< HEAD
/**
 * @author Priit Pikk
 */
=======
>>>>>>> develop-5.1
public class CaseFileStatusFieldFixBootstrap extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, joinQueryPartsAnd(
                    generateTypeQuery(DocumentAdminModel.Types.FIELD),
                    generatePropertyBooleanQuery(DocumentAdminModel.Props.SYSTEMATIC, true),
                    generateStringExactQuery(DocumentDynamicModel.Props.STATUS.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                )));
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        Map<QName, Serializable> props = nodeService.getProperties(fieldRef);
        String log[];
        if (Boolean.TRUE.equals(props.get(DocumentAdminModel.Props.SYSTEMATIC))
                && DocumentDynamicModel.Props.STATUS.getLocalName().equals(props.get(DocumentAdminModel.Props.ORIGINAL_FIELD_ID))) {
            props.remove(DocumentAdminModel.Props.DEFAULT_VALUE);
            props.put(DocumentAdminModel.Props.FIELD_TYPE, FieldType.COMBOBOX.name());
            props.put(DocumentAdminModel.Props.CLASSIFICATOR, "docListUnitStatus");
            props.put(DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE, DocListUnitStatus.OPEN.getValueName());
            nodeService.setProperties(fieldRef, props);
            log = new String[] { "Systematic field changed", props.toString() };
        } else {
            log = new String[] { "[no match]" };
        }
        return log;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
