package co.fs.evo.controllers;

import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;

import co.fs.evo.services.ConfigService;
import co.fs.evo.services.SearchService;

public class BaseController {

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected ConfigService config;

    @ModelAttribute("cluster")
    public ClusterHealthResponse getClusterHealth() {
        return searchService.getClusterHealth();
    }

    @ModelAttribute("stats")
    public Map<String, NodeStats> getNodeStats() {
        return searchService.getNodeStats();
    }

    @ModelAttribute("nodes")
    public Map<String, NodeInfo> getNodeInfo() {
        return searchService.getNodeInfo();
    }

    @ModelAttribute("status")
    public Map<String, IndexStatus> getCollectionStatus() {
        return searchService.getCollectionStatus();
    }

    @ModelAttribute("count")
    public long getDocumentCount() {
        return searchService.getTotalCollectionDocCount();
    }

    @ModelAttribute("build")
    public String getBuildNumber() {
        return config.get("build");
    }
}
