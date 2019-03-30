package ee.webmedia.alfresco.dvk.model;


public class DvkReceivedDocument extends AbstractDvkDocument {

    /**
     * aka dhl_id - unique id assigned to the sent document by the DVK server
     */
    private String dvkId;

    public String getDvkId() {
        return dvkId;
    }

    public void setDvkId(String dvkId) {
        this.dvkId = dvkId;
    }

}
