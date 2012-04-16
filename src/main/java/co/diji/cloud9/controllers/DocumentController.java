package co.diji.cloud9.controllers;

import java.util.HashMap;
import java.util.Map;

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
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    protected SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> get(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.trace("in controller=document action=get collection:{} type:{} id:{}", new Object[]{collection, type, id});
        Map<String, Object> resp = new HashMap<String, Object>();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.POST, produces = "application/json")
    public Map<String, Object> create(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.trace("in controller=document action=create collection:{} type:{} id:{}", new Object[]{collection, type, id});
        Map<String, Object> resp = new HashMap<String, Object>();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.PUT, produces = "application/json")
    public Map<String, Object> update(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.trace("in controller=document action=update collection:{} type:{} id:{}", new Object[]{collection, type, id});
        Map<String, Object> resp = new HashMap<String, Object>();
        return resp;
    }

    @ResponseBody
    @RequestMapping(value = "/{collection}/{type}/{id}", method = RequestMethod.DELETE, produces = "application/json")
    public Map<String, Object> delete(@PathVariable String collection, @PathVariable String type, @PathVariable String id) {
        logger.trace("in controller=document action=delete collection:{} type:{} id:{}", new Object[]{collection, type, id});
        Map<String, Object> resp = new HashMap<String, Object>();
        return resp;
    }

}
