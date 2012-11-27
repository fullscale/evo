package co.diji.cloud9.controllers;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/evo/user")
public class UserController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(UserController.class);

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public void get(@PathVariable String userId) {
        logger.entry(userId);
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.POST)
    public void create(@PathVariable String userId) {
        logger.entry(userId);
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public void update(@PathVariable String userId) {
        logger.entry(userId);
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String userId) {
        logger.entry(userId);
        logger.exit();
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView list(ModelMap model) {
        logger.entry();
        logger.exit();
        return new ModelAndView("users", model);
    }

}
