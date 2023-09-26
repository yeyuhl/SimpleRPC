> 在做学校云计算课程的实验中，涉及到了许多分布式相关的知识。由于lab2我是使用Netty取巧完成，而lab3涉及到RPC相关知识，因此实现一个简单的RPC框架（基于Netty+Hessian+Zookeeper/Nacos）来深入学习。

# RPC介绍

**RPC（Remote Procedure Call）** 即远程过程调用，通过名字我们就能看出RPC关注的是远程调用而非本地调用。RPC可以帮助我们**调用远程计算机上某个服务的方法，这个过程就像调用本地方法一样简单**。之所以要使用RPC，是因为在分布式系统中，两个不同的服务器上的服务提供的方法不在一个内存空间，所以需要通过网络编程才能传递方法调用所需要的参数。并且，方法调用的结果也需要通过网络编程来接收。但是，如果我们自己手动网络编程来实现这个调用过程的话工作量是非常大的，因为，我们需要考虑底层传输方式（TCP 还是 UDP）、序列化方式等等方面。

# RPC原理

RPC的核心功能抽象出来为五个部分实现：

1. **客户端**：调用远程方法的一端。
  
2. **客户端Stub（桩）**：代理类，将调用方法、类、方法参数等信息传递到服务端。
  
3. **网络传输**：网络传输就是把调用的方法的信息比如说参数之类的数据传输到服务端，然后服务端执行完之后再把返回结果通过网络传输回来。网络传输的实现方式有很多种，比如最基本的Socket或者性能以及封装更加优秀的Netty。
  
4. **服务端 Stub（桩）**：这里的服务端 Stub 实际指的就是接收到客户端执行方法的请求后，去执行对应的方法然后返回结果给客户端的类。
  
5. **服务端（服务提供端）**：提供远程方法的一端。
  

原理图如下：

