package ee.webmedia.alfresco.document.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Import delivererName value from csv fail for actOfDeliveryAndReceipt doc. type. Used to add data that was erroneously not converted from 2.5 to 3.13 version by
 * ConvertToDynamicDocumentsUpdater. Should be run only in SIM 3.13 environment after 2.5 -> 3.13 conversion. See cl task 215711 for details.
 * 
 * @author Riina Tens
 */
public class ImportDelivererName extends AbstractNodeUpdater {

    private String csvFileName;
    private Map<NodeRef, String> delivererNames;

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() {
        if (csvFileName == null) {
            throw new RuntimeException("Input csv not defined, aborting updater");
        }
        File file = new File(csvFileName);
        if (!file.exists()) {
            throw new RuntimeException("Input csv " + csvFileName + " does not exist, aborting updater");
        }
        BufferedReader reader = null;
        delivererNames = new HashMap<NodeRef, String>();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineCount = 1;
            while ((line = reader.readLine()) != null) {
                int indexOfSeparator = line.indexOf(";");
                NodeRef nodeRef = null;
                if (indexOfSeparator > 0) {
                    nodeRef = new NodeRef(line.substring(0, indexOfSeparator));
                } else {
                    continue;
                }
                int lineLength = line.length();
                String delivererName = null;
                if (lineLength > indexOfSeparator + 1) {
                    delivererName = line.substring(indexOfSeparator + 1, lineLength);
                }
                delivererNames.put(nodeRef, delivererName);
                lineCount++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading input csv " + csvFileName + ", aborting updater", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // no action
                }
            }
        }
        return delivererNames.keySet();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String delivererName = delivererNames.get(nodeRef);
        if (StringUtils.isBlank(delivererName)) {
            return new String[] { "imported deliverer is blank, not updating" };
        }
        nodeService.setProperty(nodeRef, DocumentSpecificModel.Props.DELIVERER_NAME, delivererName);
        return new String[] { "Updated: " + delivererName };
    }

    public void setCsvFileName(String csvFileName) {
        this.csvFileName = csvFileName;
    }

}
