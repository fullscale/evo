package co.fs.evo.controllers;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.fs.evo.security.EvoUserDetailsService;

@Controller
@RequestMapping("/evo/user")
public class UserController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(UserController.class);
    
    @Autowired protected EvoUserDetailsService userService;

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

    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView list(ModelMap model) {
        logger.entry();
        
        JSONArray users = new JSONArray();
        List<Map<String, Object>> userList = userService.listUsers();
        users.addAll(userList);

        model.put("usersData", users.toJSONString());
        
        logger.exit();
        return new ModelAndView("users", model);
    }

}
