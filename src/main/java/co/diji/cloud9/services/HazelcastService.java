package co.diji.cloud9.services;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HazelcastService {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);

    @Autowired
    private ConfigService configService;

    private HazelcastInstance hazelcast;

    @PostConstruct
    public void booststrap() {
        logger.trace("in bootstrap");

        // read default cloud9 hazelcast settings
        Config conf = new ClasspathXmlConfig("hazelcast-cloud9.xml");
        Settings nodeSettings = configService.getNodeSettings();

        // set our hazelcast instance name to cloud9 so we can configure the
        // session caching to use the same instance
        conf.setInstanceName("cloud9");

        // hazelcast group name should be the same as our elasticsearch cluster name
        conf.getGroupConfig().setName(nodeSettings.get("cluster.name"));

        // if elasticsearch is in unicast mode, use the same settings for hazelcast
        boolean useMulticast = nodeSettings.getAsBoolean("discovery.zen.ping.multicast.enabled", true);
        logger.debug("useMulticast: {}", useMulticast);
        if (!useMulticast) {
            logger.debug("disabling multicast");
            conf.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            logger.debug("enabling tcp/ip (unicast)");
            TcpIpConfig tcp = conf.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            for (String host : nodeSettings.getAsArray("discovery.zen.ping.unicast.hosts")) {
                logger.debug("adding member: {}", host);
                tcp.addMember(host);
            }
        }

        // start hazelcast
        logger.debug("starting hazelcast");
        hazelcast = Hazelcast.newHazelcastInstance(conf);
    }

    @PreDestroy
    public void shutdown() {
        Hazelcast.shutdownAll();
    }
}