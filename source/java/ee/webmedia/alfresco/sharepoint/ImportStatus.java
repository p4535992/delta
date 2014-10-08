package ee.webmedia.alfresco.sharepoint;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * An object definition for keeping status progress of current import.
 */
public class ImportStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private int countProcessed;

    private int countFailed;

    private final List<String> messages = new ArrayList<String>(6);

    private long start = -1;

    public void reset() {
        messages.clear();
    }

    public void incrCount() {
        countProcessed++;
    }

    public void incrFailed() {
        countFailed++;
    }

    public void beginStruct() {
        begin("START: structure import");
    }

    public void endStruct() {
        end("FINISHED: structure import");
    }

    public void beginDocs() {
        begin("START: document import");
    }

    public void endDocs() {
        end("FINISHED: document import");
    }

    public void beginWorkflow() {
        begin("START: workflow import");
    }

    public void endWorkflow() {
        end("FINISHED: workflow import");
    }

    private void begin(String msg) {
        messages.add(msg);
        start = System.currentTimeMillis();
    }

    private void end(String msg) {
        messages.add(getCountStatus());
        messages.add(msg);
        countProcessed = 0;
        countFailed = 0;
        start = -1;
    }

    private String getCountStatus() {
        long timeSpent = (System.currentTimeMillis() - start) / 1000;
        String avg = countProcessed != 0 ? new BigDecimal(timeSpent).divide(new BigDecimal(countProcessed), 10, BigDecimal.ROUND_HALF_UP).toString() : "[N/A]";
        return String.format("Processed %d items (of which %d failed) in %d seconds (avg: %s sec/item)", countProcessed, countFailed, timeSpent, avg);
    }

    @Override
    public String toString() {
        if (start == -1) {
            return StringUtils.join(messages, "<br/>");
        }

        List<String> msgs = new ArrayList<String>(messages);
        msgs.add(getCountStatus());
        return StringUtils.join(msgs, "<br/>");
    }
}
