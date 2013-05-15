package ee.webmedia.alfresco.sharepoint.entity.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.sharepoint.entity.CaseFile;

public class CaseFileMapper implements ParameterizedRowMapper<CaseFile> {

    @Override
    public CaseFile mapRow(ResultSet rs, int i) throws SQLException {
        CaseFile result = new CaseFile();
        // First line is procedure ID - not needed.
        result.setVolumeMark(rs.getString(2));
        result.setOwnerName(rs.getString(3));
        result.setTitle(rs.getString(4));
        result.setUserName(rs.getString(5));
        result.setValidFrom(rs.getTimestamp(6));
        result.setValidTo(rs.getTimestamp(7));
        result.setStatus(rs.getString(8));
        result.setContactName(rs.getString(9));
        result.setApplicantType(rs.getString(10));
        result.setApplicantArea(rs.getString(11));
        result.setApplicationLanguage(rs.getString(12));
        result.setCaseResult(rs.getString(13));
        result.setSupervisionVisit(rs.getBoolean(14));
        result.setOpcat(rs.getBoolean(15));
        result.setComment(rs.getString(16));
        result.setKeywordLevel1(rs.getString(17));
        result.setKeywordLevel2(rs.getString(18));
        result.setLegislation(rs.getString(19));
        result.setGeneralRightToEquality(rs.getBoolean(20));
        result.setDiscrimination(rs.getBoolean(21));
        result.setGoodAdministration(rs.getBoolean(22));
        result.setChildRights(rs.getBoolean(23));
        result.setChildApplicant(rs.getBoolean(24));
        result.setToSurvey(rs.getBoolean(25));
        result.setProcedureStatus(rs.getString(26));
        result.setEqualityOfTreatment(rs.getBoolean(27));
        result.setWorkflowDueDate(rs.getTimestamp(28));
        result.setKeywordsString(rs.getString(29));
        return result;
    }
}
