package co.diji.cloud9.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OverviewController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OverviewController.class);

    @ResponseBody
    @RequestMapping(value = {"/", "/cloud9"}, method = RequestMethod.GET)
    public ModelAndView getIndex(ModelMap model) {
        RedirectView redirect = new RedirectView("/cloud9/overview");
        redirect.setExposeModelAttributes(false);
        return new ModelAndView(redirect);
    }

    @ResponseBody
    @RequestMapping(value = {"/cloud9/overview"}, method = RequestMethod.GET)
    public ModelAndView get(ModelMap model) {
        logger.trace("enter controller=overview action=get");
        return new ModelAndView("overview", model);
    }

}
