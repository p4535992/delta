package ee.webmedia.alfresco.orgstructure.dao;

import java.util.List;

import ee.webmedia.alfresco.orgstructure.model.OrgStructDto;
import ee.webmedia.alfresco.orgstructure.model.PersonOrgDto;

public interface OrgStructDao {
	String BEAN_NAME = "orgStructDao";
	
	public static String PARAM_NAME_ORG_STRUCT = "orgStructChanged";
	public static String PARAM_NAME_PERSON_ORG = "personOrgChanged";
	
	List<OrgStructDto> getOrgStructs();
	List<PersonOrgDto> getPersonOrgs();
	PersonOrgDto getPersonOrg(String username);
	int getOrgParamValue(String paramName);
	int updateOrgParamValue(String paramName, int value);

}
