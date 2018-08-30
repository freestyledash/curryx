# 基于java的轻量级rpc框架  Curryx 

## 项目简介
框架中的组件分为客户端、服务端、服务注册与服务发现，四个主要的模块。在使用时，服务端启动服务，并使用服务注册功能向名字服务器注册服务，客户端通过服务发现找到服务，并进行rpc调用

## 使用说明
#### 打包该项目
如果是使用maven工具来构建项目，请下载源码，将源码打包成为jar包并加入本地或者远程的仓库（mvn install or mvn depoly ）,
然后在使用该框架的项目的pom.xml中加入如下依赖：
（依赖的版本请根据需要的版本进行填写）

```
    <!-- server添加如下依赖 -->
    <dependency>
        <groupId>com.freestyledash</groupId>
        <artifactId>curry-rpcServer</artifactId>
        <version>x.x.x</version>
    </dependency>
```

```
    <!-- client添加如下依赖 -->
    <dependency>
        <groupId>com.freestyledash</groupId>
        <artifactId>curry-rpcClient</artifactId>
        <version>x.x.x</version>
    </dependency>
```  

#### 启动zookeeper
目前支持zookeeper作为名字服务器，zookeeper的下载和使用请参照官方文档

#### 声明服务接口
使用一个项目开发接口，用于声明有哪些服务，这些接口需要被client和server依赖
```
public interface Helloworld{
    String hellow();
}
```

####  服务提供者实现接口
将某个方法设置为服务（该服务会在服务器启动时自动将服务地址注册到名字服务器中
```
@Service(name = Helloworld.class, version = "develop")
public class HelloworldImpl implements Helloworld {
    @Override
    public String hellow() {
        return "helloworld";
    }
}
```

#### 启动服务（使用spring来组织各个组件，并启动
```
     <!--配置spring扫描需要作为服务的类所在的包-->
     <context:component-scan base-package="server"/>
 
     <!--RPC Server配置-->
     <!--服务注册与发现-->
     <bean id="serviceRegistry"
           class="com.freestyledash.curryx.registry.impl.ZooKeeperServiceRegistry">
         <!--zookeeper地址-->
         <constructor-arg name="zkAddress" value="127.0.0.1:2181"/>
         <constructor-arg name="serviceRoot" value="/x"/>
     </bean>
 
     <!--通讯服务器-->
     <bean id="nettyServer" class="com.freestyledash.curryx.server.server.impl.NettyServer"/>
 
     <!--服务加载工具-->
     <bean id="springServiceLoader"
           class="com.freestyledash.curryx.serviceContainer.impl.spring.SpringServiceContainer"/>
 
     <!--server-->
     <bean id="rpcServer" class="com.freestyledash.curryx.rpcServer.RPCServer">
         <!--服务器注册在zookeeper中的地址，客户端使用改地址和服务器进行通讯-->
         <constructor-arg name="loader" ref="springServiceLoader"/>
         <constructor-arg name="serviceRegistry" ref="serviceRegistry"/>
         <constructor-arg name="server" ref="nettyServer"/>
         <constructor-arg name="serverName" value="rpcServerdemo"/>
     </bean>
```
```
    //在代码中启动服务器
    ApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
    RPCServer serverBootstrap = context.getBean(RPCServer.class);
    serverBootstrap.start();
```
在日志中会显示服务对象的加载，通讯服务器的启动，服务注册的功能

#### 服务调用者初始化

使用spring来组织服服务调用者
```
    <!--负载均衡-->
    <bean class="com.freestyledash.curryx.discovery.util.balance.impl.RandomBalancer" id="randomBalancer"/>

    <!--服务发现-->
    <bean class="com.freestyledash.curryx.discovery.impl.ZooKeeperServiceDiscovery" id="serviceDiscovery"
          scope="singleton">
        <constructor-arg name="balancer" ref="randomBalancer"/>
        <constructor-arg name="serviceRoot" value="/x"/>
        <constructor-arg name="zkAddress" value="127.0.0.1:2181"/>
    </bean>

    <!--请求发送组件-->
    <bean class="com.freestyledash.curryx.client.netty.RPCRequestLauncher" id="launcher">
        <constructor-arg name="eventLoopThreadCount" value="2"/>
    </bean>

    <!--rpc客户端-->
    <bean class="com.freestyledash.curryx.rpcClient.RPCClient" id="rpcClient" scope="singleton">
        <constructor-arg name="serviceDiscovery" ref="serviceDiscovery"/>
        <constructor-arg name="launcher" ref="launcher"/>
    </bean>

 ```
 使用代码调用一个服务
 ```
     RPCClient client = ApplicationContextHolder.getContext().getBean(RPCClient.class);
     T t = client.create(Class<T> tClazz);
     t.dosth();
 ```
 
 #### 注意
 序列化工具采用谷歌的protostuff框架无法正确序列化BigDecimal对象
 
## 设计思路
- 客户端：

- 服务端：
