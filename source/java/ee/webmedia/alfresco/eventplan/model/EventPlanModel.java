package ee.webmedia.alfresco.eventplan.model;

import org.alfresco.service.namespace.QName;

/**
 * EventPlan model constants.
 */
public interface EventPlanModel {

    String URI = "http://alfresco.webmedia.ee/model/eventPlan/1.0";
    String PREFIX = "plan";
    String PATH = "/plan:eventPlans";

    interface Types {
        QName EVENT_PLAN = QName.createQName(URI, "eventPlan");
    }

    interface Aspects {
        QName VOLUME_EVENT_PLAN = QName.createQName(URI, "volumeEventPlan");
    }

    interface Assocs {
        QName EVENT_PLANS = QName.createQName(URI, "eventPlans");
    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName IS_APPRAISED = QName.createQName(URI, "isAppraised");
        QName HAS_ARCHIVAL_VALUE = QName.createQName(URI, "hasArchivalValue");
        QName RETAIN_PERMANENT = QName.createQName(URI, "retainPermanent");
        QName RETAINTION_START = QName.createQName(URI, "retaintionStart");
        QName RETAINTION_PERIOD = QName.createQName(URI, "retaintionPeriod");
        QName RETAIN_UNTIL_DATE = QName.createQName(URI, "retainUntilDate");
        QName FIRST_EVENT = QName.createQName(URI, "firstEvent");
        QName FIRST_EVENT_START = QName.createQName(URI, "firstEventStart");
        QName FIRST_EVENT_PERIOD = QName.createQName(URI, "firstEventPeriod");
        QName ARCHIVING_NOTE = QName.createQName(URI, "archivingNote");

        QName EVENT_PLAN = QName.createQName(URI, "eventPlan");
        QName NEXT_EVENT = QName.createQName(URI, "nextEvent");
        QName NEXT_EVENT_DATE = QName.createQName(URI, "nextEventDate");
        QName MARKED_FOR_TRANSFER = QName.createQName(URI, "markedForTransfer");
        QName EXPORTED_FOR_UAM = QName.createQName(URI, "exportedForUam");
        QName EXPORTED_FOR_UAM_DATE_TIME = QName.createQName(URI, "exportedForUamDateTime");
        QName TRANSFER_CONFIRMED = QName.createQName(URI, "transferConfirmed");
        QName TRANSFERED_DATE_TIME = QName.createQName(URI, "transferedDateTime");
        QName MARKED_FOR_DESTRUCTION = QName.createQName(URI, "markedForDestruction");
        QName DISPOSAL_ACT_CREATED = QName.createQName(URI, "disposalActCreated");
        QName DISPOSAL_DATE_TIME = QName.createQName(URI, "disposalDateTime");
    }
}
