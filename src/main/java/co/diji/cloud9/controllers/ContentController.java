package co.diji.cloud9.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/cloud9/content")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/publish", method = RequestMethod.GET)
    public void add(@PathVariable String collection, @PathVariable String type) {
        logger.info("content add collection:" + collection + " type:" + type);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public void get() {
        logger.info("content get");
    }

}
