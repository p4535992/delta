package ee.webmedia.mobile.alfresco.search;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ee.webmedia.mobile.alfresco.common.AbstractBaseController;

@Controller
@RequestMapping("/search")
public class SearchController extends AbstractBaseController {

    private static final long serialVersionUID = 1L;

    @RequestMapping(method = RequestMethod.GET)
    public String filter(Model model) {
        setup(model);
        setPageTitle(model, translate("site.search"));

        return "search/filter";
    }

    @RequestMapping(value = "results")
    public String results(Model model) {
        setup(model);
        setPageTitle(model, "site.search.results");

        return "search/filter";
    }

}
