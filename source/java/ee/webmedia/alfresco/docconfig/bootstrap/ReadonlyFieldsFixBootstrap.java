package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Fix for CL 166411
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class ReadonlyFieldsFixBootstrap extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_GROUP),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.SYSTEMATIC, true),
                joinQueryPartsOr(
                        generateStringExactQuery(SystematicFieldGroupNames.DOCUMENT_LOCATION, DocumentAdminModel.Props.NAME),
                        generateStringExactQuery(SystematicFieldGroupNames.ACCESS_RESTRICTION, DocumentAdminModel.Props.NAME),
                        generateStringExactQuery(SystematicFieldGroupNames.DOCUMENT_OWNER, DocumentAdminModel.Props.NAME),
                        generateStringExactQuery(SystematicFieldGroupNames.SIGNER, DocumentAdminModel.Props.NAME),
                        generateStringExactQuery(SystematicFieldGroupNames.SENDER_NAME_AND_EMAIL, DocumentAdminModel.Props.NAME)
                        ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldGroupRef) throws Exception {
        Map<QName, Serializable> oldProps = nodeService.getProperties(fieldGroupRef);
        Map<QName, Serializable> newProps = new LinkedHashMap<QName, Serializable>();
        Boolean changeable = !SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(oldProps.get(DocumentAdminModel.Props.NAME));
        newProps.put(DocumentAdminModel.Props.READONLY_FIELDS_NAME_CHANGEABLE, changeable);
        newProps.put(DocumentAdminModel.Props.READONLY_FIELDS_RULE_CHANGEABLE, changeable);
        newProps.put(DocumentAdminModel.Props.READONLY_FIELDS_NAME, null);
        newProps.put(DocumentAdminModel.Props.READONLY_FIELDS_RULE, null);
        nodeService.addProperties(fieldGroupRef, newProps);

        List<String> columns = new ArrayList<String>();
        Serializable systematic = oldProps.get(DocumentAdminModel.Props.SYSTEMATIC);
        columns.add(systematic == null ? "[null]" : systematic.toString());
        Serializable name = oldProps.get(DocumentAdminModel.Props.NAME);
        columns.add(name == null ? "[null]" : name.toString());
        for (Entry<QName, Serializable> entry : newProps.entrySet()) {
            QName key = entry.getKey();
            String oldValue;
            if (!oldProps.containsKey(key)) {
                oldValue = "[doesNotContain]";
            } else if (oldProps.get(key) == null) {
                oldValue = "[null]";
            } else {
                oldValue = oldProps.get(key).toString();
            }
            columns.add(oldValue);
            Serializable newValue = entry.getValue();
            columns.add(newValue == null ? "[null]" : newValue.toString());
        }
        return columns.toArray(new String[columns.size()]);
    }
}
