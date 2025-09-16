public class NettyRemotingClient {

    private final Logger logger = LoggerFactory.getLogger(NettyRemotingClient.class);

    /**
     * client bootstrap
     */
    private final Bootstrap bootstrap = new Bootstrap();

    /**
     * encoder
     */
    private final NettyEncoder encoder = new NettyEncoder();

    /**
     * channels
     */
    private final ConcurrentHashMap<Host, Channel> channels = new ConcurrentHashMap<>(128);

    /**
     * started flag
     */
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    /**
     * worker group
     */
    private final EventLoopGroup workerGroup;

    /**
     * client config
     */
    private final NettyClientConfig clientConfig;

    /**
     * saync semaphore
     */
    private final Semaphore asyncSemaphore = new Semaphore(200, true);

    /**
     * callback thread executor
     */
    private final ExecutorService callbackExecutor;

    /**
     * client handler
     */
    private final NettyClientHandler clientHandler;

    /**
     * response future executor
     */
    private final ScheduledExecutorService responseFutureExecutor;

    /**
     * client init
     *
     * @param clientConfig client config
     */
    public NettyRemotingClient(final NettyClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        if (NettyUtils.useEpoll()) {
            this.workerGroup = new EpollEventLoopGroup(clientConfig.getWorkerThreads(), new ThreadFactory() {
                private final AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyClient_%d", this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            this.workerGroup = new NioEventLoopGroup(clientConfig.getWorkerThreads(), new ThreadFactory() {
                private final AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyClient_%d", this.threadIndex.incrementAndGet()));
                }
            });
        }
        this.callbackExecutor = new ThreadPoolExecutor(5, 10, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1000), new NamedThreadFactory("CallbackExecutor", 10),
                new CallerThreadExecutePolicy());
        this.clientHandler = new NettyClientHandler(this, callbackExecutor);

        this.responseFutureExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ResponseFutureExecutor"));

        this.start();
    }

    /**
     * start
     */
    private void start() {

        this.bootstrap
                .group(this.workerGroup)
                .channel(NettyUtils.getSocketChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, clientConfig.isSoKeepalive())
                .option(ChannelOption.TCP_NODELAY, clientConfig.isTcpNoDelay())
                .option(ChannelOption.SO_SNDBUF, clientConfig.getSendBufferSize())
                .option(ChannelOption.SO_RCVBUF, clientConfig.getReceiveBufferSize())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfig.getConnectTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("client-idle-handler", new IdleStateHandler(Constants.NETTY_CLIENT_HEART_BEAT_TIME, 0, 0, TimeUnit.MILLISECONDS))
                                .addLast(new NettyDecoder(), clientHandler, encoder);
                    }
                });
        this.responseFutureExecutor.scheduleAtFixedRate(ResponseFuture::scanFutureTable, 5000, 1000, TimeUnit.MILLISECONDS);
        isStarted.compareAndSet(false, true);
    }
