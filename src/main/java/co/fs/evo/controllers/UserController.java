package co.fs.evo.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import co.fs.evo.security.EvoUser;
import co.fs.evo.security.EvoUserDetailsService;

@Controller
@RequestMapping("/evo/user")
public class UserController extends BaseController {

    private static final XLogger logger = XLoggerFactory.getXLogger(UserController.class);
    
    @Autowired protected EvoUserDetailsService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    @ResponseBody
    @RequestMapping(
    		value = "/{userId}", 
    		method = RequestMethod.GET, 
    		produces = "application/json")
    public Map<String, Object> get(@PathVariable String userId) {
        logger.entry(userId);
        
        EvoUser user = (EvoUser)userService.loadUserByUsername(userId);
        logger.exit();
        return user.asMap();
    }

    @ResponseBody
    @RequestMapping(
    		value = "/{userId}", 
    		method = RequestMethod.POST, 
    		consumes = "application/json", 
    		produces = "application/json")
    public Map<String, Object> create(
    		@PathVariable String userId, 
    		@RequestBody Map<String, Object> data) {
    	
        logger.entry(userId);
        
        String uid = UUID.randomUUID().toString();
        
        EvoUser user = new EvoUser();
        user.setUsername((String)data.get("username"));
        user.setPassword(
        		passwordEncoder.encodePassword(
        				(String)data.get("password"), uid));
        user.setUid(uid);
        user.setAuthorities(new String[]{"supervisor"});
        
        userService.createUser(user);
        userService.userExists(userId);
        
        Map<String, Object> resp = new HashMap<String, Object>();
        
        if (userService.userExists(userId)) {
        	resp.put("status", "ok");
        	resp.put("response", user.asMap());
        } else {
        	resp.put("status", "error");
        	resp.put("response", new HashMap<String, Object>());
        }
        
        logger.exit();
        return resp;
    }

    @SuppressWarnings("unchecked")
	@ResponseBody
    @RequestMapping(
    		value = "/{userId}", 
    		method = RequestMethod.PUT,
    		consumes = "application/json", 
    		produces = "application/json")
    public Map<String, Object> update(@PathVariable String userId, @RequestBody Map<String, Object> data) {
        
    	logger.entry(userId);
        
    	EvoUser user = (EvoUser)userService.loadUserByUsername(userId);
    	
    	if (data.containsKey("password")) {
    		user.setPassword(
    				passwordEncoder.encodePassword(
    						(String)data.get("password"), 
    						data.get("password")
    				));
    	}
    	
    	if (data.containsKey("authorities")) {
    		user.setAuthorities((ArrayList<String>)data.get("authorities"));
    	}
    	
    	userService.updateUser(user);
    	
    	Map<String, Object> resp = new HashMap<String, Object>();
    	resp.put("status", "ok");
    	resp.put("response", user.asMap());
    	
        logger.exit();
        return resp;
    }

    @ResponseBody
    @RequestMapping(
    		value = "/{userId}", 
    		method = RequestMethod.DELETE,
    		produces = "application/json")
    public Map<String, Object> delete(@PathVariable String userId) {
        logger.entry(userId);
        
        userService.deleteUser(userId);
        
        Map<String, Object> resp = new HashMap<String, Object>();
        
        if (!userService.userExists(userId)) {
        	resp.put("status", "ok");
        } else {
        	resp.put("status", "error");
        }
        
        logger.exit();
        return resp;
    }

    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(
    		method = RequestMethod.GET)
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
