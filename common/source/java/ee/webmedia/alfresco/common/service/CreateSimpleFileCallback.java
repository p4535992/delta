package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.file.model.SimpleFile;

public interface CreateSimpleFileCallback {

    SimpleFile create(Map<QName, Serializable> fileProp, Serializable... objects);

}
