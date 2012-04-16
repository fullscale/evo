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
public class CollectionController {

    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

    @Autowired
    protected SearchService searchService;
    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.GET)
    public void get(@PathVariable String collection) {
        logger.info("collection get collection:" + collection);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.POST)
    public void create(@PathVariable String collection) {
        logger.info("collection create collection:" + collection);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.PUT)
    public void update(@PathVariable String collection) {
        logger.info("collection update collection:" + collection);
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String collection) {
        logger.info("collection delete collection:" + collection);
    }

}
