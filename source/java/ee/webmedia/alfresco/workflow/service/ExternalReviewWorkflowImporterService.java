package ee.webmedia.alfresco.workflow.service;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;

import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;
import org.apache.xmlbeans.XmlObject;

public interface ExternalReviewWorkflowImporterService {

    <F extends XmlObject> NodeRef importWorkflowDocument(Reader viewReader, Location location,
            NodeRef existingDocumentRef, List<F> dataFiles, String dvkId, Map<QName, Task> notifications);

}