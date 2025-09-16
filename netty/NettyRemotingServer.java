public class NettyRemotingServer {

    private final Logger logger = LoggerFactory.getLogger(NettyRemotingServer.class);

    /**
     * server bootstrap
     */
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    /**
     * default executor
     */
    private final ExecutorService defaultExecutor = Executors.newFixedThreadPool(Constants.CPUS);

    /**
     * boss group
     */
    private final EventLoopGroup bossGroup;

    /**
     * worker group
     */
    private final EventLoopGroup workGroup;

    /**
     * server config
     */
    private final NettyServerConfig serverConfig;

    /**
     * server handler
     */
    private final NettyServerHandler serverHandler = new NettyServerHandler(this);

    /**
     * started flag
     */
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    /**
     * Netty server bind fail message
     */
    private static final String NETTY_BIND_FAILURE_MSG = "NettyRemotingServer bind %s fail";

    /**
     * server init
     *
     * @param serverConfig server config
     */
    public NettyRemotingServer(final NettyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        ThreadFactory bossThreadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("NettyServerBossThread_%s").build();
        ThreadFactory workerThreadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("NettyServerWorkerThread_%s").build();
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup(1, bossThreadFactory);
            this.workGroup = new EpollEventLoopGroup(serverConfig.getWorkerThread(), workerThreadFactory);
        } else {
            this.bossGroup = new NioEventLoopGroup(1, bossThreadFactory);
            this.workGroup = new NioEventLoopGroup(serverConfig.getWorkerThread(), workerThreadFactory);
        }
    }

    /**
     * server start
     */
    public void start() {
        if (isStarted.compareAndSet(false, true)) {
            this.serverBootstrap
                    .group(this.bossGroup, this.workGroup)
                    .channel(NettyUtils.getServerSocketChannelClass())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, serverConfig.getSoBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, serverConfig.isSoKeepalive())
                    .childOption(ChannelOption.TCP_NODELAY, serverConfig.isTcpNoDelay())
                    .childOption(ChannelOption.SO_SNDBUF, serverConfig.getSendBufferSize())
                    .childOption(ChannelOption.SO_RCVBUF, serverConfig.getReceiveBufferSize())
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            initNettyChannel(ch);
                        }
                    });

            ChannelFuture future;
            try {
                future = serverBootstrap.bind(serverConfig.getListenPort()).sync();
            } catch (Exception e) {
                logger.error("NettyRemotingServer bind fail {}, exit", e.getMessage(), e);
                throw new RemoteException(String.format(NETTY_BIND_FAILURE_MSG, serverConfig.getListenPort()));
            }
            if (future.isSuccess()) {
                logger.info("NettyRemotingServer bind success at port : {}", serverConfig.getListenPort());
            } else if (future.cause() != null) {
                throw new RemoteException(String.format(NETTY_BIND_FAILURE_MSG, serverConfig.getListenPort()), future.cause());
            } else {
                throw new RemoteException(String.format(NETTY_BIND_FAILURE_MSG, serverConfig.getListenPort()));
            }
        }
    }
