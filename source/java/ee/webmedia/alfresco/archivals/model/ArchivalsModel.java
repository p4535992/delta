package ee.webmedia.alfresco.archivals.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.functions.model.FunctionsModel;

public interface ArchivalsModel {
    String URI = "http://alfresco.webmedia.ee/model/archivals/1.0";
    String PREFIX = "arch:";

    public interface Repo {
        String ARCHIVALS_TEMP_PARENT = "/";
        String ARCHIVALS_TEMP_ROOT = "archivalsTemp";
        String ARCHIVALS_TEMP_SPACE = ARCHIVALS_TEMP_PARENT + FunctionsModel.NAMESPACE_PREFFIX + ARCHIVALS_TEMP_ROOT;
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
    }

}
