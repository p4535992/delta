package ee.smit.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class RestUtil {

    protected final static Log log = LogFactory.getLog(RestUtil.class);

    private static RestTemplate restTemplate = new RestTemplate();


    /**
     *
     * @param searchUri
     * @return
     * @throws Exception
     */
    public static JSONObject searchByGet(String searchUri) throws JSONException {

        log.debug("SEARCH URI: " + searchUri);
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> result = restTemplate.getForEntity(searchUri, Map.class);
        JSONObject responseJson = new JSONObject(result.getBody());

        logResponseEntityMap(result, responseJson);

        return responseJson;
    }

    private static HttpHeaders getNewHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static Object makeObjectPostRequest(Class responseObject, Object obj, String postUri) throws URISyntaxException {
        log.debug("Request post uri: " + postUri);
        URI uri = new URI(postUri);
        return restTemplate.postForObject(uri, obj, responseObject);
    }


    public static JSONObject makePostRequest(JSONObject request, String postUri) throws JSONException {
        log.debug("Request post uri: " + postUri);
        HttpEntity<String> entity = new HttpEntity<String>(request.toString(), getNewHeaders());
        ResponseEntity<String> result = restTemplate.postForEntity(postUri, entity, String.class);
        log.debug("POST REQUEST status: " + result.getStatusCode());
        log.debug("Has body: " + result.hasBody());

        String body = result.getBody();
        log.debug("Body length: " + body.length());
        JSONObject responseJson = new JSONObject(body);
        return responseJson;
    }


    public static void logResponseEntityMap(ResponseEntity<Map> result, JSONObject json){
        if(result != null){
            log.debug("RESULT STATUS CODE: name(): " + result.getStatusCode().name());
            log.debug("RESULT STATUS CODE: getReasonPhrase(): " + result.getStatusCode().getReasonPhrase());
            log.debug("RESULT STATUS CODE: series().name(): " + result.getStatusCode().series().name());
            if(json != null){
                log.debug("JSON FOUND.....check objeckts...");
                try{
                    JSONArray jsonArray = json.names();
                    int length = jsonArray.length();
                    log.debug("Found: " + length);

                    for(int i = 0; i < length; i++){
                        //log.debug("JSON Array names value: " + jsonArray.get(i));
                    }
                } catch (Exception ex){{
                    log.error(ex.getMessage(), ex);
                }

                }

            } else {
                log.debug("JSON RESPONSE IS NULL!");
            }
        } else {
            log.debug("RESULT IS NULL!");
        }
    }


}
