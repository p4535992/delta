package ee.webmedia.alfresco.adit.service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.xtee.client.ametlikuddokumendid.AditProp;
import ee.webmedia.xtee.client.ametlikuddokumendid.AmetlikudDokumendidXTeeService;
import ee.webmedia.xtee.client.exception.XTeeServiceConsumptionException;

public class AditServiceImpl implements AditService {

    private DocumentSearchService documentSearchService;
    private NodeService nodeService;
    private AmetlikudDokumendidXTeeService ametlikudDokumendidXTeeService;

    @Override
    public int updateAditDocViewedStatuses() {
        Map<NodeRef, Pair<String, String>> sendInfoRefAndIds = documentSearchService.searchUnopenedAditDocs();
        if (sendInfoRefAndIds.isEmpty()) {
            return 0;
        }
        Set<String> dvkIds = new HashSet<String>();
        for (Pair<String, String> p : sendInfoRefAndIds.values()) {
            dvkIds.add(p.getFirst());
        }
        Map<String, List<Map<String, Serializable>>> sendStatuses = null;
        try {
            sendStatuses = ametlikudDokumendidXTeeService.getSendStatusV1(dvkIds, null);
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_ADIT);
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_ADIT, e);
            throw e;
        } catch (XTeeServiceConsumptionException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_ADIT, e);
            String faultMessage = e.getNonTechnicalFaultString() != null ? e.getNonTechnicalFaultCode() : e.getFaultString();
            throw new RuntimeException(faultMessage);
        }

        int updatedPropsCount = 0;
        for (Entry<NodeRef, Pair<String, String>> entry : sendInfoRefAndIds.entrySet()) {
            Pair<String, String> dvkAndRecipientId = entry.getValue();
            String dvkId = dvkAndRecipientId.getFirst();
            String recipientId = dvkAndRecipientId.getSecond();
            List<Map<String, Serializable>> personPropslist = sendStatuses.get(dvkId);
            for (Map<String, Serializable> personProps : personPropslist) {
                String userCode = (String) personProps.get(AditProp.Document.ID_CODE.getName());
                if (recipientId != null & userCode != null && recipientId.replaceAll("[\\D]", "").equals(userCode.replaceAll("[\\D]", ""))
                        && personProps.containsKey(AditProp.Document.OPEN_TIME.getName())) {
                    nodeService.setProperty(entry.getKey(), DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME, personProps.get(AditProp.Document.OPEN_TIME.getName()));
                    updatedPropsCount++;
                    break;
                }
            }
        }
        return updatedPropsCount;
    }

    @Override
    public Set<String> getUnregisteredAditUsers(Set<String> userIdCodes) throws XTeeServiceConsumptionException {
        Set<String> unregisteredUsers = new HashSet<String>();
        String runAsUser = AuthenticationUtil.getRunAsUser();
        Map<String, Map<String, Serializable>> results = ametlikudDokumendidXTeeService.getUserInfoV1(userIdCodes, runAsUser);
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

    public void setAmetlikudDokumendidXTeeService(AmetlikudDokumendidXTeeService ametlikudDokumendidXTeeService) {
        this.ametlikudDokumendidXTeeService = ametlikudDokumendidXTeeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

}
