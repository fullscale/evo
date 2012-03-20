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
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.GET)
    public void get(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.info("document get collection:" + collection + " type:" + type + " id:" + id);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.POST)
    public void create(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.info("document create collection:" + collection + " type:" + type + " id:" + id);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.info("document update collection:" + collection + " type:" + type + " id:" + id);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.info("document delete collection:" + collection + " type:" + type + " id:" + id);
    }

}
