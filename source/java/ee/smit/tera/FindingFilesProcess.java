package ee.smit.tera;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDigiSignService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getTeraService;

public class FindingFilesProcess {
    private static final Log log = LogFactory.getLog(FindingFilesProcess.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private NodeRef rootNodeRef;

    public FindingFilesProcess() {
        log.info("Start finding files...");


        log.debug("GET NODE SERVICE....");
        this.nodeService = BeanHelper.getNodeService();

        log.debug("GET FILE-FOLDER SERVICE...");
        this.fileFolderService = BeanHelper.getFileFolderService();

        findAllFilesBySQL();

        log.info("Files in TERA TABLE AFTER FIND... " + getTeraService().countAllFiles());

    }

    private void findAllFilesBySQL() {
        log.info("FIND FILES FROM DELTA DB By SQL.....");

        final List<String> filesProcessed = new ArrayList<>();
        final List<Callable<String>> tasks = new ArrayList<>();

        List<Map<String, Object>> rows = getTeraService().findAllDigidocfiles();

        final int findRows = rows.size();
        log.info("FILE NODE_REFs found: " + findRows);

        int threadCount = Math.min(
                Runtime.getRuntime().availableProcessors(),
                findRows
        );

        if (getDigiSignService().getMaxThreads() > 0) {
            threadCount = Math.min(getDigiSignService().getMaxThreads(), findRows);
        }

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            final int[] i = {0};
            for (final Map<String, Object> row : rows) {
                tasks.add(
                        new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                i[0]++;
                                return parseRow(row, i, findRows);
                            }
                        }
                );
            }

            for (final Future<String> f : executor.invokeAll(tasks/*, timeout? */)) {
                try {
                    // If you added a timeout, you can check f.isCancelled().
                    // If timed out, get() will throw a CancellationException.
                    if (f != null) {
                        filesProcessed.add(f.get());
                    } else {

                    }
                } catch (final ExecutionException e) {
                    // This request failed.  Handle it however you like.
                    log.error(e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
        /*
        if(rows != null){
            int i = 0;
            for(Map<String, Object> row : rows){
                i++;
                String node_ref = (String)row.get("node_ref");
                log.trace(i + "/" + findRows + ") NODE_REF: " + node_ref);
                checkAndAddTeraFiles(node_ref, "", "", "");
            }
        }
        */
    }

    private String parseRow(Map<String, Object> row, int[] i, int findRows) {
        String node_ref = (String) row.get("node_ref");
        String fileName = (String) row.get("filename");
        String fileExt = row.get("ext") != null ? ((String) row.get("ext")).toUpperCase() : "";

        if (fileName != null && !fileName.isEmpty()) {
            log.trace("FILENAME: " + fileName);
            fileName = getTeraService().checkFileNameSymbols(fileName);
            log.trace("FILENAME: AFTER CHECK: " + fileName);
        }

        log.trace(i[0] + "/" + findRows + ") NODE_REF: " + node_ref + ", FILENAME: " + fileName);
        checkAndAddTeraFiles(node_ref, fileName, fileExt, "");
        return node_ref;

    }

    private void importNodeRefsFromFile(String filename) {
        BufferedReader reader = null;

        try {
            File file = new File(filename);
            reader = new BufferedReader(new FileReader(file));

            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("node_ref: " + line);
                checkAndAddTeraFiles(line, "", "", "");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void findingAllFiles() {
        log.debug("getListOfDDOCFiles()... START!");
        getListOfDDOCFiles();
        log.debug("getListOfDDOCFiles()... END!");
    }

    private void getListOfDDOCFiles() {
        if (nodeService == null) {
            log.error("NODE SERVICE IS NULL! return ...");
            return;
        }

        List<String> namePattern = Arrays.asList("*.ddoc", "*.bdoc");

        log.debug("Filename pattern: " + Arrays.toString(namePattern.toArray()));

        try {
            //setUp();
            List<StoreRef> storeRefsList = nodeService.getStores();

            if (storeRefsList == null) {
                log.error("Store REF list is NULL! Break process!");
                return;
            } else {
                log.debug("StoreRefs to process... " + storeRefsList.size());
                for (StoreRef storeRef : storeRefsList) {
                    try {
                        log.info("StoreRef.getProtocol: " + storeRef.getProtocol());
                        log.info("StoreRef.getIdentifier: " + storeRef.getIdentifier());
                        if (storeRef.getProtocol().equals("workspace") || storeRef.getProtocol().equals("archive")) {
                            rootNodeRef = nodeService.getRootNode(storeRef);
                            log.info("NodeService.getRootNode: " + rootNodeRef);

                            findAllFiles(rootNodeRef, namePattern);

                        } else {
                            log.info("Not allowed! Not permitted! Get next store");
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    log.debug("Get next storeRef from list...");
                }
            }
        } catch (Exception ex) {
            log.error("getAllDDocFiles ERROR: " + ex.getMessage(), ex);
        }
        log.debug("Process getListOfDDOCFiles()... END!");
    }

    void findAllFiles(NodeRef rootNodeRef, List<String> namePatternList) {

        for (String namePattern : namePatternList) {
            log.debug("FILES NAME PATTERN to SEARCH: " + namePattern);
            try {
                List<FileInfo> fileIn = fileFolderService.search(rootNodeRef, namePattern, true);
                if (fileIn == null) {
                    log.warn("Find all files: fileForderService.search returned NULL!");
                } else {
                    log.debug("Find files: " + fileIn.size());
                    log.debug("============================================================================================");
                    for (FileInfo fileInfo : fileIn) {
                        try {
                            String fileName = fileInfo.getName();
                            if (fileName != null && !fileName.isEmpty()) {
                                fileName = getTeraService().checkFileNameSymbols(fileName);
                            }


                            if (fileInfo.isFolder()) {
                                log.warn("DIRECTORY: Name: [" + fileName + "] is FOLDER! continue to next...");
                                continue;
                            }

                            NodeRef fileNodeRef = fileInfo.getNodeRef();

                            log.debug("FILE: NodeRef" + fileNodeRef + ", Name: " + fileName + ", CreatedDate: " + fileInfo.getCreatedDate() + ", ModifiedDate: " + fileInfo.getModifiedDate());
                            if (FilenameUtil.isFileBdoc(fileName) || FilenameUtil.isFileDdoc(fileName)) {
                                String fileExt = FilenameUtil.getDigiDocExt(fileName);
                                checkAndAddTeraFiles(fileNodeRef.toString(), fileName, fileExt, "");
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                        log.debug("----------------------------------------------------------------------------------------");
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

    }

    private void checkAndAddTeraFiles(String fileNodeRef, String fileName, String fileExt, String crypt) {
        if (!getTeraService().checkFileEntryByNodeRef(fileNodeRef)) {
            log.trace("Adding fileinfo to tera table...");
            getTeraService().addFileEnrty(fileNodeRef, fileName, fileExt, crypt);
        } else {
            log.trace("File is in TERA table...next..");
        }

    }

}
