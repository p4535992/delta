package ee.webmedia.alfresco.docadmin.service;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;

/**
 * Used to pass metaData items (fields, fieldGroups and separator) to FieldListDialog and {@link FieldDetailsDialog}
 * 
 * @author Ats Uiboupin
 */
public interface MetadataContainer {
    ChildrenList<? extends MetadataItem> getMetadata();
}
