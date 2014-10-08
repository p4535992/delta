package ee.webmedia.alfresco.log.bootstrap;

import java.util.Map;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class LogSequenceInitBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT delta_log_date.idprefix, to_char(CURRENT_DATE,'YYYYMMDD') AS idprefix_now FROM delta_log_date LIMIT 1");
        String idPrefix = (String) result.get("idprefix");
        String idPrefixNow = (String) result.get("idprefix_now");
        LOG.info("idPrefixNow=" + idPrefixNow + " idPrefix=" + idPrefix);
        if (idPrefixNow.equals(idPrefix)) {
            long max = jdbcTemplate.queryForLong("SELECT COUNT(*) FROM delta_log WHERE date(created_date_time) = CURRENT_DATE");
            Map<String, Object> seq = jdbcTemplate.queryForMap("SELECT * FROM delta_log_seq");
            long seqLastValue = (Long) seq.get("last_value");
            boolean seqIsCalled = (Boolean) seq.get("is_called");
            LOG.info("max=" + max + " seqLastValue=" + seqLastValue + " seqIsCalled=" + seqIsCalled);
            if (!seqIsCalled) {
                seqLastValue--;
                LOG.info("max=" + max + " seqLastValue=" + seqLastValue);
            }
            if (seqLastValue < max) {
                jdbcTemplate.queryForMap("SELECT setval('delta_log_seq', ?, true)", max);
                LOG.info("setval to " + max);
            }
        }
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
