package ee.webmedia.mobile.alfresco.common.holder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

public class UserRequestInfo {

    protected Map<String, Set<String>> messages = new HashMap<String, Set<String>>();

    public UserRequestInfo userRequestInfo() {
        return new UserRequestInfo();
    }

    public void addMessage(String message, MessageSeverity messageSeverity) {

        String messageSeverityStr = messageSeverity.name();
        if (!messages.containsKey(messageSeverityStr)) {
            messages.put(messageSeverityStr, new HashSet<String>());
        }
        messages.get(messageSeverityStr).add(message);
    }

    public Map<String, Set<String>> getMessages() {
        return messages;
    }

}
