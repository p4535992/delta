package ee.webmedia.alfresco.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by 38101230038 on 29.12.2016.
 */
@Controller
@RequestMapping("/rabis")
public class RabisDocumentsController {

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public String get() {
        //tuleb implementeerida sarnane funktsionaalsus nagu ee.webmedia.alfresco.adr.service.AdrServiceImpl.koikDokumendidLisatudMuudetudV2()
        return "{\"data\": [{\"id\": 123, \"name\": \"...\"]}";
    }
}
