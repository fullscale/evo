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
@RequestMapping("/cloud9/overview")
public class OverviewController {
	
	private static final Logger logger = LoggerFactory.getLogger(OverviewController.class);
	
    @ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	public String get() {
		logger.info("overview get");
        return "<html><head><title>Overview</title></head><body><p>This is the Overview</p></body></html>";
	}
	
}
