package ee.webmedia.alfresco.permfix;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class SeriesStructUnitUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SeriesStructUnitUpdater.class);

    private String storeString = "workspace://SpacesStore";
    private String dataFolder;
    private String mappingFileName = "structUnitMapping.csv";
    private Map<String, String> structUnitMap = new HashMap<>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Assert.notNull(storeString, "Store must be provided");
        storeString = StringUtils.trim(storeString);
        StoreRef storeRef = new StoreRef(storeString);
        List<StoreRef> allRefs = nodeService.getStores();
        if (!allRefs.contains(storeRef)) {
            throw new UnableToPerformException("User entered unknown storeRef: " + storeString);
        }
        
        List<String> queryParts = new ArrayList<>();
        queryParts.add("(" + generateTypeQuery(SeriesModel.Types.SERIES) + ")");
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(SeriesModel.Props.STRUCT_UNIT));
        String query = SearchUtil.joinQueryPartsAnd(queryParts, false);

        
        return Arrays.asList(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef seriesRef) throws Exception {
    	List<String> structUnitsOld = (List<String>) nodeService.getProperty(seriesRef, SeriesModel.Props.STRUCT_UNIT);
    	ArrayList<String> structUnitsNew = new ArrayList<>();
    	for (String oldUnit: structUnitsOld) {
    		String newUnit = structUnitMap.get(oldUnit);
    		if (newUnit != null && !newUnit.equals("-")) {
    			structUnitsNew.add(newUnit);
    		}
    	}
    	
    	nodeService.setProperty(seriesRef, SeriesModel.Props.STRUCT_UNIT, structUnitsNew);
    	
        return new String[] { "updated struct unit for serie: " + seriesRef };
    }

    @Override
    protected void executeUpdater() throws Exception {
    	fillStructUnits();
        super.executeUpdater();
        resetFields();
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }
    
    private void fillStructUnits() throws Exception {
    	File file = new File(dataFolder + mappingFileName);
    	if (!file.exists()) {
            throw new UnableToPerformException("File does not exist:: " + file.getAbsolutePath());
        }

        log.info("Loading structUnit mapping from file " + file.getAbsolutePath());

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(file)), CSV_SEPARATOR, CSV_CHARSET);
        try {
            while (reader.readRecord()) {
                structUnitMap.put(reader.get(0), reader.get(1));
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + structUnitMap.size() + " structUnits from file " + file.getAbsolutePath());
    }

    @Override
    protected Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
        return null;
    }

    private void resetFields() {
        storeString = "workspace://SpacesStore";
        mappingFileName = "structUnitMapping.csv";
        dataFolder = null;
        structUnitMap = new HashMap<>();
    }

    public String getStoreString() {
        return storeString;
    }

    public void setStoreString(String storeString) {
        this.storeString = storeString;
    }

	public String getDataFolder() {
		return dataFolder;
	}

	public void setDataFolder(String dataFolder) {
		this.dataFolder = dataFolder;
	}

	public String getMappingFileName() {
		return mappingFileName;
	}

	public void setMappingFileName(String mappingFileName) {
		this.mappingFileName = mappingFileName;
	}

    
}
