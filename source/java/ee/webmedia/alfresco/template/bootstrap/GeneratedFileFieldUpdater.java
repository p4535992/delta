<<<<<<< HEAD
package ee.webmedia.alfresco.template.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * CL 163201: remove generated field and set generatedFromTemplate field
 * 
 * @author Kaarel Jõgeva
 */
public class GeneratedFileFieldUpdater extends AbstractNodeUpdater {

    private final QName generatedProp = QName.createQName(FileModel.URI, "generated");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generatePropertyBooleanQuery(generatedProp, true);

        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);

        newProps.put(FileModel.Props.GENERATED_FROM_TEMPLATE, "true");
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);
        nodeService.removeProperty(nodeRef, generatedProp);
        return new String[] { "removeGeneratedField", "addGeneratedFromTemplateField" };
    }
}
=======
package ee.webmedia.alfresco.template.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * CL 163201: remove generated field and set generatedFromTemplate field
 */
public class GeneratedFileFieldUpdater extends AbstractNodeUpdater {

    private final QName generatedProp = QName.createQName(FileModel.URI, "generated");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generatePropertyBooleanQuery(generatedProp, true);

        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);

        newProps.put(FileModel.Props.GENERATED_FROM_TEMPLATE, "true");
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);
        nodeService.removeProperty(nodeRef, generatedProp);
        return new String[] { "removeGeneratedField", "addGeneratedFromTemplateField" };
    }
}
>>>>>>> develop-5.1
