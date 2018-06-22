package ee.smit.tera;

import ee.webmedia.alfresco.common.web.BeanHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessSettings implements Cloneable {
    private static final Log log = LogFactory.getLog(ProcessSettings.class);

    private int batchSize = 10;



    public int getBatchSize() {
        int confBatchSize = BeanHelper.getDigiSignService().getBatchSize();
        if(confBatchSize > 0){
            return confBatchSize;
        }
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }


    @Override
    public ProcessSettings clone() {
        try {
            return (ProcessSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
