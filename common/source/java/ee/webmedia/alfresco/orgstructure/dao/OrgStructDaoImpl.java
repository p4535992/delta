package ee.webmedia.alfresco.orgstructure.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ee.webmedia.alfresco.orgstructure.model.OrgStructDto;
import ee.webmedia.alfresco.orgstructure.model.PersonOrgDto;

public class OrgStructDaoImpl implements OrgStructDao {
	
	private JdbcTemplate jdbcTemplate;
	private PlatformTransactionManager txManager;
	
	@Override
	public List<OrgStructDto> getOrgStructs() {
		final String sql = "select unit_id, name, institution_reg_code, group_email from org_struct";
		List<OrgStructDto> result = new ArrayList<>();
		result.addAll(jdbcTemplate.query(sql, new OrgStructRowMapper()));
		return result;
	}
	
	@Override
	public List<PersonOrgDto> getPersonOrgs() {
		final String sql = "select unit_id, person_id from org_struct_person";
		List<PersonOrgDto> result = new ArrayList<>();
		result.addAll(jdbcTemplate.query(sql, new PersonOrgRowMapper()));
		return result;
	}
	
	@Override
	public PersonOrgDto getPersonOrg(String username) {
		final String sql = "select unit_id, person_id from org_struct_person where person_id = ?";
		List<PersonOrgDto> result = new ArrayList<>();
		result.addAll(jdbcTemplate.query(sql, new PersonOrgRowMapper(), username));
		return (CollectionUtils.isEmpty(result))?null:result.get(0);
	}
	
	@Override
	public int getOrgParamValue(String paramName) {
		final String sql = "select value from org_struct_param where name=?";
		
		List<Integer> result = jdbcTemplate.query(sql, new ParameterizedRowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("value");
            }
        },
		paramName);
        
		if (result != null && result.size() > 0) {
			return result.get(0);
		}
		return 1;
	}
	
	@Override
	public int updateOrgParamValue(String paramName, int value) {
		 
		final String sql = "update org_struct_param set value = ? where name=?";
		DefaultTransactionDefinition paramTransactionDefinition = new DefaultTransactionDefinition();
		int res = 0;
		TransactionStatus status = txManager.getTransaction(paramTransactionDefinition );
		try{
			res = jdbcTemplate.update(sql, value, paramName);
			txManager.commit(status);
		}catch (Exception e) {
			txManager.rollback(status);
		}
		/*
		int res = jdbcTemplate.update(sql, value, paramName);
		try {
			boolean isAutoCommit = jdbcTemplate.getDataSource().getConnection().getAutoCommit();
			int isol = jdbcTemplate.getDataSource().getConnection().getMetaData().getDefaultTransactionIsolation();
			
			jdbcTemplate.getDataSource().getConnection().commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		int val = getOrgParamValue(paramName);
		return res;
	}
	
	
	private static class OrgStructRowMapper implements ParameterizedRowMapper<OrgStructDto> {

        @Override
        public OrgStructDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        	OrgStructDto dto = new OrgStructDto();
        	dto.setUnitId(rs.getString("unit_id"));
        	dto.setName(rs.getString("name"));
        	dto.setInstitutionRegCode(rs.getString("institution_reg_code"));
        	dto.setGroupEmail(rs.getString("group_email"));
        	
            return dto;
        }

    }
    
    private static class PersonOrgRowMapper implements ParameterizedRowMapper<PersonOrgDto> {

        @Override
        public PersonOrgDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        	PersonOrgDto dto = new PersonOrgDto();
        	dto.setUnitId(rs.getString("unit_id"));
        	dto.setPersonCode(rs.getString("person_id"));
        	
        	return dto;
        }

    }

	
    public void setTxManager(PlatformTransactionManager txManager) {
    	this.txManager = txManager;
    }
	
	public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
}
