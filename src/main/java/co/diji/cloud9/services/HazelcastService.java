package co.diji.cloud9.services;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spring.context.SpringManagedContext;
import com.hazelcast.web.WebFilter;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class HazelcastService {

    private static final XLogger logger = XLoggerFactory.getXLogger(HazelcastService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ApplicationContext applicationContext;

    private HazelcastInstance hazelcast;

    @PostConstruct
    public void bootstrap() {
        logger.entry();

        // read default cloud9 hazelcast settings
        Config conf = new ClasspathXmlConfig("hazelcast-cloud9.xml");

        // set our hazelcast instance name to cloud9 node name
        conf.setInstanceName(configService.get("node.name"));

        // hazelcast group name should be the same as our elasticsearch cluster name
        conf.getGroupConfig().setName(configService.get("cluster.name"));

        // if unicast is enabled, do the same for hazelcast
        boolean useUnicast = configService.getBool("network.unicast.enabled", false);
        logger.debug("useUnicast: {}", useUnicast);
        if (useUnicast) {
            logger.debug("disabling multicast");
            conf.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            logger.debug("enabling tcp/ip (unicast)");
            TcpIpConfig tcp = conf.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            for (String host : configService.getArray("network.unicast.hosts", new String[]{})) {
                logger.debug("adding member: {}", host);
                tcp.addMember(host);
            }
        }

        // make sure hazelcast starts on the "publish" address that elasticsearch nodes communicate on
        logger.debug("setting interface");
        conf.getNetworkConfig().getInterfaces().setEnabled(true).addInterface(searchService.getPublishAddress());

        // configure hazelcast to be "spring aware"
        logger.debug("enabling spring managed context");
        SpringManagedContext springContext = new SpringManagedContext();
        springContext.setApplicationContext(applicationContext);
        conf.setManagedContext(springContext);

        // start hazelcast
        logger.debug("starting hazelcast");
        hazelcast = Hazelcast.newHazelcastInstance(conf);
        
        logger.exit();
    }

    public IMap getMap(String name) {
        return hazelcast.getMap(name);
    }

    public String getNodeId() {
        return hazelcast.getCluster().getLocalMember().getUuid();
    }

    @PreDestroy
    public void shutdown() {
        logger.entry();
        hazelcast.getLifecycleService().shutdown();
        logger.exit();
    }

    @Bean
    public WebFilter hazelcastWebFilter() {
        logger.entry();
        Properties sessionCacheConfig = new Properties();
        sessionCacheConfig.put("instance-name", hazelcast.getName()); // the name of our hazelcast instance
        sessionCacheConfig.put("map-name", "session-cache");
        sessionCacheConfig.put("sticky-session", "false");
        sessionCacheConfig.put("cookie-name", "cloud9-sid");
        sessionCacheConfig.put("shutdown-on-destroy", "false");
        logger.debug("sessionCacheConfig: {}", sessionCacheConfig);

        logger.exit();
        return new WebFilter(sessionCacheConfig);
    }
}