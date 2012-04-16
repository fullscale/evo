package co.diji.cloud9.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import co.diji.cloud9.services.SearchService;

@Controller
@RequestMapping("/cloud9/content")
public class TypeController {

    private static final Logger logger = LoggerFactory.getLogger(TypeController.class);

    @Autowired
    protected SearchService searchService;
    
    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.GET)
    public void get(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=get collection: {} type:{}", collection, type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.POST)
    public void create(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=create collection:{} type:{}", collection, type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=update collection:{} type:{}", collection, type);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String collection, @PathVariable String type) {
        logger.trace("in controller=type action=delete collection:{} type:{}", collection, type);
    }

}
