package ee.webmedia.mobile.alfresco.workflow;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MyTasksController {

    @RequestMapping("/my-tasks")
    public String getMyTasks() {
        return "home";
    }

}
