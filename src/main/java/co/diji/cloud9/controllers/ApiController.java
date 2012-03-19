package co.diji.cloud9.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
public class ApiController {
	
	private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
	
    @ResponseBody
	@RequestMapping(value = "/apps/{app}", method = RequestMethod.GET)
	public void exportApp(@PathVariable String app) {
		logger.info("api exportApp: " + app);
	}
	
    @ResponseBody
	@RequestMapping(value = "/apps/{app}", method = RequestMethod.POST)
	public void importApp(@PathVariable String app) {
		logger.info("api importApp: " + app);
	}
	
    @ResponseBody
	@RequestMapping(value = "/**")
	public void passthough(HttpServletRequest request, HttpServletResponse response) {
		logger.info(request.getMethod() + ": api passthrough (" + request.getRequestURI() + ")");
	}
	
}
