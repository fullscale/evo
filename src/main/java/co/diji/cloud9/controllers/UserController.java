package co.diji.cloud9.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/cloud9/user")
public class UserController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public void get(@PathVariable String userId) {
        logger.info("user get userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.POST)
    public void create(@PathVariable String userId) {
        logger.info("user create userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public void update(@PathVariable String userId) {
        logger.info("user update userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String userId) {
        logger.info("user delete userId:" + userId);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView list(ModelMap model) {
        logger.trace("enter controller=user action=list");
        return new ModelAndView("users", model);
    }

}
