package ee.webmedia.alfresco.archivals.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.functions.model.FunctionsModel;

public interface ArchivalsModel {
    String URI = "http://alfresco.webmedia.ee/model/archivals/1.0";
<<<<<<< HEAD
    String PREFIX = "arch:";
=======
    String PREFIX = "archivals:";
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public interface Repo {
        String ARCHIVALS_TEMP_PARENT = "/";
        String ARCHIVALS_TEMP_ROOT = "archivalsTemp";
        String ARCHIVALS_TEMP_SPACE = ARCHIVALS_TEMP_PARENT + FunctionsModel.NAMESPACE_PREFFIX + ARCHIVALS_TEMP_ROOT;
<<<<<<< HEAD
        String ARCHIVAL_ACTIVITIES_SPACE = ARCHIVALS_TEMP_PARENT + PREFIX + "archivalActivities";
    }

    public interface Types {
        QName ARCHIVAL_ACTIVITIES = QName.createQName(URI, "archivalActivities");
        QName ARCHIVAL_ACTIVITY = QName.createQName(URI, "archivalActivity");
        QName ARCHIVAL_ACTIVITY_SEARCH_FILTER = QName.createQName(URI, "archivalActivitySearchFilter");
    }

    public interface Assocs {
        QName ARCHIVAL_ACTIVITY = QName.createQName(URI, "archivalActivity");
        QName ARCHIVAL_ACTIVITY_DOCUMENT = QName.createQName(URI, "archivalActivityDocument");
    }

    public interface Aspects {
        QName ARCHIVAL_ACTIVITY_CONTAINER = QName.createQName(URI, "archivalActivityContainer");
    }

    public interface Props {
        QName ACTIVITY_TYPE = QName.createQName(URI, "activityType");
        QName CREATED = QName.createQName(URI, "created");
        QName CREATOR_ID = QName.createQName(URI, "creatorId");
        QName CREATOR_NAME = QName.createQName(URI, "creatorName");
        QName STATUS = QName.createQName(URI, "status");

        QName FILTER_CREATED = QName.createQName(URI, "filterCreated");
        QName FILTER_CREATED_END_DATE = QName.createQName(URI, "filterCreated_EndDate");
        QName FILTER_ACTIVITY_TYPE = QName.createQName(URI, "filterActivityType");
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

}
