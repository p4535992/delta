package ee.webmedia.alfresco.document.permissions;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class DocumentListWriteDynamicAuthority extends DocumentListCreateDynamicAuthority {

    public static final String DOCUMENT_LIST_WRITE_AUTHORITY = "ROLE_DOCUMENT_LIST_WRITE";

    public DocumentListWriteDynamicAuthority() {
        types.add(FunctionsModel.Types.FUNCTION);
        types.add(SeriesModel.Types.SERIES);
        types.add(VolumeModel.Types.VOLUME);
        types.add(CaseModel.Types.CASE);
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_LIST_WRITE_AUTHORITY;
    }

}
