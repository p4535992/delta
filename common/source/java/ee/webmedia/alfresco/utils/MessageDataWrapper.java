package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * This class can be used to give feedback about actions done service layer to web layer (where information could be formated into faces message using {@link MessageUtil})
 */
public class MessageDataWrapper implements Iterable<MessageData> {
    private Collection<MessageData> feedbackItems;

    public MessageDataWrapper() {
        // No-args constructor
    }

    public MessageDataWrapper(Collection<MessageData> feedbackItems) {
        this.feedbackItems = new ArrayList<MessageData>(feedbackItems);
    }

    public void addFeedbackItem(MessageData feedbackItem) {
        if (feedbackItems == null) {
            feedbackItems = new ArrayList<MessageData>(5);
        }
        feedbackItems.add(feedbackItem);
    }

    public int getFeedbackItemCount() {
        return feedbackItems == null ? 0 : feedbackItems.size();
    }

    @Override
    public Iterator<MessageData> iterator() {
        return feedbackItems == null ? Collections.<MessageData> emptyList().iterator() : feedbackItems.iterator();
    }

    public boolean hasErrors() {
        for (MessageData feedBack : this) {
            if (MessageSeverity.ERROR.equals(feedBack.getSeverity()) || MessageSeverity.FATAL.equals(feedBack.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return feedbackItems == null ? true : feedbackItems.isEmpty();
    }

    @Override
    public String toString() {
        return hasErrors() ? "HAS ERRORS:\n" : "Feedback:\n" + feedbackItems;
    }

}
