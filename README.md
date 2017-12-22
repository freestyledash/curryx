# Curryx 基于RPC的面向服务的轻量级框架 

## 项目简介

该项目是一个使用java开发的rpc框架，框架中的角色分为服务调用者、服务提供者和注册中心，其职能分别如下：

* 服务提供者负责实现具体的业务逻辑，需要向注册中心注册其所提供的服务，并将服务接口暴露给服务调用者
* 服务调用者调用服务提供者提供的服务，需要从注册中心中查找相应的服务
* 注册中心负责管理服务提供者所提供的服务集合并能够被服务调用者所发现

  ├── curryx-client   客户端<br>
  ├── curryx-server   服务端<br>
  ├── curryx-serviceRegistryAndDiscovery  业务注册和业务发现<br>
  ├── curryx-common   公共工具，包含请求报文格式，编码和解码器<br>
  ├── curryx-distributedLock 分布式锁<br>
  
## 使用说明

### 打包该项目
如果是使用maven来构建项目，请下载源码，并将源码打包成为jar包，加入本地或者远程的仓库（maven install）,然后在使用该框架的项目的pom.xml中加入如下依赖：
（依赖的版本请根据需要的版本进行填写）

```
    <!-- 服务提供者添加如下依赖 -->
    <dependency>
        <groupId>com.freestyledash</groupId>
        <artifactId>curry-server</artifactId>
        <version>x.x.x</version>
    </dependency>
```

```
    <!-- 服务调用者添加如下依赖 -->
    <dependency>
        <groupId>com.freestyledash</groupId>
        <artifactId>curry-client</artifactId>
        <version>x.x.x</version>
    </dependency>
```  

### 启动zookeeper
zookeeper在本框架中充当名字服务器的功能，zookeeper的下载和使用请参照官方文档
下载zookeeper之后启动zookeeper

### 服务提供者初始化
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

启动服务（使用spring来组织各个组件，并启动
```
   <context:component-scan base-package="xxxx"/>
    
   <!--RPC Server配置-->
    <!--服务注册与发现-->
    <bean id="serviceRegistry"
          class="com.freestyledash.curryx.registryAndDiscovery.registry.impl.ZooKeeperServiceRegistry">
        <constructor-arg name="zkAddress" value="127.0.0.1:2181"/>
        <constructor-arg name="serviceRoot" value="/x"/>
        <!--<constructor-arg name="zkConnectionTimeout" value="3000"/>-->
        <!--<constructor-arg name="zkSessionTimeout" value="3000"/>-->
    </bean>

    <!--通讯服务器-->
    <bean id="nettyServer" class="com.freestyledash.curryx.server.server.impl.NettyServer">
        <constructor-arg name="serverListeningAddress" value="127.0.0.1:8001"/>
        <constructor-arg name="bossThreadCount" value="2"/>
        <constructor-arg name="workerThreadCount" value="8"/>
    </bean>

    <!--整合-->
    <bean id="rpcServer" class="com.freestyledash.curryx.server.RPCServer">
        <constructor-arg name="serverAddress" value="127.0.0.1:8001"/>
        <constructor-arg name="serviceRegistry" ref="serviceRegistry"/>
        <constructor-arg name="server" ref="nettyServer"/>
    </bean>

    <!--启动器-->
    <bean id="rpcServerBootStrap" class="com.freestyledash.curryx.server.bootstrap.RPCServerBootstrap"
          scope="singleton">
        <constructor-arg name="rpcServer" ref="rpcServer"/>
    </bean>
```
```
    //在代码中启动服务器
    ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
    RPCServerBootstrap bean = context.getBean(RPCServerBootstrap.class);
    bean.launch();
```
### 服务调用者初始化

使用spring来组织服服务调用者
```
    <bean class="com.freestyledash.curryx.registryAndDiscovery.util.balance.impl.RandomBalancer" id="randomBalancer"/>

    <bean class="com.freestyledash.curryx.registryAndDiscovery.discovery.impl.ZooKeeperServiceDiscovery" id="serviceDiscovery"
          scope="singleton">
        <constructor-arg name="balancer" ref="randomBalancer"/>
        <constructor-arg name="serviceRoot" value="/x"/>
        <constructor-arg name="zkAddress" value="127.0.0.1:2181"/>
    </bean>

    <bean class="com.freestyledash.curryx.client.RPCClient" id="rpcClient" scope="singleton">
        <constructor-arg name="serviceDiscovery" ref="serviceDiscovery"/>
    </bean>
 ```
 使用代码调用一个服务
 ```
     RPCClient client = ApplicationContextHolder.getContext().getBean(RPCClient.class);
     T t = client.create(Class<T> tClazz);
     t.dosth();
 ```
 
 ### 注意
 序列化工具采用谷歌的protostuff框架无法正确序列化BigDecimal对象