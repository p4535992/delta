package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RequestCacheBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<String, Object> resultMap = new HashMap<>();
    private String userName;

    public void setResult(String key, Object result) {
        resultMap.put(key, result);
    }

    public Object getResult(String key) {
        return resultMap.get(key);
    }

    public void clear() {
        resultMap.clear();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
