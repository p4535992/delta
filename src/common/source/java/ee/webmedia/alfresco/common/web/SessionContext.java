package ee.webmedia.alfresco.common.web;

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;

/**
 * Session context.<br>
 * NB! Unsynchronized - should be accessed from single thread
 * 
 * @author Ats Uiboupin
 */
public class SessionContext extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "sessionContext";
    public static final Random r = new Random();
    private String substitutionKey = "";
    private String forceSubstituteTaskReloadKey = "";

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value) {
        return add(value, null);
    }

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @param suggestedKeyPrefix - prefix to be used(suffix, will be generated)
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value, String suggestedKeyPrefix) {
        String key;
        do {
            key = suggestedKeyPrefix + "_" + Long.toString(Math.abs(r.nextLong()), 36);
        } while (containsKey(key));
        put(key, value);
        return key;
    }
    
    public synchronized void setSubstitutionInfo(SubstitutionInfo substInfo){
        substitutionKey = add(substInfo);
    }
    
    public synchronized SubstitutionInfo getSubstitutionInfo(){
        return (SubstitutionInfo) (StringUtils.isEmpty(substitutionKey) ? new SubstitutionInfo() : get(substitutionKey));
    }
    
    public void setForceSubstituteTaskReload(Boolean force){
        forceSubstituteTaskReloadKey = add(force);
    }
    
    public Boolean getForceSubstituteTaskReload(){
        return (Boolean ) (StringUtils.isEmpty(forceSubstituteTaskReloadKey) ? Boolean.FALSE : get(forceSubstituteTaskReloadKey));
    }

}
