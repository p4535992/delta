package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * This updater combines three updaters:
 * 1) removes DocumentFileRead permission, adds permissions that are added when series is created
 * 2) Changes value of property series.volType like that:
 * Teemapõhine toimik -> SUBJECT_FILE
 * Aastapõhine toimik -> ANNUAL_FILE
 * Asjatoimik -> CASE_FILE
 * and adds SUBJECT_FILE and ANNUAL_FILE if needed.
 * 3) adds caseFileContainer aspect
 * 
 * @author Ats Uiboupin - removing permission
 * @author Vladimir Drozdik - changing series.volType
 * @author Kaarel Jõgeva - adding caseFileContainer aspect
 */
public class SeriesUpdater extends AbstractNodeUpdater {
    @Deprecated
    public static final String DOCUMENT_FILE_READ = "DocumentFileRead";

    private boolean seriesUpdater1Executed = false;

    private boolean executeInBackground;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        NodeRef previousComponentRef = generalService.getNodeRef("/sys:system-registry/module:modules/module:simdhs/module:components/module:seriesUpdater", new StoreRef(
                "system://system"));
        seriesUpdater1Executed = previousComponentRef != null;

        String query = generateTypeQuery(SeriesModel.Types.SERIES);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef seriesRef) throws Exception {
        List<String> logInfo = new ArrayList<String>();

        if (!seriesUpdater1Executed) {
            updatePermissions(seriesRef, logInfo);
            volumeTypesUpdater(seriesRef, logInfo);
        }
        addCaseFileContainerAspect(seriesRef, logInfo);
        if (nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS) == null) {
            nodeService.setProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS, Boolean.TRUE);
        }

        return logInfo.toArray(new String[logInfo.size()]);
    }

    private void addCaseFileContainerAspect(NodeRef seriesRef, List<String> logInfo) {
        QName aspect = CaseFileModel.Aspects.CASE_FILE_CONTAINER;
        if (nodeService.hasAspect(seriesRef, aspect)) {
            logInfo.add("caseFileContainerAspectExists");
        } else {
            nodeService.addAspect(seriesRef, aspect, null);
            logInfo.add("caseFileContainerAspectAdded");
        }
    }

    private void updatePermissions(NodeRef seriesRef, List<String> logInfo) {
        PermissionService permissionService = getPermissionService();
        List<String> removedAuthorities = removePermission(seriesRef, DOCUMENT_FILE_READ, permissionService);
        logInfo.add(StringUtils.join(removedAuthorities, " "));
        // add permissions that are added when series is created
        getSeriesService().setSeriesDefaultPermissionsOnCreate(seriesRef);
    }

    public static List<String> removePermission(NodeRef nodeRef, String permissionToRemove, PermissionService permissionService) {
        List<String> removedAuthorities = new ArrayList<String>();
        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(nodeRef)) {
            if (permissionToRemove.equals(accessPermission.getPermission()) && accessPermission.isSetDirectly()) {
                String authority = accessPermission.getAuthority();
                permissionService.deletePermission(nodeRef, authority, permissionToRemove);
                removedAuthorities.add(authority);
            }
        }
        return removedAuthorities;
    }

    private void volumeTypesUpdater(NodeRef nodeRef, List<String> logInfo) {
        String logIsChanged = "volumeTypeChanged";
        String logIsAdded = "volumeTypeAdded";
        String newVolumeTypeValue;
        Set<String> newSeriesVolTypes = new HashSet<String>();
        newSeriesVolTypes.add(VolumeType.SUBJECT_FILE.name());
        newSeriesVolTypes.add(VolumeType.ANNUAL_FILE.name());
        @SuppressWarnings("unchecked")
        List<String> oldSeriesVolTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.VOL_TYPE);
        if (oldSeriesVolTypes == null) {
            nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.ANNUAL_FILE.name());
            logInfo.add(logIsAdded);
            logInfo.add(VolumeType.SUBJECT_FILE.name());
            return;
        }
        Iterator<String> iter = oldSeriesVolTypes.iterator();
        while (iter.hasNext()) {
            String volType = iter.next();
            if ("objektipõhine".equals(volType) || "OBJECT".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
            if ("aastapõhine".equals(volType) || "YEAR_BASED".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
            if ("Asjatoimik".equals(volType) || "CASE".equals(volType)) {
                iter.remove();
                newVolumeTypeValue = VolumeType.CASE_FILE.name();
                newSeriesVolTypes.add(newVolumeTypeValue);
                logInfo.add(logIsChanged);
                logInfo.add(newVolumeTypeValue);
            }
        }
        newSeriesVolTypes.addAll(oldSeriesVolTypes);
        nodeService.setProperty(nodeRef, SeriesModel.Props.VOL_TYPE, new ArrayList<String>(newSeriesVolTypes));
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", DOCUMENT_FILE_READ + " removed from auths", "volumeType added/changed", "newVolumeTypeValue" };
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!isEnabled()) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        if (executeInBackground) {
            super.executeUpdaterInBackground();
        } else {
            super.executeUpdater();
        }
    }

    public void setExecuteInBackground(boolean executeInBackground) {
        this.executeInBackground = executeInBackground;
    }

}
