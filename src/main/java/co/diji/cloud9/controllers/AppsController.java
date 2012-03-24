package co.diji.cloud9.controllers;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppsController {

    private static final Logger logger = LoggerFactory.getLogger(AppsController.class);

    @ResponseBody
    @RequestMapping(value = "/cloud9/ide/{app}", method = RequestMethod.GET)
    public void showIde(@PathVariable String app) {
        logger.info("apps showIde app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps", method = RequestMethod.GET)
    public void list() {
        logger.info("apps list");
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.GET)
    public void listContentTypes(@PathVariable String app) {
        logger.info("apps listContentTypes app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.POST)
    public void createApp(@PathVariable String app) {
        logger.info("apps createApp app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}", method = RequestMethod.DELETE)
    public void deleteApp(@PathVariable String app) {
        logger.info("apps deleteApp app:" + app);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}", method = RequestMethod.GET)
    public void listResources(@PathVariable String app, @PathVariable String dir) {
        logger.info("apps listResources app:" + app + " dir:" + dir);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.GET)
    public void getResourceFromDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps getResourceFromDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.POST)
    public void createResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps createResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.PUT)
    public void updateResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps updateResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/{resource}", method = RequestMethod.DELETE)
    public void deleteResourceInDir(@PathVariable String app, @PathVariable String dir, @PathVariable String resource) {
        logger.info("apps deleteResourceInDir app:" + app + " dir:" + dir + " resource:" + resource);
    }

    @ResponseBody
    @RequestMapping(value = "/cloud9/apps/{app}/{dir}/_rename", method = RequestMethod.PUT)
    public void renameResource(@PathVariable String app, @PathVariable String dir) {
        logger.info("apps renameResource app:" + app + " dir:" + dir);
    }

    @ResponseBody
    //@RequestMapping(value = "/{app}/{dir}/{resource}")
    @RequestMapping(value = "/{app:!(css|images|js)}/{dir}/{resource}")
    public void processResource(@PathVariable String app, @PathVariable String dir, @PathVariable String resource,
            HttpServletRequest request) {
        logger.info(request.getMethod() + ": apps processResource app:" + app + " dir:" + dir + " resource:" + resource);
    }

}
