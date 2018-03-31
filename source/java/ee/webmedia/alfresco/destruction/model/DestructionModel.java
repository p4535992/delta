package ee.webmedia.alfresco.destruction.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.functions.model.FunctionsModel;

public interface DestructionModel {
    String URI = "http://alfresco.webmedia.ee/model/archivals/1.0";
    String PREFIX = "arch:";

    public interface Repo {
        String DESTRUCTIONS_TEMP_PARENT = "/";
        String DESTRUCTIONS_TEMP_ROOT = "destructionsTemp";
        String DESTRUCTIONS_TEMP_SPACE = DESTRUCTIONS_TEMP_PARENT + FunctionsModel.NAMESPACE_PREFFIX + DESTRUCTIONS_TEMP_ROOT;
        String ARCHIVAL_ACTIVITIES_SPACE = DESTRUCTIONS_TEMP_PARENT + PREFIX + "destructionActivities";
        String DESTRUCTIONS_SPACE = DESTRUCTIONS_TEMP_PARENT + PREFIX + "destructionsQueue";
    }

    public interface Types {
        QName ARCHIVAL_ACTIVITIES = QName.createQName(URI, "destructionActivities");
        QName ARCHIVAL_ACTIVITY = QName.createQName(URI, "destructionActivity");
        QName ARCHIVAL_ACTIVITY_SEARCH_FILTER = QName.createQName(URI, "destructionActivitySearchFilter");
        QName DESTRUCTIONS_QUEUE_ROOT = QName.createQName(URI, "destructionsQueue");
        QName DESTRUCTION_JOB = QName.createQName(URI, "destructingJob");
    }

    public interface Assocs {
        QName ARCHIVAL_ACTIVITY = QName.createQName(URI, "destructionActivity");
        QName ARCHIVAL_ACTIVITY_DOCUMENT = QName.createQName(URI, "destructionActivityDocument");
        QName DESTRUCTIONS_QUEUE = QName.createQName(URI, "destructionsQueue");
        QName DESTRUCTING_JOB = QName.createQName(URI, "destructingJob");
        QName ACTIVITY_LINKED_JOBS = QName.createQName(URI, "ActivityLinkedJobs");
    }

    public interface Aspects {
        QName ARCHIVAL_ACTIVITY_CONTAINER = QName.createQName(URI, "destructionActivityContainer");
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

        QName USERNAME = QName.createQName(URI, "userName");
        QName DESTRUCTING_START_TIME = QName.createQName(URI, "destructingStartTime");
        QName DESTRUCTING_END_TIME = QName.createQName(URI, "destructingEndTime");
        QName VOLUME_REF = QName.createQName(URI, "volumeRef");
        QName DESTRUCING_JOB_STATUS = QName.createQName(URI, "destructingJobStatus");
        QName DESTRUCTION_NOTE = QName.createQName(URI, "destructionNote");
        QName ERROR_MESSAGE = QName.createQName(URI, "errorMessage");
        QName FAILED_NODE_COUNT = QName.createQName(URI, "failedNodeCount");
        QName FAILED_DOCUMENTS_COUNT = QName.createQName(URI, "failedDocumentsCount");
        QName TOTAL_DESCTRUCTED_DOCUMENTS_COUNT = QName.createQName(URI, "totalDestructdDocumentsCount");
        QName DESCTRUCTED_NODE_COUNT = QName.createQName(URI, "destructedNodeCount");
        QName DESCTRUCTING_ACTIVITY_REF = QName.createQName(URI, "destructingActivityRef");
        QName DESTRUCTION_PAUSED = QName.createQName(URI, "destructionPaused");
        
    }

}
