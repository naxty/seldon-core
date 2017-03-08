package io.seldon.clustermanager.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seldon.clustermanager.component.ZookeeperManager;
import io.seldon.clustermanager.pb.ProtoBufUtils;
import io.seldon.protos.DeploymentProtos.DeploymentDef;

public class ZookeeperManagerImpl implements ZookeeperManager {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperManagerImpl.class);

    private CuratorFramework curator = null;

    public void init() throws Exception {
        logger.info("init");
        String zookeeperConnectionString = "localhost";
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        curator = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        curator.start();

        try {
            byte[] v = curator.getData().forPath("/"); // Check we can get the root node to see if all is working
            logger.info("Sucessfully passed root node data check");
        } catch (Exception e) {
            throw e;
        }
    }

    public void cleanup() throws Exception {
        logger.info("cleanup");
        if (curator != null) {
            curator.close();
        }
    }

    @Override
    public void persistDeployment(DeploymentDef deploymentDef) throws Exception {
        String json = ProtoBufUtils.toJson(deploymentDef, true);
        byte[] json_bytes = json.getBytes("UTF-8");

        final String seldonDeploymentId = Long.toString(deploymentDef.getId());
        String deployment_node_path = String.format("/deployments/%s", seldonDeploymentId);

        if (curator.checkExists().forPath(deployment_node_path) == null) {
            // Create node
            curator.create().creatingParentsIfNeeded().forPath(deployment_node_path, json_bytes);
            logger.debug(String.format("[CREATE] [%s] [%s]", deployment_node_path, json));
        } else {
            // Update node
            curator.setData().forPath(deployment_node_path, json_bytes);
            logger.debug(String.format("[UPDATE] [%s] [%s]", deployment_node_path, json));
        }

    }

}
