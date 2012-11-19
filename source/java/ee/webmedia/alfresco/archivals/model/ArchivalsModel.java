package ee.webmedia.alfresco.archivals.model;

import ee.webmedia.alfresco.functions.model.FunctionsModel;

public interface ArchivalsModel {

    public interface Repo {
        String ARCHIVALS_TEMP_PARENT = "/";
        String ARCHIVALS_TEMP_ROOT = "archivalsTemp";
        String ARCHIVALS_TEMP_SPACE = ARCHIVALS_TEMP_PARENT + FunctionsModel.NAMESPACE_PREFFIX + ARCHIVALS_TEMP_ROOT;
    }

}
