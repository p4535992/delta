package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * This class can be used to give feedback about actions done service layer to web layer (where information could be formated into faces message using
 * {@link MessageUtil})
 * 
 * @author Ats Uiboupin
 */
public class FeedbackWrapper implements Iterable<FeedbackVO> {
    private Collection<FeedbackVO> feedbackItems;

    public void addFeedbackItem(FeedbackVO feedbackItem) {
        if (feedbackItems == null) {
            feedbackItems = new ArrayList<FeedbackVO>(5);
        }
        this.feedbackItems.add(feedbackItem);
    }

    @Override
    public Iterator<FeedbackVO> iterator() {
        return feedbackItems == null ? Collections.<FeedbackVO> emptyList().iterator() : feedbackItems.iterator();
    }

}
