package ee.webmedia.alfresco.archivals.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.functions.model.FunctionsModel;

public interface ArchivalsModel {
    String URI = "http://alfresco.webmedia.ee/model/archivals/1.0";
    String PREFIX = "archivals:";

    public interface Repo {
        String ARCHIVALS_TEMP_PARENT = "/";
        String ARCHIVALS_TEMP_ROOT = "archivalsTemp";
        String ARCHIVALS_TEMP_SPACE = ARCHIVALS_TEMP_PARENT + FunctionsModel.NAMESPACE_PREFFIX + ARCHIVALS_TEMP_ROOT;
        String ARCHIVALS_SPACE = ARCHIVALS_TEMP_PARENT + PREFIX + "archivalsQueue";
    }

    public interface Types {
        QName ARCHIVALS_QUEUE_ROOT = QName.createQName(URI, "archivalsQueue");
        QName ARCHIVING_JOB = QName.createQName(URI, "archivingJob");
    }

    public interface Assocs {
        QName ARCHIVALS_QUEUE = QName.createQName(URI, "archivalsQueue");
        QName ARCHIVING_JOB = QName.createQName(URI, "archivingJob");
    }

    public interface Props {
        QName USERNAME = QName.createQName(URI, "userName");
        QName ARCHIVING_START_TIME = QName.createQName(URI, "archivingStartTime");
        QName ARCHIVING_END_TIME = QName.createQName(URI, "archivingEndTime");
        QName VOLUME_REF = QName.createQName(URI, "volumeRef");
        QName ARCHIVING_JOB_STATUS = QName.createQName(URI, "archivingJobStatus");
        QName ARCHIVE_NOTE = QName.createQName(URI, "archiveNote");
        QName ERROR_MESSAGE = QName.createQName(URI, "errorMessage");
    }

}
