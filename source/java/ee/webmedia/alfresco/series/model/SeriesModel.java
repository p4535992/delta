package ee.webmedia.alfresco.series.model;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public interface SeriesModel {
    String URI = "http://alfresco.webmedia.ee/model/series/1.0";

    interface Types {
        QName SERIES = QName.createQName(URI, "series");
    }

    interface Associations {
        QName SERIES = QName.createQName(URI, "series");
    }

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName STATUS = QName.createQName(URI, "status");
        QName ORDER = QName.createQName(URI, "order");
        QName SERIES_IDENTIFIER = QName.createQName(URI, "seriesIdentifier");
        QName TITLE = QName.createQName(URI, "title");
        QName DESCRIPTION = QName.createQName(URI, "description");
        QName REGISTER = QName.createQName(URI, "register");
        QName INDIVIDUALIZING_NUMBERS = QName.createQName(URI, "individualizingNumbers");
        QName STRUCT_UNIT = QName.createQName(URI, "structUnit");
        QName RELATED_USERS_GROUPS = QName.createQName(URI, "relatedUsersGroups");
        QName CONTAINING_DOCS_COUNT = QName.createQName(URI, "containingDocsCount");
        QName TYPE = QName.createQName(URI, "type");
        QName DOC_TYPE = QName.createQName(URI, "docType");
        QName DOC_NUMBER_PATTERN = QName.createQName(URI, "docNumberPattern");
        QName NEW_NUMBER_FOR_EVERY_DOC = QName.createQName(URI, "newNumberForEveryDoc");
        QName VALID_FROM_DATE = QName.createQName(URI, "validFromDate");
        QName VALID_TO_DATE = QName.createQName(URI, "validToDate");
        QName VOL_TYPE = QName.createQName(URI, "volType");
        QName VOL_REGISTER = QName.createQName(URI, "volRegister");
        QName VOL_NUMBER_PATTERN = QName.createQName(URI, "volNumberPattern");
        QName DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS = QName.createQName(URI, "documentsVisibleForUsersWithoutAccess");
        QName EVENT_PLAN = QName.createQName(URI, "eventPlan");

        QName ACCESS_RESTRICTION = QName.createQName(DocumentCommonModel.DOCCOM_URI, "accessRestriction");
        QName ACCESS_RESTRICTION_REASON = QName.createQName(DocumentCommonModel.DOCCOM_URI, "accessRestrictionReason");
        QName ACCESS_RESTRICTION_BEGIN_DATE = QName.createQName(DocumentCommonModel.DOCCOM_URI, "accessRestrictionBeginDate");
        QName ACCESS_RESTRICTION_END_DATE = QName.createQName(DocumentCommonModel.DOCCOM_URI, "accessRestrictionEndDate");
        QName ACCESS_RESTRICTION_END_DESC = QName.createQName(DocumentCommonModel.DOCCOM_URI, "accessRestrictionEndDesc");
    }
}
