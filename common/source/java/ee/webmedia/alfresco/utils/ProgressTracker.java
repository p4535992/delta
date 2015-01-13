package ee.webmedia.alfresco.utils;

import java.io.Serializable;

import org.springframework.util.Assert;

public class ProgressTracker implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long totalSize;
    private long completedSize;
    private long thisRunCompletedSize = 0;
    private final long thisRunStartTime;
    private long stepStartTime;
    private long stepCompletedSize = 0;

    public ProgressTracker(long totalSize, long completedSize) {
        Assert.isTrue(totalSize >= 0);
        Assert.isTrue(completedSize >= 0);
        Assert.isTrue(completedSize <= totalSize);
        this.totalSize = totalSize;
        this.completedSize = completedSize;
        thisRunStartTime = System.nanoTime() / 1000000L;
        stepStartTime = thisRunStartTime;
    }

    public String step(long i) {
        stepCompletedSize += i;
        thisRunCompletedSize += i;
        completedSize += i;
        long stepEndTime = System.nanoTime() / 1000000L;
        long stepTime = stepEndTime - stepStartTime;
        if (thisRunCompletedSize <= 0 || (stepTime < 1000 && completedSize < totalSize)) {
            return null;
        }
        if (stepTime == 0) {
            // If completedSize == totalSize, then we still want to display the last info line, even though stepTime = 0, so prevent division by zero
            stepEndTime++;
        }
        double completedPercent = totalSize == 0 ? 100d : (completedSize * 100L / ((double) totalSize));
        double lastDocsPerSec = stepCompletedSize * 1000L / ((double) stepTime);
        long thisRunTotalTime = stepEndTime - thisRunStartTime;
        double totalDocsPerSec = thisRunCompletedSize * 1000L / ((double) thisRunTotalTime);
        long remainingSize = totalSize - completedSize;
        long divisor = thisRunCompletedSize * 60000L;
        long etaMinutes = (remainingSize * thisRunTotalTime / divisor) + 1;
        long etaHours = 0;
        if (etaMinutes > 59) {
            etaHours = etaMinutes / 60;
            etaMinutes = etaMinutes % 60;
        }
        String eta = etaMinutes + "m";
        if (etaHours > 0) {
            eta = etaHours + "h " + eta;
        }
        stepStartTime = stepEndTime;
        stepCompletedSize = 0;
        String info = "%6.2f%% completed - %7d of %7d, %5.1f per second (last), %5.1f (total), ETA %s";
        return String.format(info, completedPercent, completedSize, totalSize, lastDocsPerSec, totalDocsPerSec, eta);
    }

}
