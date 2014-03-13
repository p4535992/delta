package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class CompoundWorkflowCommentTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_compound_workflow_comment_table.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_compound_workflow_comment";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_compound_workflow_comment");
    }

    @Override
    protected String getTablesLogName() {
        return "Compound workflow comment";
    }

}
