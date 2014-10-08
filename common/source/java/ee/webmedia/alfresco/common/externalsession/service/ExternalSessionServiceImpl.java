package ee.webmedia.alfresco.common.externalsession.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.user.service.UserNotFoundException;

public class ExternalSessionServiceImpl implements ExternalSessionService {

    private ParametersService parametersService;
    private PersonService personService;
    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    public Pair<String, Date> createSession(String username) {
        if (!personService.personExists(username)) {
            throw new UserNotFoundException(username);
        }
        String sessId = GUID.generate();
        Long expTime = parametersService.getLongParameter(Parameters.EXTERNAL_SESSION_EXPIRATION_TIME);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, expTime.intValue());
        saveSession(username, sessId, cal);
        return new Pair<String, Date>(sessId, cal.getTime());
    }

    @Override
    public String getUserForSession(String sessionId) {
        Map<String, Object> map = queryForSessionById(sessionId);
        if (map == null) {
            return null;
        }
        Timestamp timestamp = (Timestamp) map.get("expiration_date_time");
        if (timestamp == null) {
            return null;
        }
        long expiry = timestamp.getTime();
        if (expiry <= System.currentTimeMillis()) {
            return null;
        }
        return (String) map.get("username");
    }

    private Map<String, Object> queryForSessionById(String sessionId) {
        try {
            String sql = "SELECT * FROM delta_external_session WHERE session_id=?";
            return jdbcTemplate.queryForMap(sql, sessionId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void saveSession(String username, String sessionId, Calendar expiration) {
        int updatedCount = jdbcTemplate.update("UPDATE delta_external_session SET session_id=?, expiration_date_time=?, created_date_time=CURRENT_TIMESTAMP WHERE username=?",
                sessionId, expiration,
                username);
        if (updatedCount == 0) {
            jdbcTemplate.update("INSERT INTO delta_external_session (username, session_id, expiration_date_time) VALUES (?,?,?)", username, sessionId, expiration);
        }
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

}
