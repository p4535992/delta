package ee.webmedia.alfresco.orgstructure.model;

import java.io.Serializable;
import java.util.Date;

public class OrgStructDto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String unitId;
	private String name;
	private String groupEmail;
	private String institutionRegCode;
	
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGroupEmail() {
		return groupEmail;
	}
	public void setGroupEmail(String groupEmail) {
		this.groupEmail = groupEmail;
	}
	public String getInstitutionRegCode() {
		return institutionRegCode;
	}
	public void setInstitutionRegCode(String institutionRegCode) {
		this.institutionRegCode = institutionRegCode;
	}
	
}
