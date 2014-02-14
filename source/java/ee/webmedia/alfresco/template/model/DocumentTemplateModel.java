package ee.webmedia.alfresco.template.model;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * @author Kaarel Jõgeva
 */
public interface DocumentTemplateModel {

    String URI = "http://alfresco.webmedia.ee/model/document-template/1.0";
    String NAMESPACE_PREFIX = "docTempl:";

    interface Repo {
        final static String TEMPLATES_PARENT = "/";
        final static String TEMPLATES_SPACE = TEMPLATES_PARENT + QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "templates");
    }

    interface Types {
        QName TEMPLATES_ROOT = ContentModel.TYPE_FOLDER;
    }

    interface Prop {
        QName NAME = QName.createQName(URI, "name");
        QName COMMENT = QName.createQName(URI, "comment");
        QName DOCTYPE_ID = QName.createQName(URI, "docTypeId");
        QName TEMPLATE_TYPE = QName.createQName(URI, "templateType");
        QName REPORT_OUTPUT_TYPE = QName.createQName(URI, "reportOutputType");
        QName REPORT_TYPE = QName.createQName(URI, "reportType");
        QName NOTIFICATION_SUBJECT = QName.createQName(URI, "notificationSubject");
    }

    interface Assoc {
        QName TEMPLATE = QName.createQName(URI, "template");
    }

    interface Aspects {
        QName TEMPLATE = QName.createQName(URI, "template");
        QName SUBJECT = QName.createQName(URI, "subject");
        QName TEMPLATE_NOTIFICATION = QName.createQName(URI, "notificationTemplate");
        QName TEMPLATE_EMAIL = QName.createQName(URI, "emailTemplate");
        QName TEMPLATE_DOCUMENT = QName.createQName(URI, "documentTemplate");
        QName TEMPLATE_REPORT = QName.createQName(URI, "reportTemplate");
        QName TEMPLATE_ARCHIVAL_REPORT = QName.createQName(URI, "archivalReportTemplate");
    }

}