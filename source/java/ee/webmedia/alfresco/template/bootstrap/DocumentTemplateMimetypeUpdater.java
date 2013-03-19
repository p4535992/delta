package ee.webmedia.alfresco.template.bootstrap;

import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.mso.service.MsoService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Updates document templates mimetypes.
 * 
 * @author Kaarel Jõgeva
 */
public class DocumentTemplateMimetypeUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        String query = SearchUtil.generateAspectQuery(DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);

        String templateName = (String) origProps.get(DocumentTemplateModel.Prop.NAME);
        String mimetype = null;
        if (endsWithIgnoreCase(templateName, ".dotx")) {
            mimetype = MsoService.MIMETYPE_DOTX;
        } else if (endsWithIgnoreCase(templateName, ".dot")) {
            mimetype = MsoService.MIMETYPE_DOT;
        } else {
            return new String[] { "contentDataNotUpdateable", templateName };
        }

        ContentData data = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (data == null) {
            return new String[] { "contentDataMissing", templateName };
        } else if (mimetype.equals(data.getMimetype())) {
            return new String[] { "correctMimetype", templateName };
        }

        // Update properties
        newProps.put(ContentModel.PROP_CONTENT, ContentData.setMimetype(data, mimetype));
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);

        return new String[] { "mimetypeUpdated", data.getMimetype(), mimetype };
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }
}
