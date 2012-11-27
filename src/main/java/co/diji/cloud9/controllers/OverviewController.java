package co.diji.cloud9.controllers;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OverviewController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(OverviewController.class);

    @ResponseBody
    @RequestMapping(value = {"/", "/evo"}, method = RequestMethod.GET)
    public ModelAndView getIndex(ModelMap model) {
        logger.entry();
        RedirectView redirect = new RedirectView("/evo/overview");
        redirect.setExposeModelAttributes(false);
        logger.exit();
        return new ModelAndView(redirect);
    }

    @ResponseBody
    @RequestMapping(value = {"/evo/overview"}, method = RequestMethod.GET)
    public ModelAndView get(ModelMap model) {
        logger.entry();
        logger.exit();
        return new ModelAndView("overview", model);
    }

}
