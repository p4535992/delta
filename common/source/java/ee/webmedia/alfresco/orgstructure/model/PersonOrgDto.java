package ee.webmedia.alfresco.orgstructure.model;

import java.io.Serializable;
import java.util.Date;

public class PersonOrgDto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String unitId;
	private String personCode;
	
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getPersonCode() {
		return personCode;
	}
	public void setPersonCode(String personCode) {
		this.personCode = personCode;
	}
	
}
