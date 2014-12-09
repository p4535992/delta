package ee.webmedia.mobile.alfresco.common.model;

public class MessageItem {

    private String message;
    private Action action;

    public MessageItem(String message) {
        this.message = message;
    }

    public MessageItem(String message, String actionLabel, String href, String elementClass) {
        this.message = message;
        action = new Action(actionLabel, href, elementClass);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Action getAction() {
        return action;
    }

    public class Action {

        private final String label;
        private final String href;
        private final String elementClass;

        public Action(String label, String href, String elementClass) {
            this.label = label;
            this.href = href;
            this.elementClass = elementClass;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public String getElementClass() {
            return elementClass;
        }
    }
}
