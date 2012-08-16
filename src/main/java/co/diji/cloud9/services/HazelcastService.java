package co.diji.cloud9.services;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HazelcastService {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);

    private HazelcastInstance hazelcast;

    @PostConstruct
    public void booststrap() {
        logger.trace("in bootstrap");
        //Config conf = new ClasspathXmlConfig("hazelcast-cloud9.xml");
        //conf.setInstanceName("cloud9");
        //hazelcast = Hazelcast.newHazelcastInstance(conf);
    }

    @PreDestroy
    public void shutdown() {
        //Hazelcast.shutdownAll();
    }
}