![](https://my-blog-to-use.oss-cn-beijing.aliyuncs.com/18-12-6/37345851.jpg)

1. 服务消费端（client）以本地调用的方式调用远程服务；
  
2. 客户端 Stub（client stub） 接收到调用后负责将方法、参数等组装成能够进行网络传输的消息体（序列化）：`RpcRequest`；
  
3. 客户端 Stub（client stub） 找到远程服务的地址，并将消息发送到服务提供端；
  
4. 服务端 Stub（桩）收到消息将消息反序列化为 Java 对象: `RpcRequest`；
  
5. 服务端 Stub（桩）根据`RpcRequest`中的类、方法、方法参数等信息调用本地的方法；
  
6. 服务端 Stub（桩）得到方法执行结果并将组装成能够进行网络传输的消息体：`RpcResponse`（序列化）发送至消费方；
  
7. 客户端 Stub（client stub）接收到消息并将消息反序列化为 Java 对象:`RpcResponse` ，这样也就得到了最终结果。
  

# 实现RPC

## 1. RPC框架

> 具体参考：[开发指南 | Apache Dubbo](https://cn.dubbo.apache.org/zh-cn/docsv2.7/dev/)

参考Dubbo的框架图如下：

![](https://yeyu-1313730906.cos.ap-guangzhou.myqcloud.com/PicGo/20230525202520.png)

各节点的说明如下：

- **Provider**：服务提供方
  
- **Consumer**：调用远程服务的服务消费方
  
- **Registry**：服务注册与发现的注册中心
  
- **Monitor**：统计服务调用次数和调用时间的监控中心
  
- **Container**：服务运行的容器
  

运行流程如下：

0. Container启动并初始化，运行Provider。
  
1. Provider启动，向Registry注册自己提供的服务。
  
2. Consumer启动，向Registry订阅自己所需的服务。
  
3. Registry返回Provider的地址列表给Consumer，如果有变更，Registry将基于长连接推送变更数据给Consumer。
  
4. Consumer从Provider的地址列表中，基于软负载均衡算法选一台Provider进行调用，如果调用失败，再选另一台调用。
  
5. Consumer和Provider在内存中累计调用次数和调用时间，定时发送统计数据到Monitor。
  

综上，一个基本的RPC框架如下：

![](https://yeyu-1313730906.cos.ap-guangzhou.myqcloud.com/PicGo/20230525204719.png)

其中需要注意以下事项：

1. **注册中心**：注册中心起到协调服务的作用，使用Zookeeper和Nacos都可以。
  
2. **网络传输**：由于本质上是远程调用，因此离不开网络传输，这里选择Netty。
  
3. **序列化**：传输数据离不开序列化，出于安全性，兼容性和性能考虑首选Hessian2和Protobuf。
  
4. **数据压缩**：为了提高数据的传输速率，我们可以对数据进行压缩，比如使用gzip。
  
5. **动态代理**：为了让远程调用能像本地调用一样简单，因此需要动态代理来屏蔽远程方法调用的细节比如网络传输。
  
6. **负载均衡**：有多台Client发起请求时，不能只让某一台Server来处理请求，这样容易造成服务机崩溃宕机。而负载均衡则是位于用户与服务器组之间的中间者，充当不可见的协调者，确保均等使用所有资源服务器。
  

实现后的代码整体框架如下：

```
SimpleRPC
├── rpc-api —— 通用接口，即要调用的服务的接口
├── rpc-client —— 测试用客户端
├── rpc-common —— 实体对象、工具类等公用类
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── io
│       │   │       └── github
│       │   │           └── yeyuhl
│       │   │               ├── enums —— 枚举类
│       │   │               ├── exception —— 异常类
│       │   │               ├── extension —— 仿照Dubbo设计扩展点加载
│       │   │               ├── factory —— 单例工厂类
│       │   │               └── utils —— 工具类                                      
│       │   └── resources
├── rpc-core —— 框架的核心实现
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── io
│       │   │       └── github
│       │   │           └── yeyuhl
│       │   │               ├── annotation —— 自定义的注解
│       │   │               ├── compress —— 压缩相关
│       │   │               │   └── gzip
│       │   │               ├── config —— 设置相关
│       │   │               ├── loadbalance —— 负载均衡相关
│       │   │               │   └── loadbalancer
│       │   │               ├── provider —— 服务提供者相关
│       │   │               │   └── impl
│       │   │               ├── proxy —— 动态代理相关
│       │   │               ├── registry —— 注册中心
│       │   │               │   ├── nacos —— 使用nacos为注册中心
│       │   │               │   │   └── util
│       │   │               │   └── zk —— 使用zookeeper为注册中心
│       │   │               │       └── util
│       │   │               ├── remoting —— 核心中的核心，网络传输模块
│       │   │               │   ├── constants —— 常量
│       │   │               │   ├── dto —— 网络传输实体类
│       │   │               │   ├── handler —— 处理rpc请求的类
│       │   │               │   └── transport —— 网络传输
│       │   │               │       └── netty —— 采用netty
│       │   │               │           ├── client —— 基于netty的客户端
│       │   │               │           ├── codec —— 数据编码和解码
│       │   │               │           └── server —— 基于netty的服务器
│       │   │               ├── serialize —— 序列化
│       │   │               │   ├── hessian
│       │   │               │   ├── kyro
│       │   │               │   └── protostuff
│       │   │               └── spring —— 实现自定义注解相关
│       │   └── resources
│       │       └── META-INF
│       │           └── extensions —— ExtensionLoader需要的扩展文件的配置目录
└── rpc-server —— 测试用服务器
```

## 2. 代码实现

### rpc-core: remoting（网络传输模块）

#### 介绍

这个模块的首要任务就是让客户端和服务器实现通信，使数据能在二者中传递。如果基于socket实现的话，客户端可以通过socket与服务器建立连接，而服务器可以通过BIO的形式监听socket。当有请求过来时，新开一个线程对其进行处理，处理完之后再发回给客户端。当有n个客户端连接，那就要新建n个线程处理相应请求，并且如果请求还未处理完成，比如要读取某一数据，但当前并无数据可读，线程就会阻塞在读操作。不难看出，当并发量很大的时候，BIO是一种很糟糕的选择，这会导致线程资源的浪费。

BIO的升级版也就是NIO，提供了 `Channel` , `Selector`，`Buffer` 等抽象。它是支持面向缓冲的，基于通道的 IO 操作方法。NIO是基于IO多路复用模型，线程首先发起 select 调用，询问内核数据是否准备就绪，等内核把数据准备好了，用户线程再发起 read 调用。在read 调用的过程（数据从内核空间 -> 用户空间）还是阻塞的。IO 多路复用模型，通过减少无效的系统调用，减少了对 CPU 资源的消耗。并且通过`Selector`（选择器），只需要一个线程便可以管理多个客户端连接，只有当客户端需要的数据准备就绪之后，才会为其服务。

Java原生NIO由于设计问题，编程复杂，并且可能出现空轮询bug影响性能。因此在这基础上进行改进后的Netty，变成了最好的选择。

#### dto

**RpcMessage**，即rpc需要传递的消息（数据），其data部分一般都是RpcResponse或者RpcConstants.PING/PONG（心跳请求/响应）。由于data部分序列化过，因此没必要对整个RpcMessage序列化。

```java
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcMessage {
    /**
     * rpc消息类型
     */
    private byte messageType;

    /**
     * 序列化类型
     */
    private byte codec;

    /**
     * 压缩类型
     */
    private byte compress;

    /**
     * 请求id
     */
    private int requestId;

    /**
     * 请求数据
     */
    private Object data;
}
```

**RpcRequest**，当调用远程方法时，需要先传一个RpcRequest给对方，RpcRequest包含要调用的目标方法和类的名称、参数等数据。另外version字段一般是为了后续不兼容升级提供可能，而group字段一般用于处理一个接口有多个实现类的情况。

```java
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcRequest implements Serializable {
    /**
     * 序列化id
     */
    private static final long serialVersionUID = 2023052601L;

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 要调用的方法的接口名
     */
    private String interfaceName;

    /**
     * 要调用的方法名
     */
    private String methodName;

    /**
     * 要传递的参数
     */
    private Object[] parameters;

    /**
     * 参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 版本号
     */
    private String version;

    /**
     * 同一接口不同实现类的区分标识
     */
    private String group;

    /**
     * 获取rpc服务名
     */
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
```

**RpcResponse**，服务器通过RpcRequest中的相关数据调用到目标服务的目标方法之后，调用结果就通过RpcResponse返回给客户端。

```java
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcResponse<T> implements Serializable {
    /**
     * 序列化id
     */
    private static final long serialVersionUID = 2023052602L;

    /**
     * 处理的请求的id
     */
    private String requestId;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应成功
     */
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (data != null) {
            response.setData(data);
        }
        return response;
    }

    /**
     * 响应失败
     */
    public static <T> RpcResponse<T> fail(RpcResponseCodeEnum rpcResponseCodeEnum) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.getCode());
        response.setMessage(rpcResponseCodeEnum.getMessage());
        return response;
    }
}
```

#### transport

> 先讲述基于Netty的客户端

**ChannelProvider**，用于存放Channel，从中获取Channel。

```java
@Slf4j
public class ChannelProvider {
    /**
     * 用Map存放Channel，key为地址，value为Channel
     */
    private final Map<String, Channel> channelMap;

    /**
     * 构造函数，为了线程安全使用ConcurrentHashMap
     */
    public ChannelProvider() {
        this.channelMap = new ConcurrentHashMap<>();
    }

    /**
     * 根据地址获取Channel
     *
     * @param address 地址
     * @return Channel
     */
    public Channel get(InetSocketAddress address) {
        String key = address.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            // 如果Channel不为空且为活跃状态（即可用），直接返回
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                // 否则从Map中移除
                channelMap.remove(key);
            }
        }
        return null;
    }

    /**
     * 存放Channel
     *
     * @param address 地址
     * @param channel Channel
     */
    public void set(InetSocketAddress address, Channel channel) {
        String key = address.toString();
        channelMap.put(key, channel);
    }

    /**
     * 移除Channel
     *
     * @param address 地址
     */
    public void remove(InetSocketAddress address) {
        String key = address.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
```

**NettyRpcClient**，Netty客户端主要提供了以下两个方法：

- **doConnect**：用于连接服务器（目标方法所在的服务器），并返回对应的Channel。当我们知道了服务器的地址之后，我们就可以通过NettyRpcClient成功连接服务器了（使用Channel传输数据）。
  
- **sendRpcRequest**：用于传输rpc请求（RpcRequest）到服务器。
  

```java
    /**
     * NettyRpcClient的构造函数
     */
    public NettyRpcClient() {
        // 事件循环组
        eventLoopGroup = new NioEventLoopGroup();
        // 启动引导类
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                // 往pipeline中添加日志处理器
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置连接超时时间，5s内没有连接上则连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        // 增加IdleStateHandler，如果5秒内没有写入数据（要发送到服务器的数据），则发送心跳请求
                        channelPipeline.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        channelPipeline.addLast(new RpcMessageEncoder());
                        channelPipeline.addLast(new RpcMessageDecoder());
                        channelPipeline.addLast(new NettyRpcClientHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.NACOS.getName());
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 连接服务器并获取channel，以便向服务器发送rpc消息
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    /**
     * 发送rpc请求
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 构建返回值
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 获取服务地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 获取channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 将返回值放入未处理的请求中
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // 发送成功，打印日志
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    // 发送失败，关闭channel，将异常信息放入返回值中
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }
```

**NettyRpcClientHandler**，自定义客户端ChannelHandler来处理服务器发送的数据，主要提供了以下两种方法：

- **channelRead**：从channel读取服务器返回的消息，如果是心跳消息则打印，如果是响应消息则放入unprocessedRequests中。
  
- **userEventTriggered**：用来处理空闲状态事件的方法，这与Netty心跳机制有关，避免连接断开。
  

关于userEventTriggered方法我们可以展开来说说，首先Netty的Channel可以设置多个Handler，如果是InboundHandler（处理入站相关），在ChannelPipeline（每个Channel都对应一个ChannelPipeline）上是按照从前往后的顺序传递数据，如果是OutboundHandler（处理出站相关），则是按照从后往前的顺序传递数据。如下图所示：

![](https://yeyu-1313730906.cos.ap-guangzhou.myqcloud.com/PicGoYZ1nIW5VTtEBFvs.png)

此外Handler的数据传递规则是如果当前Handler无法对数据进行处理，就会传给下一个Handler，数据处理完成则不传给下一个Handle。当然也可以fireChannelRead把数据继续传给下一个Handler，或者是writeAndFlush写入并刷新数据。由于前面构造NettyRpcClient的时候，往ChannelPipeline里面加了一个IdleStateHandler，每隔五秒进行一次写空闲检测，如果检测到就会触发IdleStateEvent事件。由于它自身只起到检测作用，因此我们需要在自定义的Handler里面重写userEventTriggered这个方法来处理IdleStateEvent事件。

```java
    /**
     * 读取服务器发送的数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            log.info("client receive msg: [{}]", msg);
            // 检查是否是RpcMessage类型的数据
            if (msg instanceof RpcMessage) {
                RpcMessage tmp = (RpcMessage) msg;
                byte messageType = tmp.getMessageType();
                // 如果是心跳消息，则直接打印
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    log.info("heart [{}]", tmp.getData());
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    // 如果是响应消息，则将响应结果放入unprocessedRequests中
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
                    unprocessedRequests.complete(rpcResponse);
                }
            }
        } finally {
            // 释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 当连接的空闲时间（读或者写）太长时，将会触发一个IdleStateEvent事件，利用userEventTrigged方法来处理该事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 查看evt是否是IdleStateEvent事件
        if (evt instanceof IdleStateEvent) {
            // 如果是，获取空闲事件的状态
            IdleState state = ((IdleStateEvent) evt).state();
            // 如果是写空闲，将消息记录到控制台并向远程服务器发送心跳请求
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.PROTOSTUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                // 写入并刷新，增加监听器，如果写入失败则关闭通道
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            // 如果不是，调用super方法来处理
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
```

**UnprocessedRequests**，用于存放未被服务器处理的请求。Netty中Channel的相关操作都是异步进行的，比如上面刚提及的channel.writeAndFlush()返回的其实就是ChannelFuture，设置监听器来监控writeAndFlush是否完成也是ChannelFuture的一个功能。因此，在这个类里面使用的CompletableFuture，实际上也是一个专门用于异步编程的类，在Java 8才被引入。

首先我们先了解Future是什么，这个接口直译叫未来，也很贴切这个接口的功能。Future提供了一种异步并行计算的功能，如果主线程需要执行一个很耗时的计算任务，我们就可以通过Future把这个任务放到异步线程中执行，主线程继续处理其他任务。当未来某个时刻，主线程处理完任务后，再通过Future获取计算结果。但Future的缺点在于，它获取结果只能通过**阻塞**和**轮询**的方式获得。Future有以下五个方法：

- `boolean cancel(boolean mayInterruptIfRunning)`：尝试取消执行任务。
- `boolean isCancelled()`：判断任务是否被取消。
- `boolean isDone()`：判断任务是否已经被执行完成。
- `get()`：等待任务执行完成并获取运算结果。
- `get(long timeout, TimeUnit unit)`：多了一个超时时间。

其中get方法会在线程获取结果之前一直阻塞，而isDone方法会以轮询的形式来查询任务是否完成。假如主线程要去获取运算结果，运算结果还没计算出来，无论调用哪种方法都会让主线程在不断等待而不去执行其他任务，这无疑浪费了线程资源。

而CompletableFuture同时实现了Future和CompletionStage，其中CompletionStage接口描述了一个异步计算的阶段。很多计算可以分成多个阶段或步骤，此时可以通过它将所有步骤组合起来，形成异步计算的流水线。如果想要简单使用CompletableFuture，可以使用其静态工厂方法：

- `runAsync()`：执行CompletableFuture任务，没有返回值。
  
- `supplyAsync()`：执行CompletableFuture任务，有返回值。
  

这两种方法都默认内置线程池，不需要我们再单独创建线程池，并且比起Future支持很多种方法的组合，此处不再展开。值得注意的是，CompletableFuture获取运算结果的get方法，还是阻塞的。

回到这个类上面来，这个类真正用到CompletableFuture的方法其实只有一个，那就是complete方法。当服务器处理完请求，返回一个RpcResponse时，客户端的handler调用complete并将RpcResponse存入map中。complete意味着计算完成，所以每个future的complete只能调用一次。由于还设计了动态代理，当代理类调用完客户端的sendRpcRequest方法后，我们用CompletableFuture<RpcResponse< Object >> completableFuture来承载其返回值。并且调用completableFuture的get方法来获取RpcResponse，此时会阻塞代理类的线程，但是客户端仍然可以正常工作，不会对其造成影响。这样一来，就提高了程序的运行效率。

```java
public class UnprocessedRequests {
    /**
     * 存放请求id和对应的RpcResponse
     */
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    /**
     * 存放请求
     *
     * @param requestId 请求id
     * @param future    请求id对应的响应
     */
    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    /**
     * 完成请求
     *
     * @param rpcResponse 响应
     */
    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            // complete方法只能调用一次，将处理完的响应放入future中
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
```

> 接下来讲述编码器和解码器

我们都学习过传输协议，比如应用层的HTTP，SSH之类。当然这个rpc的协议不需要设计的如此复杂，只是单纯让服务器和客户端之间的通信能遵循某种规定，以便解析数据。

对消息的编码如下：

```
  0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
  +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
  |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
  +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
  |                                                                                                       |
  |                                         body                                                          |
  |                                                                                                       |
  |                                        ... ...                                                        |
  +-------------------------------------------------------------------------------------------------------+
  4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
  1B compress（压缩类型）    1B codec（序列化类型）    4B  requestId（请求的Id）
  body（object类型数据）
```

**RpcMessageEncoder**继承了MessageToByteEncoder，这是Netty自带的一个编码器。我们可以设置一个AtomicInteger ATOMIC_INTEGER，调用其getAndIncrement方法，可以实现request ID自增。其核心方法encode如下：

```java
@Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage, ByteBuf byteBuf) throws Exception {
        try {
            // 写入魔法数
            byteBuf.writeBytes(RpcConstants.MAGIC_NUMBER);
            // 写入版本号
            byteBuf.writeByte(RpcConstants.VERSION);
            // 写入消息长度，由于此时还不知道消息总长，因此先写入4个字节，占位
            byteBuf.writerIndex(byteBuf.writerIndex() + 4);
            // 写入消息类型
            byteBuf.writeByte(rpcMessage.getMessageType());
            // 写入序列化类型
            byteBuf.writeByte(rpcMessage.getCodec());
            // 写入压缩类型
            byteBuf.writeByte(CompressTypeEnum.GZIP.getCode());
            // 写入请求Id，这里使用自增的方式，每次请求都会自增
            byteBuf.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // 构建消息长度
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            // 如果消息类型不是心跳相关消息，则消息长度=消息头长度+消息体长度
            if (rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // 对序列化后的数据压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                // 消息长度增加
                fullLength += bodyBytes.length;
            }
            if (bodyBytes != null) {
                // 将rpcMessage的body数据写入byteBuf
                byteBuf.writeBytes(bodyBytes);
            }
            int writeIndex = byteBuf.writerIndex();
            byteBuf.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            byteBuf.writeInt(fullLength);
            byteBuf.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
```

**RpcMessageDecoder**继承了LengthFieldBasedFrameDecoder，这是Netty自带的一个解码器。在RpcMessageDecoder中，我们需要实现的核心方法是decodeFrame，其中检查魔法数，是为了能快速筛选出是否是无效数据包（不遵守协议），如果不是可以直接关闭连接或者要求重新等其他处理。而检查版本号则是提供一种不兼容式更新，必须按照最新版本协议来发送数据包。

```java
    /**
     * 解码
     */
    private Object decodeFrame(ByteBuf in) {
        // 检查魔法数
        checkMagicNumber(in);
        // 检查版本号
        checkVersion(in);
        // 读取消息长度
        int fullLength = in.readInt();
        // 读取消息类型
        byte messageType = in.readByte();
        // 读取序列化类型
        byte codecType = in.readByte();
        // 读取压缩类型
        byte compressType = in.readByte();
        // 读取请求Id
        int requestId = in.readInt();
        // 创建RpcMessage对象
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId)
                .build();
        // 如果消息类型是心跳消息，则直接返回RpcMessage对象
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        // 如果消息类型是其他消息，则需要读取附加信息
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            // 读取附加信息
            byte[] bs = new byte[bodyLength];
            in.readBytes(bs);
            // 解压
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bs = compress.decompress(bs);
            // 反序列化
            String codecName = SerializationTypeEnum.getName(codecType);
            log.info("codecName: [{}]", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                // 如果消息类型是请求消息，则需要读取请求信息
                RpcRequest tmpValue = serializer.deserialize(bs, RpcRequest.class);
                rpcMessage.setData(tmpValue);
            } else {
                // 如果消息类型是响应消息，则需要读取响应信息
                RpcResponse tmpValue = serializer.deserialize(bs, RpcResponse.class);
                rpcMessage.setData(tmpValue);
            }
        }
        return rpcMessage;
    }
```

> 最后来介绍基于Netty的服务器的实现

**NettyRpcServer**，端口在9998，监听客户端的连接。其中涉及到了一个自定义的hook方法，用于在服务器关闭时执行相关的操作，比如清除注册中心的注册，关闭线程池等。

```java
@Slf4j
@Component
public class NettyRpcServer {
    public static final int PORT = 9998;
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                // 服务端默认线程数为CPU核心数的两倍
                Runtime.getRuntime().availableProcessors() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 服务端启动配置
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启Nagle算法，其作用是尽可能的发送大数据块，减少网络传输。TCP_NODELAY，true就是启用Nagle算法
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启TCP底层心跳机制，true为开启
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度，如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 30s内没有收到客户端请求，就关闭连接
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new RpcMessageEncoder());
                            pipeline.addLast(new RpcMessageDecoder());
                            pipeline.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(host, PORT).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server: ", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
```

**NettyRpcServerHandler**，继承了ChannelInboundHandlerAdapter，是一个自定义的服务器handler，用来处理客户端发送过来的数据。当客户端发送RpcRequest到达服务器后，服务器就会对其进行处理，并把相应RpcResponse传输给客户端。由于这是一个channel的handler，所以本质上它只是在处理channel读写的问题，真正要处理的RpcRequest是交由RpcRequestHandler。而RpcRequestHandler也是调用客户端需要的方法来对数据进行处理，本质上就是层层外包。

```java
/**
     * 读取客户端发送过来的数据并处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                // 如果是心跳请求，就返回心跳响应
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    // 如果是Rpc请求，就调用对应的方法，然后返回该方法的结果
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    // 如果channel可写，就返回结果
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        // 否则返回失败信息
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            // 释放ByteBuf
            ReferenceCountUtil.release(msg);
        }
    }
```

### rpc-core: proxy（动态代理模块）

#### 代理模式

所谓**代理模式**，就是我们**使用代理对象来代替对真实对象(real object)的访问，这样就可以在不修改原目标对象的前提下，提供额外的功能操作，扩展目标对象的功能**。代理模式的主要作用是**扩展目标对象的功能，比如说在目标对象的某个方法执行前后你可以增加一些自定义的操作**。

举个例子：假如房东要将房子出售，于是到房地产中介公司找一个中介（代理），由他来帮房东完成销售房屋，签订合同、网签、贷款过户等等事宜。

#### 静态代理

**静态代理中，我们对目标对象的每个方法的增强都是手动完成的，非常不灵活（比如接口一旦新增加方法，目标对象和代理对象都要进行修改）且麻烦(需要对每个目标类都单独写一个代理类）**。实际应用场景非常非常少，日常开发几乎看不到使用静态代理的场景。

上面我们是从实现和应用角度来说的静态代理，从 JVM 层面来说， **静态代理在编译时就将接口、实现类、代理类这些都变成了一个个实际的 class 文件。**

静态代理实现步骤:

- 定义一个接口及其实现类；
  
- 创建一个代理类同样实现这个接口。
  
- 将目标对象注入进代理类，然后在代理类的对应方法调用目标类中的对应方法。这样的话，我们就可以通过代理类屏蔽对目标对象的访问，并且可以在目标方法执行前后做一些自己想做的事情。
  

```java
// 发送短信的接口
public interface SmsService {
    String send(String message);
}

// 实现发送短信的接口
public class SmsServiceImpl implements SmsService {
    public String send(String message) {
        System.out.println("send message:" + message);
        return message;
    }
}

// 创建代理类并同样实现发送短信的接口
public class SmsProxy implements SmsService {

    private final SmsService smsService;

    public SmsProxy(SmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public String send(String message) {
        //调用方法之前，我们可以添加自己的操作
        System.out.println("before method send()");
        smsService.send(message);
        //调用方法之后，我们同样可以添加自己的操作
        System.out.println("after method send()");
        return null;
    }
}

// 实际使用
public class Main {
    public static void main(String[] args) {
        SmsService smsService = new SmsServiceImpl();
        SmsProxy smsProxy = new SmsProxy(smsService);
        smsProxy.send("java");
    }
}
```

#### 动态代理

相比于静态代理来说，动态代理更加灵活。我们不需要针对每个目标类都单独创建一个代理类，并且也不需要我们必须实现接口，我们可以直接代理实现类( CGLIB 动态代理机制)。**从 JVM 角度来说，动态代理是在运行时动态生成类字节码，并加载到 JVM 中的**。在Java中常用到的动态代理一般是 **JDK 动态代理**和**CGLIB 动态代理**。在本项目中就使用了JDK动态代理。

**JDK动态代理**

在JDK动态代理里面，**`InvocationHandler` 接口和 `Proxy` 类是核心**。RpcClientProxy类就实现了InvocationHandler接口。

```java
public class RpcClientProxy implements InvocationHandler {

}
```

而Proxy类中，最常使用的就是newProxyInstance()方法，用来生成一个代理对象。这个方法一共有 3 个参数：

- **loader**：类加载器，用于加载代理对象。
  
- **interfaces**：被代理类实现的一些接口。
  
- **h**：实现了 `InvocationHandler` 接口的对象；
  

```java
    public <T> T getProxy(Class<T> clazz) {
        // 生成代理对象，参数为：代理对象的类加载器，被代理类实现的接口，实现了InvocationHandler接口的对象
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
```

要实现动态代理的话，还必须需要实现`InvocationHandler` 来自定义处理逻辑。 当我们的动态代理对象调用一个方法时，这个方法的调用就会被转发到实现`InvocationHandler` 接口类的 `invoke` 方法来调用。`invoke()` 方法有下面三个参数：

- **proxy**：动态生成的代理类。
  
- **method**：与代理类对象调用的方法相对应。
  
- **args**：当前 method 方法的参数。
  

```java
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .build();
        RpcResponse<Object> rpcResponse = null;
        if (rpcRequestTransport instanceof NettyRpcClient) {
            CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = completableFuture.get();
        }
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }
```

也就是说：**你通过`Proxy` 类的 `newProxyInstance()` 创建的代理对象在调用方法的时候，实际会调用到实现`InvocationHandler` 接口的实现类的 `invoke()`方法**。你可以在 `invoke()` 方法中自定义处理逻辑，比如在方法执行前后做什么事情。

**CGLIB动态代理**

**JDK 动态代理有一个最致命的问题是其只能代理实现了接口的类**。为了解决这个问题，我们可以用 CGLIB 动态代理机制来避免。[CGLIB](https://github.com/cglib/cglib)(*Code Generation Library*)是一个基于[ASM](http://www.baeldung.com/java-asm)的字节码生成库，它允许我们在运行时对字节码进行修改和动态生成。CGLIB 通过继承方式实现代理。很多知名的开源框架都使用到了[CGLIB](https://github.com/cglib/cglib)， 例如 Spring 中的 AOP 模块中：如果目标对象实现了接口，则默认采用 JDK 动态代理，否则采用 CGLIB 动态代理。在 CGLIB 动态代理机制中 **`MethodInterceptor` 接口和 `Enhancer` 类是核心**。

你需要自定义 `MethodInterceptor` 并重写 `intercept` 方法，`intercept` 用于拦截增强被代理类的方法。

```java
public interface MethodInterceptor extends Callback{
    // 拦截被代理类中的方法
    public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args,MethodProxy proxy) throws Throwable;
}
```

- **obj**：被代理的对象（需要增强的对象）
  
- **method**：被拦截的方法（需要增强的方法）
  
- **args**：方法入参
  
- **proxy**：用于调用原始方法
  

你可以通过 `Enhancer`类来动态获取被代理类，当代理类调用方法的时候，实际调用的是 `MethodInterceptor` 中的 `intercept` 方法。

一般CGLIB 动态代理类使用步骤如下：

- 定义一个类；
- 自定义 `MethodInterceptor` 并重写 `intercept` 方法，`intercept` 用于拦截增强被代理类的方法，和 JDK 动态代理中的 `invoke` 方法类似；
- 通过 `Enhancer` 类的 `create()`创建代理类；

#### 比较二者

先比较JDK动态代理和CGLIB动态代理：

- **JDK 动态代理只能代理实现了接口的类或者直接代理接口，而 CGLIB 可以代理未实现任何接口的类。** 另外， CGLIB 动态代理是通过生成一个被代理类的子类来拦截被代理类的方法调用，因此不能代理声明为 final 类型的类和方法。
  
- 就二者的效率来说，大部分情况都是 JDK 动态代理更优秀，随着 JDK 版本的升级，这个优势更加明显。
  

然后来比较静态代理和动态代理：

- **灵活性**：动态代理更加灵活，不需要必须实现接口，可以直接代理实现类，并且可以不需要针对每个目标类都创建一个代理类。另外，静态代理中，接口一旦新增加方法，目标对象和代理对象都要进行修改，这是非常麻烦的！
  
- **JVM 层面**：静态代理在编译时就将接口、实现类、代理类这些都变成了一个个实际的 class 文件。而动态代理是在运行时动态生成类字节码，并加载到 JVM 中的。
  

## 3. 总结发言

最后来总结一遍整个RPC框架的运行流程：

客户端：

- 有一个启动类和一个控制器类，启动类通过自定义的注解@RpcScan获取了控制器类的Bean，然后调用了控制器类的test方法。而控制器类调用了测试用的API接口里的方法，该接口的实现类在服务端。
  
- 由于控制器类里的HelloService接口有@RpcReference注解，@RpcReference通过Spring的BeanPostProcessor的子类，重写postProcessAfterInitialization方法，在 Bean初始化之后对其进行修改，这里要做的其实就是通过代理类RpcClientProxy来代理NettyRpcClient，NettyRpcClient在注册中心获取到了所需服务的地址后，与NettyRpcServer建立连接并发送请求。
  
- 最后等待对面返回响应并对响应进行检查。
  

服务端：

- 有一个启动类和两个服务接口实现类，启动类通过自定义的注解@RpcScan获取了NettyRpcServer的Bean，然后调用其registerService方法注册服务，服务将会保存到ServiceProvider并由ServiceProvider向注册中心注册，最后启动NettyRpcServer。
  
- 当NettyRpcServer收到请求，交由NettyRpcServerHandler处理，NettyRpcServerHandler又将请求转交到RpcRequestHandler处理，RpcRequestHandler调用ServiceProvider对应服务，并将结果层层返回给NettyRpcServer。
  
- NettyRpcServer将得到的结果发给NettyRpcClient。
  

总体流程图如下：

左边阴影可认为是Consumer，右边阴影可认为是Provider。

![](https://yeyu-1313730906.cos.ap-guangzhou.myqcloud.com/PicGo/rpc.drawio.png)

除去remoting模块外，剩下的模块其实没有太多和RPC强相关的内容了，都是围绕着RPC的扩展。比如说借鉴了Dubbo的ExtensionLoader，可以通过自定义的文件配置，来决定扩展什么服务，像使用何种注册中心，何种序列化，何种负载均衡策略。又比如说设计SingletonFactory，让工厂来管理唯一实例，我们需要哪个类的实例可以通过工厂获取或者创建，不需要自己手动处理。又比如设计各种注解外加bean的后置处理器，实现像Spring Boot一样通过注解来注册和使用服务。此外，比起ZooKeeper，我更推荐使用Nacos，Nacos由于内部已经实现了很多方法，因此配置起来比ZooKeeper简单很多。
