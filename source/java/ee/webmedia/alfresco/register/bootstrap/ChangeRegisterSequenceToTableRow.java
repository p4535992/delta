package ee.webmedia.alfresco.register.bootstrap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.register.service.RegisterService;

public class ChangeRegisterSequenceToTableRow extends AbstractNodeUpdater {
    private final String SEQ_REGISTER_PREFIX = "register_";
    private final String SEQ_REGISTER_SUFFIX = "_seq";

    private RegisterService registerService;
    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return null; // nothing to do
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(registerService.getRoot());
        Set<NodeRef> nodeSet = new HashSet<NodeRef>(children.size());
        for (ChildAssociationRef childAssociationRef : children) {
            nodeSet.add(childAssociationRef.getChildRef());
        }
        return nodeSet;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Integer idProp = (Integer) nodeService.getProperty(nodeRef, RegisterModel.Prop.ID);
        Integer counterValue = getRegisterCounter(idProp);
        jdbcTemplate.update("INSERT INTO delta_register (register_id,counter) values (?,?)", idProp, counterValue);
        jdbcTemplate.update("DROP SEQUENCE " + getSequenceName(idProp));
        return new String[] { idProp.toString(), counterValue.toString() };
    }

    private Integer getRegisterCounter(int registerId) {
        // XXX: millegi pärast viisakam lahendus ei tööta: BadSqlGrammarException: PreparedStatementCallback; bad SQL grammar [SELECT last_value FROM ?]; nested exception is
        // org.postgresql.util.PSQLException: ERROR: syntax error at or near "$1"
        // return jdbcTemplate.queryForInt("SELECT last_value FROM ?", getSequenceName(registerId));
        return jdbcTemplate.queryForInt("SELECT last_value FROM " + getSequenceName(registerId));
    }

    private String getSequenceName(int registerId) {
        final String seqName = SEQ_REGISTER_PREFIX + registerId + SEQ_REGISTER_SUFFIX;
        return seqName;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
