package org.alfresco.service.cmr.view;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class ExcludingExporterCrawlerParameters extends ExporterCrawlerParameters {

    private List<NodeRef> excludeNodeRefs = new ArrayList<NodeRef>();
    private List<QName> excludedProperties = new ArrayList<QName>();
    private List<QName> excludedAssocTypes = new ArrayList<QName>();

    public void setExcludeNodeRefs(List<NodeRef> excludeNodeRefs) {
        this.excludeNodeRefs = excludeNodeRefs;
    }

    public List<NodeRef> getExcludeNodeRefs() {
        return excludeNodeRefs;
    }

    public void setExcludedProperties(List<QName> excludedProperties) {
        this.excludedProperties = excludedProperties;
    }

    public List<QName> getExcludedProperties() {
        return excludedProperties;
    }

    public void setExcludedAssocTypes(List<QName> excludedAssocTypes) {
        this.excludedAssocTypes = excludedAssocTypes;
    }

    public List<QName> getExcludedAssocTypes() {
        return excludedAssocTypes;
    }
}
