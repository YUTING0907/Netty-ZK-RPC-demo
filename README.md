# Netty-ZK-RPC-demo
该项目为学习利用Netty和zookeeper实现的RPC通信


netty-zk-rpc-ds-demo/
├── pom.xml
└── src/main/java/com/demo/rpc/
    ├── command/
    │   ├── Command.java
    │   ├── CommandType.java
    │   └── TaskExecuteAckCommand.java
    ├── master/
    │   └── MasterServer.java
    ├── worker/
    │   └── WorkerServer.java
    ├── zk/
    │   ├── ServiceRegistry.java
    │   └── ServiceDiscovery.java
    └── DemoApp.java
