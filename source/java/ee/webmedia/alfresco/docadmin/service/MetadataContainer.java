<<<<<<< HEAD
package ee.webmedia.alfresco.docadmin.service;

import java.util.Collection;
import java.util.Set;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;

/**
 * Used to pass metaData items (fields, fieldGroups and separator) to FieldListDialog and {@link FieldDetailsDialog}
 * 
 * @author Ats Uiboupin
 */
public interface MetadataContainer {
    ChildrenList<? extends MetadataItem> getMetadata();

    Collection<Field> getFieldsById(Set<String> fieldIdLocalNames);
}
=======
package ee.webmedia.alfresco.docadmin.service;

import java.util.Collection;
import java.util.Set;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;

/**
 * Used to pass metaData items (fields, fieldGroups and separator) to FieldListDialog and {@link FieldDetailsDialog}
 */
public interface MetadataContainer {
    ChildrenList<? extends MetadataItem> getMetadata();

    Collection<Field> getFieldsById(Set<String> fieldIdLocalNames);
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
