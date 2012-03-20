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
public class TypeController {

    private static final Logger logger = LoggerFactory.getLogger(TypeController.class);

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.GET)
    public void get(@PathVariable String collection, @PathVariable String type) {
        logger.info("type get collection:" + collection + " type:" + type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.POST)
    public void create(@PathVariable String collection, @PathVariable String type) {
        logger.info("type create collection:" + collection + " type:" + type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection, @PathVariable String type) {
        logger.info("type update collection:" + collection + " type:" + type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String collection, @PathVariable String type) {
        logger.info("type delete collection:" + collection + " type:" + type);
    }

}
