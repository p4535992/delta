<<<<<<< HEAD
package ee.webmedia.alfresco.template.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Vladimir Drozdik
 *         Add to templates templateType property and value. Changes systemTemplate aspect to notificationTemplate.
 */
public class DocumentTemplateTypeUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        String query = SearchUtil.generateAspectQuery(DocumentTemplateModel.Aspects.TEMPLATE);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String templateTypeName = "";
        boolean aspectUpdated = false;
        boolean docTemplateAspectRemoved = false;
        String URI = "http://alfresco.webmedia.ee/model/document-template/1.0";
        QName sysAspectQName = QName.createQName(URI, "systemTemplate");
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        if (aspects.contains(DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)
                && (aspects.contains(DocumentTemplateModel.Aspects.TEMPLATE_EMAIL) || aspects.contains(sysAspectQName))) {
            nodeService.removeProperty(nodeRef, DocumentTemplateModel.Prop.DOCTYPE_ID);
            nodeService.removeAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT);
            docTemplateAspectRemoved = true;
        }
        if (nodeService.hasAspect(nodeRef, sysAspectQName)) {
            nodeService.removeAspect(nodeRef, sysAspectQName);
            nodeService.addAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION, null);
            aspectUpdated = true;
            templateTypeName = TemplateType.NOTIFICATION_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        } else if (nodeService.hasAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
            templateTypeName = TemplateType.DOCUMENT_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        } else if (nodeService.hasAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
            templateTypeName = TemplateType.EMAIL_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        }
        return new String[] { "updateTemplateType", templateTypeName, aspectUpdated ? "aspectFromSystemToNotificationUpdated" : "",
                docTemplateAspectRemoved ? "documentTemplateAspectRemoved" : "" };
    }
}
=======
package ee.webmedia.alfresco.template.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 *         Add to templates templateType property and value. Changes systemTemplate aspect to notificationTemplate.
 */
public class DocumentTemplateTypeUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        String query = SearchUtil.generateAspectQuery(DocumentTemplateModel.Aspects.TEMPLATE);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String templateTypeName = "";
        boolean aspectUpdated = false;
        boolean docTemplateAspectRemoved = false;
        String URI = "http://alfresco.webmedia.ee/model/document-template/1.0";
        QName sysAspectQName = QName.createQName(URI, "systemTemplate");
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        if (aspects.contains(DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)
                && (aspects.contains(DocumentTemplateModel.Aspects.TEMPLATE_EMAIL) || aspects.contains(sysAspectQName))) {
            nodeService.removeProperty(nodeRef, DocumentTemplateModel.Prop.DOCTYPE_ID);
            nodeService.removeAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT);
            docTemplateAspectRemoved = true;
        }
        if (nodeService.hasAspect(nodeRef, sysAspectQName)) {
            nodeService.removeAspect(nodeRef, sysAspectQName);
            nodeService.addAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION, null);
            aspectUpdated = true;
            templateTypeName = TemplateType.NOTIFICATION_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        } else if (nodeService.hasAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT)) {
            templateTypeName = TemplateType.DOCUMENT_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        } else if (nodeService.hasAspect(nodeRef, DocumentTemplateModel.Aspects.TEMPLATE_EMAIL)) {
            templateTypeName = TemplateType.EMAIL_TEMPLATE.name();
            properties.put(DocumentTemplateModel.Prop.TEMPLATE_TYPE, templateTypeName);
            nodeService.setProperties(nodeRef, properties);
        }
        return new String[] { "updateTemplateType", templateTypeName, aspectUpdated ? "aspectFromSystemToNotificationUpdated" : "",
                docTemplateAspectRemoved ? "documentTemplateAspectRemoved" : "" };
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
