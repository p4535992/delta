package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DvkModel.URI)
public interface AccessRights {
    public String getLetterAccessRestriction();

    public void setLetterAccessRestriction(String letterAccessRestriction);

    public Date getLetterAccessRestrictionBeginDate();

    public void setLetterAccessRestrictionBeginDate(Date letterAccessRestrictionBeginDate);

    public Date getLetterAccessRestrictionEndDate();

    public void setLetterAccessRestrictionEndDate(Date letterAccessRestrictionEndDate);

    public String getLetterAccessRestrictionReason();

    public void setLetterAccessRestrictionReason(String letterAccessRestrictionReason);

}
