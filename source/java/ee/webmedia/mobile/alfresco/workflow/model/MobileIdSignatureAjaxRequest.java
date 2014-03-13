package ee.webmedia.mobile.alfresco.workflow.model;

public class MobileIdSignatureAjaxRequest {

    private String mobileIdChallengeId;
    private Long signingFlowId;

    public String getMobileIdChallengeId() {
        return mobileIdChallengeId;
    }

    public void setMobileIdChallengeId(String mobileIdChallengeId) {
        this.mobileIdChallengeId = mobileIdChallengeId;
    }

    public Long getSigningFlowId() {
        return signingFlowId;
    }

    public void setSigningFlowId(Long signingFlowId) {
        this.signingFlowId = signingFlowId;
    }
}
