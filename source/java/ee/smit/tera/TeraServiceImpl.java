package ee.smit.tera;

import ee.smit.tera.model.TeraFilesEntry;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ee.webmedia.alfresco.common.web.BeanHelper.getTeraService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.document.file.model.FileModel.Props.DISPLAY_NAME;


public class TeraServiceImpl implements TeraService {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TeraServiceImpl.class);
    private static final FastDateFormat LOG_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");

    private JdbcTemplate jdbcTemplate;


    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addFileEnrty(String nodeRef, String filename, String filetype, String crypt){
        //String FILENAME = "/app/delta/tera-files.txt";
        //String FILENAME_SQL = "/app/delta/tera-files.sql";
        //writeTofile(FILENAME, nodeRef);
        Connection conn = getConnection();
        try{
            Statement stmt = conn.createStatement();

            String SQL = "INSERT INTO tera_files (filename, filetype, node_ref) VALUES ('"+filename+"', '"+filetype+"', '"+nodeRef+"')";
            //log.trace("QUERY: " + SQL);
            //writeTofile(FILENAME_SQL, SQL+";");
            int flag = stmt.executeUpdate(SQL);
            log.info("FLAG = {"+flag+"}");

            //String q = "INSERT INTO tera_files (filename, filetype, node_ref) VALUES (?,?,?)";
            //this.jdbcTemplate.update(q, filename, filetype, nodeRef);
            conn.commit();
            conn.close();
        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
        closeConnection(conn);
        //log.trace("COUNT ALL FILES in TERA TABLE: " + countAllFiles());
    }

    private void writeTofile(String FILENAME, String data){
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME, true))) {
            bw.write(data +"\n");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean checkFileEntryByNodeRef(String nodeRef){
        String q = "SELECT count(*) FROM tera_files WHERE node_ref=?";
        log.trace("QUERY: " + q + " :: nodeRef: " + nodeRef);
        int found = this.jdbcTemplate.queryForInt(q, nodeRef);
        log.trace("FOUND NODE_REF-s: " + found);
        return found != 0;
    }

    public int countAllFiles(){
        String q = "SELECT count(*) FROM tera_files;";
        log.trace("QUERY: " + q);
        return this.jdbcTemplate.queryForInt(q);
    }
    public List<TeraFilesEntry> getTeraFilesEntrys(int limit, int offset){
        String baseQuery = "SELECT id, created_date_time, filename, filetype, node_ref, crypt, asics, checked, status_info FROM tera_files WHERE asics=FALSE AND checked=FALSE ORDER By filetype DESC";

        StringBuilder q = new StringBuilder(baseQuery);

        if (limit > 0) {
            q.append(" LIMIT ").append(limit);
        }
        if (offset > 0) {
            q.append(" OFFSET ").append(offset);
        }

        String query = q.toString();
        //log.trace("QUERY: " + query);
        return queryEntries(query, new TeraFilesEntryMapper());
    }

    private <E> List<E> queryEntries(String query, RowMapper<E> rowMapper){
        log.trace("QUERY: " + query);
        List<E> results = this.jdbcTemplate.query(query, rowMapper);
        return results;
    }

    private Connection getConnection(){
        Connection conn = null;
        try {
            conn = jdbcTemplate.getDataSource().getConnection();
            return conn;
        } catch (Exception e){
            log.error(e.getMessage());
        }
        return conn;
    }

    private void closeConnection(Connection conn){
        if(conn!= null){
            try{
                conn.close();
            } catch (Exception e1){ }

        }
    }
    public void updateCryptAndAsics(Long id, String crypt, boolean asics) {
        if (id == null) {
            return;
        }
        Connection conn = getConnection();
        try{

            String sql = "UPDATE tera_files SET created_date_time = ?, crypt = ?, asics = ? WHERE id = ?";
            log.trace("QUERY: " + sql);

            this.jdbcTemplate.update(sql, new Object[] { new Date(), id, crypt, asics });
            conn.commit();
            conn.close();
        } catch (Exception e){

            log.error(e.getMessage(), e);

        } finally {
        }
        closeConnection(conn);
    }

    public void updateTeraFilesRow(String nodeRef, String fileName, String fileType, String crypt, String statusInfo, boolean fileChecked, boolean asicsCreated){
        if(nodeRef == null){
            return;
        }

        String SQL = "UPDATE tera_files SET created_date_time = CURRENT_TIMESTAMP";
        if(fileName != null && !fileName.isEmpty()){
            SQL += ", filename = '" + fileName + "'";
        }

        if(fileType != null && !fileType.isEmpty()){
            SQL += ", filetype = '" + fileType + "'";
        }

        if(crypt != null && !crypt.isEmpty()){
            SQL += ", crypt = '" + crypt + "'";
        }

        if(statusInfo != null && !statusInfo.isEmpty()){
            SQL += ", status_info = '" + statusInfo + "'";
        }

        if(fileChecked){
            SQL += ", checked=TRUE";
        }
        if(asicsCreated){
            SQL += ", asics=TRUE";
        }

        SQL += " WHERE node_ref='"+nodeRef+"'";
        log.trace(SQL);

        Connection conn = getConnection();
        try{

            Statement stmt = conn.createStatement();
            int flag = stmt.executeUpdate(SQL);
            log.info("FLAG = {"+flag+"}");
            conn.commit();
            conn.close();

        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
        closeConnection(conn);
    }
    public void updateTeraFilesProcessStatus(String nodeRef, String crypt, boolean asics){
        if(nodeRef == null){
            return;
        }

        String sql = "UPDATE tera_files SET created_date_time = CURRENT_TIMESTAMP, crypt = ?, asics = ? WHERE node_ref = ?";
        log.trace("QUERY: " + sql + "; nodeRef value: " + nodeRef);
        this.jdbcTemplate.update(sql, new Object[] { nodeRef, crypt, asics }, new Object[] {});
    }

    public List<Map<String, Object>> findAllDigidocfiles(){
        String SQL = "SELECT "
        +" concat(alf_store.protocol, '://', alf_store.identifier, '/', alf_node.uuid) AS node_ref, "
        +" public.alf_node_properties.string_value AS filename, "
        +" substring(public.alf_node_properties.string_value from '\\.([^\\.]*)$') as ext "
        +" FROM "
        +"        alf_store "
        +" INNER JOIN alf_node ON (alf_node.store_id = alf_store.id) "
        +" INNER JOIN public.alf_node_properties ON (alf_node.id = public.alf_node_properties.node_id) "
        +" INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id) "
        +" WHERE "
        +" alf_node.id IN (SELECT public.alf_node_properties.node_id FROM public.alf_node_properties "
        +" INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id) "
        +" WHERE public.alf_node_properties.node_id IN (SELECT public.alf_node_properties.node_id FROM "
        +" public.alf_node_properties INNER JOIN public.alf_qname ON (public.alf_node_properties.qname_id = public.alf_qname.id) "
        +" WHERE (LOWER(public.alf_node_properties.string_value) LIKE '%.ddoc' OR LOWER(public.alf_node_properties.string_value) "
        +" LIKE '%.bdoc') AND public.alf_qname.local_name = 'name') AND public.alf_qname.local_name = 'content') "
        +" AND  public.alf_qname.local_name = 'name'";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL);

        return rows;
    }

    private class TeraFilesEntryMapper implements ParameterizedRowMapper<TeraFilesEntry> {

        public TeraFilesEntry mapRow(ResultSet rs, int i) throws SQLException {
            TeraFilesEntry entry = new TeraFilesEntry();
            entry.setId(rs.getLong(1));
            entry.setCreatedDateTime(rs.getTimestamp(2));
            entry.setFilename(rs.getString(3));
            entry.setFileype(rs.getString(4));
            entry.setNodeRef(rs.getString(5));
            entry.setCrypt(rs.getString(6));
            entry.setAsics(rs.getBoolean(7));
            entry.setChecked(rs.getBoolean(8));
            entry.setStatusInfo(rs.getString(9));
            return entry;
        }
    }

    public String checkFileNameSymbols(String filename){
        // Not working with Russian and other not latin chars
        filename = FilenameUtil.makeSafeFilename(
                FilenameUtil.replaceApostropheMark(filename)
        );

        /*
        filename = FilenameUtil.limitFileNameLength(
                FilenameUtil.replaceAmpersand(
                        FilenameUtil.trimDotsAndSpaces(
                                FilenameUtil.stripForbiddenWindowsCharactersAndRedundantWhitespaces(
                                        FilenameUtil.replaceApostropheMark(filename)
                                )
                        )
                )
        );*/
        return filename;
    }

    public void fixModifier(NodeService nodeService, FileFolderService fileFolderService, NodeRef nodeRef, String CREATOR_MODIFIER) {
        log.info("fixModifier()...");
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        Map<QName, Serializable> properties = fileInfo.getProperties();

        String fileModifier = (String) properties.get(ContentModel.PROP_MODIFIER);
        log.debug("Filemodifier name: " + fileModifier);

        String userFullName = getUserService().getUserFullName(fileModifier);
        log.debug("Filemodifier userfullname: " + userFullName);

        String displayName = (String) properties.get(DISPLAY_NAME);

        /*
        if(displayName == null || displayName.isEmpty() || displayName.equals("null")){
            displayName = fileInfo.getName();
            properties.put(DISPLAY_NAME, displayName);
        }
        */

        properties.put(ContentModel.PROP_MODIFIER, CREATOR_MODIFIER);
        log.debug("CREATOR_MODIFIER: " + properties.get(ContentModel.PROP_MODIFIER));
        log.debug("UPDATE FILE Properties...");
        nodeService.setNodePropertiesBySystem(fileInfo.getNodeRef(), properties, CREATOR_MODIFIER);
        log.debug("UPDATE FILE Properties...Done!");
    }

    public Map<QName, Serializable> renameFile(NodeService nodeService, FileInfo fileInfo, String fileBaseName, String fileExt, String fileType, String CREATOR_MODIFIER) {
        String asicsFilename = fileBaseName + fileExt;
        log.debug("New filename: " + asicsFilename);
        boolean loop = true;
        int i = 0;
        while (loop) {
            if (i > 1000) {
                break;
            }

            if (i > 0) {
                asicsFilename = fileBaseName + "-" + i + fileExt;
                log.debug("New LOOP filename: " + asicsFilename);
            }

            try {

                //FileInfo asicSFileInfo = fileFolderService.rename(fileInfo.getNodeRef(), asicsFilename);

                Map<QName, Serializable> properties = fileInfo.getProperties();
                String displayName = (String) properties.get(DISPLAY_NAME);

                String fileModifier = (String) properties.get(ContentModel.PROP_MODIFIER);
                log.debug("Filemodifier name: " + fileModifier);

                String userFullName = getUserService().getUserFullName(fileModifier);
                log.debug("Filemodifier userfullname: " + userFullName);

                log.info("displayName: " + displayName);
                if(displayName == null || displayName.isEmpty() || displayName.equals("null")){
                    displayName = asicsFilename;
                    log.info("displayName in NULL! Using filename: " + displayName);
                }

                String displayNameBaseName = FilenameUtils.getBaseName(displayName);
                if(displayNameBaseName == null || displayNameBaseName.isEmpty() || displayNameBaseName.equals("null")){
                    displayNameBaseName = asicsFilename;
                    log.info("displayName in NULL! Using filename: " + displayNameBaseName);
                }

                String newDisplayName = displayNameBaseName + fileExt;
                log.info("CURRENT DISPLAY NAME: " + displayName + ". UPDATE TO NEW NAME: " + newDisplayName);

                properties.put(DISPLAY_NAME, newDisplayName);
                properties.put(ContentModel.PROP_NAME, asicsFilename);
                properties.put(ContentModel.PROP_MODIFIER, CREATOR_MODIFIER);
                properties.put(ContentModel.PROP_MODIFIED, new Date());
                //properties.put(FileModel.Props.ACTIVE, true);
                log.debug("CREATOR_MODIFIER: " + properties.get(ContentModel.PROP_MODIFIER));
                log.debug("UPDATE FILE Properties...");
                //nodeService.addProperties(fileInfo.getNodeRef(), properties);
                //for(Map.Entry entry: properties.entrySet()){
                //    log.trace(entry.getKey() + " ==> " + entry.getValue());
                //}
                log.trace("SET NODE PROPERTIES By SYSTEM USER...");
                nodeService.setNodePropertiesBySystem(fileInfo.getNodeRef(), properties, CREATOR_MODIFIER);

                //getNodeProperties(fileInfo.getNodeRef());

                loop = false;
                log.debug("File rename DONE!");

                //FileInfo newFileInfo = fileFolderService.getFileInfo(fileInfo.getNodeRef());

                return properties;
            } catch (Exception e) {
                log.error(e.getMessage());
                i++;
                log.debug("NEW i value: " + i);
            }

        }
        log.info("File rename FAILED!");

        updateTeraFilesRow(fileInfo.getNodeRef().toString(), fileBaseName + "." + fileType.toLowerCase(), fileType, "SHA-1", "FILE RENAME FAILED!", true, false);

        return null;
    }

    public String getAndFixFilename(FileInfo fileInfo){
        String fileName = fileInfo.getName();
        if (fileName != null && !fileName.isEmpty()) {
            fileName = checkFileNameSymbols(fileName);
        }
        return fileName;
    }
}
