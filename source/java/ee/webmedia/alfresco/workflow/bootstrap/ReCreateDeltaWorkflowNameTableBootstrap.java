package ee.webmedia.alfresco.workflow.bootstrap;

import org.alfresco.service.namespace.QName;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.bootstrap.ExecuteStatementsBootstrap;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ReCreateDeltaWorkflowNameTableBootstrap extends ExecuteStatementsBootstrap {

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    public void executeInternal() throws Exception {
        super.executeInternal();
        String sqlQuery = "insert into delta_workflow_type_name "
                + "(select alf_qname.id, ? from alf_qname join alf_namespace ns on ns.id = alf_qname.ns_id where alf_qname.local_name = ? and ns.uri = ?)";
        for (QName workflowType : BeanHelper.getWorkflowConstantsBean().getWorkflowTypes().keySet()) {
            jdbcTemplate.update(sqlQuery, MessageUtil.getMessage(workflowType.getLocalName()), workflowType.getLocalName(), workflowType.getNamespaceURI());
        }
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
