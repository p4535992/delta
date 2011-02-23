package ee.webmedia.alfresco.util;

import java.io.IOException;
import java.io.InputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import ee.webmedia.alfresco.app.AppConstants;

public class ContentCreatorHelper {
    
    private ApplicationContext applicationContext;
    
    private FileFolderService fileFolderService;
    private SearchService searchService;
    
    private NodeRef companyHome;
    
    public ContentCreatorHelper(ApplicationContext ctx) {
        applicationContext = ctx;
        fileFolderService = (FileFolderService) applicationContext.getBean("fileFolderService");
        searchService = (SearchService) applicationContext.getBean("searchService");
        
        // Get a reference to the company home node
        ResultSet results1 = searchService.query(
                new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
                SearchService.LANGUAGE_XPATH,
                "app:company_home");
        companyHome = results1.getNodeRefs().get(0);
        results1.close();
    }
    
    public NodeRef createTestFolder(String name) {
        return fileFolderService.create(companyHome, name, ContentModel.TYPE_FOLDER).getNodeRef();
    }
    
    public NodeRef createTestFile(NodeRef parent, String name) {
        return fileFolderService.create(parent, name, ContentModel.TYPE_CONTENT).getNodeRef();
    }
    
    public ContentWriter getContentWriter(NodeRef nodeRef, String encoding, String mimeType) {
        ContentWriter contentWriter = fileFolderService.getWriter(nodeRef);
        contentWriter.setMimetype(mimeType);
        contentWriter.setEncoding(encoding);
        return contentWriter; 
    }
    
    public void writeTestContent(NodeRef nodeRef, String encoding, String mimeType, String content) {
        getContentWriter(nodeRef, encoding, mimeType).putContent(content);
    }
    
    public static InputStream getClassPathResourceInputStream(String resource) throws IOException {
        return new ClassPathResource(resource).getInputStream();
    }
    
    public static String getClassPathResource(String resource) throws IOException {
        InputStream is = getClassPathResourceInputStream(resource);
        StringBuilder sb = new StringBuilder();
        byte[] ba = new byte[2048];
        while (is.read(ba) != -1) {
            sb.append(new String(ba));
        }
        is.close();
        return sb.toString();
    }

    public NodeRef createTestFileWithContentUtf8(NodeRef folderNodeRef, String fileName) {
        final NodeRef testFileNodeRef = createTestFile(folderNodeRef, fileName);
        writeTestContent(testFileNodeRef, AppConstants.CHARSET, MimetypeMap.MIMETYPE_TEXT_PLAIN, "täpiline sisu: šõäöüž");
        return testFileNodeRef;
    }

}
