package ee.webmedia.alfresco.adit.service;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import ee.smit.adit.domain.AditDocSendStatusInfo;
import ee.smit.adit.domain.AditSendStatusVIResponse;
import ee.webmedia.alfresco.common.web.BeanHelper;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import com.nortal.jroad.client.adit.AditProp;
import com.nortal.jroad.client.adit.AditXTeeService;
import com.nortal.jroad.client.exception.XRoadServiceConsumptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class AditServiceImpl implements AditService {
    protected final static Log log = LogFactory.getLog(AditServiceImpl.class);
    private DocumentSearchService documentSearchService;
    private NodeService nodeService;
    private AditXTeeService aditXTeeService;
    private String infoSystem;

    @Override
    public int updateAditDocViewedStatuses() {
        log.info("UPDATE ADIT DOC VIEWES STATUSES....");
        Map<NodeRef, Pair<String, String>> sendInfoRefAndIds = documentSearchService.searchUnopenedAditDocs();
        if (sendInfoRefAndIds.isEmpty()) {
            return 0;
        }

        Set<String> dvkIds = new HashSet<String>();
        List<String> dvkIdsList = new ArrayList<>();
        for (Pair<String, String> p : sendInfoRefAndIds.values()) {
            log.debug("ADIT: DVK DOC ID: " + p.getFirst());
            dvkIds.add(p.getFirst());
            dvkIdsList.add(p.getFirst());
        }

        Map<String, List<Map<String, Serializable>>> sendStatuses = null;
        Map<String, List<AditDocSendStatusInfo>> documentsInfoMap = new HashMap<>();

        try {
            if(BeanHelper.getAditAdapterService().isAditAdapterActive()){
                String runAsUser = AuthenticationUtil.getRunAsUser();

                log.debug("Make ADIT-ADAPTER request...");
                AditSendStatusVIResponse response = BeanHelper.getAditAdapterSearches().getSendStatuses(dvkIds, runAsUser);

                if(response == null) {
                    log.warn("ADIT ADAPTER response is NULL!");
                    return 0;
                }

                documentsInfoMap = response.getDocuments();
                if(documentsInfoMap == null){
                    log.warn("ADIT ADAPTER RESPONSE DocumentsInfo list is NULL!");
                    return 0;
                }

                log.debug("ADIT ADAPTER DocumentsInfo map size: " + documentsInfoMap.size());

            } else {
                sendStatuses = aditXTeeService.getSendStatusV1(dvkIds, infoSystem);

            }
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_ADIT);
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_ADIT, e);
            throw e;
        } catch (XRoadServiceConsumptionException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_ADIT, e);
            String faultMessage = e.getNonTechnicalFaultString() != null ? e.getNonTechnicalFaultCode() : e.getFaultString();
            throw new RuntimeException(faultMessage);
        }

        int updatedPropsCount = 0;
        for (Entry<NodeRef, Pair<String, String>> entry : sendInfoRefAndIds.entrySet()) {
            Pair<String, String> dvkAndRecipientId = entry.getValue();
            String dvkId = dvkAndRecipientId.getFirst();
            String recipientId = dvkAndRecipientId.getSecond();

            List<AditDocSendStatusInfo> personPropList = new ArrayList<>();
            if(BeanHelper.getAditAdapterService().isAditAdapterActive()){
                personPropList = documentsInfoMap.get(dvkId);
            } else {
                personPropList = convertDirecTAditXteeResponse(sendStatuses.get(dvkId));
            }

            NodeRef key = entry.getKey();

            for (AditDocSendStatusInfo personProps : personPropList) {
                String userCode = personProps.getUserCode();
                boolean containsOpenTime = personProps.isContainsOpenTime();
                Date openTime = personProps.getOpenTime();
                if(checkSendStatus(key, recipientId, userCode, containsOpenTime, openTime)){
                    updatedPropsCount++;
                    break;
                }

            }
        }
        return updatedPropsCount;
    }

    private List<AditDocSendStatusInfo> convertDirecTAditXteeResponse(List<Map<String, Serializable>> personPropslist){
        List<AditDocSendStatusInfo> infos = new ArrayList<>();
        for (Map<String, Serializable> personProps : personPropslist) {
            AditDocSendStatusInfo info = new AditDocSendStatusInfo();
            info.setContainsOpenTime(personProps.containsKey(AditProp.Document.OPEN_TIME.getName()));
            info.setOpenTime((Date) personProps.get(AditProp.Document.OPEN_TIME.getName()));
            info.setUserCode((String) personProps.get(AditProp.Document.ID_CODE.getName()));
            infos.add(info);
        }
        return infos;
    }

    private boolean checkSendStatus(NodeRef key, String recipientId, String userCode, boolean containsOpenTime, Date openTime){
            if (recipientId != null & userCode != null && recipientId.replaceAll("[\\D]", "").equals(userCode.replaceAll("[\\D]", ""))
                    && containsOpenTime) {
                nodeService.setProperty(key, DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME, openTime);
                return true;

            }
            return false;
    }

    @Override
    public Set<String> getUnregisteredAditUsers(Set<String> userIdCodes) throws XRoadServiceConsumptionException {
        log.info("ADIT: get unregistred users...");
        String runAsUser = AuthenticationUtil.getRunAsUser();
        log.info("RUN as a user: " + runAsUser);
        if(BeanHelper.getAditAdapterService().isAditAdapterActive()){
            log.info("ADIT-ADAPTER is ACTIVE....");
            return BeanHelper.getAditAdapterSearches().getUnregistredUsers(userIdCodes, runAsUser);
        } else {
            log.info("USIN DIRECT ADIT X-TEE REQUEST...");
            Set<String> unregisteredUsers = new HashSet<String>();
            Map<String, Map<String, Serializable>> results = aditXTeeService.getUserInfoV1(userIdCodes, infoSystem);
            if (!results.isEmpty()) {
                for (String userId : userIdCodes) {
                    Map<String, Serializable> props = results.get(userId);
                    if (props == null) {
                        unregisteredUsers.add(userId);
                        continue;
                    }
                    boolean hasJoined = Boolean.TRUE.equals(props.get(AditProp.User.HAS_JOINED.getName()));
                    boolean usesDvk = Boolean.TRUE.equals(props.get(AditProp.User.USES_DVK.getName()));
                    if (hasJoined && !usesDvk) {
                        continue;
                    }
                    unregisteredUsers.add(userId);
                }
            }
            return unregisteredUsers;
        }
    }

    public void setAditXTeeService(AditXTeeService aditXTeeService) {
        this.aditXTeeService = aditXTeeService;
    }
    
    public void setInfoSystem(String infoSystem) {
        this.infoSystem = infoSystem;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

}
