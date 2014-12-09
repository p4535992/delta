package ee.webmedia.mobile.alfresco.common.holder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.mobile.alfresco.common.model.MessageItem;

public class UserRequestInfo {

    protected Map<String, List<MessageItem>> messages = new HashMap<>();

    public UserRequestInfo userRequestInfo() {
        return new UserRequestInfo();
    }

    public void addMessage(String message, MessageSeverity messageSeverity) {

        String messageSeverityStr = messageSeverity.name();
        if (!messages.containsKey(messageSeverityStr)) {
            messages.put(messageSeverityStr, new ArrayList<MessageItem>());
        }
        messages.get(messageSeverityStr).add(new MessageItem(message));
    }

    public Map<String, List<MessageItem>> getMessages() {
        return messages;
    }

}
