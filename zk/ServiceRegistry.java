package com.demo.rpc.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

public class ServiceRegistry {
    private final CuratorFramework client;
    private static final String REGISTRY_PATH = "/rpc-demo";

    public ServiceRegistry(String zkAddress) {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
    }

    public void register(String serviceName, String address) throws Exception {
        String servicePath = REGISTRY_PATH + "/" + serviceName;
        if (client.checkExists().forPath(servicePath) == null) {
            client.create().creatingParentsIfNeeded().forPath(servicePath);
        }
        String nodePath = servicePath + "/node-";
        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(nodePath, address.getBytes());
        System.out.println("Service registered: " + serviceName + " -> " + address);
    }
}
