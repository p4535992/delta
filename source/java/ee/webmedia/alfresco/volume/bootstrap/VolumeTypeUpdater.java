package ee.webmedia.alfresco.volume.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Combined volumes updater, that does two things
 * 1)
 * Changes volume type value in repository.
 * Teemapõhine toimik -> SUBJECT_FILE
 * Aastapõhine toimik -> ANNUAL_FILE
 * Asjatoimik -> CASE_FILE
 * (CL task 177957)
 * 2) changes namespaces of the volume properties
 */
public class VolumeTypeUpdater extends AbstractNodeUpdater {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(VolumeTypeUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = generateTypeQuery(VolumeModel.Types.VOLUME);
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        String newVolumeTypeValue = null;
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        Set<Entry<QName, Serializable>> entrySet = props.entrySet();
        Map<QName, Serializable> newNamespaceProps = new HashMap<QName, Serializable>();
        List<String> propNamesOfChangedNS = new ArrayList<String>(newNamespaceProps.size());
        for (Iterator<Entry<QName, Serializable>> it = entrySet.iterator(); it.hasNext();) {
            Entry<QName, Serializable> entry = it.next();
            QName propName = entry.getKey();
            if (VolumeModel.VOLUME_MODEL_URI.equals(propName.getNamespaceURI())) {
                Serializable value = entry.getValue();
                String propLoclaName = propName.getLocalName();
                newNamespaceProps.put(QName.createQName(VolumeModel.URI, propLoclaName), value);
                it.remove();
                propNamesOfChangedNS.add(propLoclaName);
            }
        }
        props.putAll(newNamespaceProps);

        String volType = (String) props.get(VolumeModel.Props.VOLUME_TYPE);
        if (volType.equals("objektipõhine") || volType.equals("OBJECT")) {
            newVolumeTypeValue = VolumeType.SUBJECT_FILE.name();
            props.put(VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        } else if (volType.equals("aastapõhine") || volType.equals("YEAR_BASED")) {
            newVolumeTypeValue = VolumeType.ANNUAL_FILE.name();
            props.put(VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        } else if (volType.equals("Asjatoimik") || volType.equals("CASE")) {
            newVolumeTypeValue = VolumeType.CASE_FILE.name();
            props.put(VolumeModel.Props.VOLUME_TYPE, newVolumeTypeValue);
        }
        nodeService.setProperties(nodeRef, props);
        if (newVolumeTypeValue == null) {
            newVolumeTypeValue = "NOT CHANGED";
        }
        String propNamesOfChangedNSText = StringUtils.join(propNamesOfChangedNS, " ");
        if (propNamesOfChangedNS.isEmpty()) {
            String msg = "volume didn't have any properties with old namespace";
            LOG.warn(msg + ". volRef=" + nodeRef);
            propNamesOfChangedNSText = "WARNING: " + msg;
        }
        return new String[] { volType, newVolumeTypeValue, propNamesOfChangedNSText };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "volume.nodeRef", "old volumeType", "changed volumeType", "localNames of updated properties (namespace changed from vol to docdyn)" };
    }

}
