package com.demo.rpc.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.List;
import java.util.Random;

public class ServiceDiscovery {
    private final CuratorFramework client;
    private static final String REGISTRY_PATH = "/rpc-demo";

    public ServiceDiscovery(String zkAddress) {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
    }

    public String discover(String serviceName) throws Exception {
        String servicePath = REGISTRY_PATH + "/" + serviceName;
        List<String> nodes = client.getChildren().forPath(servicePath);
        if (nodes == null || nodes.isEmpty()) {
            throw new RuntimeException("No node available for " + serviceName);
        }
        String node = nodes.get(new Random().nextInt(nodes.size()));
        byte[] data = client.getData().forPath(servicePath + "/" + node);
        return new String(data);
    }
}
