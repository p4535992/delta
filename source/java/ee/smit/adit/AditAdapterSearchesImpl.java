package ee.smit.adit;

import ee.smit.adit.domain.*;
import ee.smit.digisign.DigiSignSearchesImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.ResponseEntity;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ee.smit.common.RestUtil.makeObjectPostRequest;

public class AditAdapterSearchesImpl implements AditAdapterSearches {
    protected final static Log log = LogFactory.getLog(DigiSignSearchesImpl.class);

    protected AditAdapterService aditAdapterService;

    // -- GETTERS, SETTERS --------------------------------------------------------------
    public void setAditAdapterService(AditAdapterServiceImpl aditAdapterService) {
        this.aditAdapterService = aditAdapterService;
    }

    // -- Implementatsion ---------------------------------------------------------------
    public Set<String> getUnregistredUsers(Set<String> userIdCodes, String userIdCode) {
        log.info("getUnregistredUsers()...");
        String seachUri = aditAdapterService.getUri() + "/api/xtee/adit/getUserInfo";
        log.info("seachUri: " + seachUri);
        return getUserInfo(seachUri, userIdCodes, userIdCode);
    }

    private Set<String> getUserInfo(String uri, Set<String> userIdCodes, String userIdCode){
        log.info("getUserInfo()...");
        AditUserInfoRequest request = new AditUserInfoRequest();
        request.setUserids(userIdCodes);
        log.info("SYSTEM ID: " + aditAdapterService.getRegCode());
        request.setSystemId(aditAdapterService.getRegCode());
        log.info("REQUEST USER id-code: " + userIdCode);
        request.setUserIdCode(userIdCode);

        try {
            log.info("MAKE REQUEST...");
            Object result = makeObjectPostRequest(AditUserInfoResponse.class, request, uri);

            if(result instanceof AditUserInfoResponse){
                log.info("BODY IS INSTANCE OF AditUserInfoResponse...");
                AditUserInfoResponse response = (AditUserInfoResponse) result;

                List<AditUserInfo> activeUsers = response.getActiveUserList();
                if(activeUsers == null){
                    log.info("ACTIVE USERS list is NULL!");
                } else {
                    log.info("ACTIVE USERS list size: " + activeUsers.size());
                }

                List<AditUserInfo> unreqistredUsers = response.getUnreqistredUserList();
                if(unreqistredUsers == null){
                    log.info("UNREGISTRED USERS list is NULL!");
                } else {
                    log.info("UNREGISTRED USERS list size: " + unreqistredUsers.size());
                    Set<String> userList = new HashSet<>();
                    for(AditUserInfo info : unreqistredUsers){
                        log.info("USER ID-CODE: " + info.getUserIdCode() + "; message: " + info.getMessages());
                        userList.add(info.getUserIdCode());
                    }
                    return userList;
                }
            } else {
                log.error("BODY IS INSTANCE OF UNKNOWN CLASS: " + result.getClass().toString());
                throw new Exception("UNKNOWN RESPONSE OBJECT ERROR! CLASS: " + result.getClass().toString());
            }

        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public AditSendStatusVIResponse getSendStatuses(Set<String> docIds, String userIdCode) {
        String seachUri = aditAdapterService.getUri() + "/api/xtee/adit/getSendStatusV1";
        log.info("getSendStatuses()... searchUri: " + seachUri);
        return getSendStatus(seachUri, docIds, userIdCode);
    }


    private AditSendStatusVIResponse getSendStatus(String uri, Set<String> docIds, String userIdCode){
        AditDocSendStatusRequest request = new AditDocSendStatusRequest();
        request.setDocIds(docIds);
        request.setSystemId(aditAdapterService.getRegCode());
        request.setUserIdCode(userIdCode);

        try {
            Object result = makeObjectPostRequest(AditSendStatusVIResponse.class, request, uri);
            if(result instanceof AditSendStatusVIResponse){
                AditSendStatusVIResponse response = (AditSendStatusVIResponse) result;
                return response;
            } else {
                log.error("UNKNOWN RESPONSE OBJECT ERROR! CLASS: " + result.getClass());
            }

        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }


}
