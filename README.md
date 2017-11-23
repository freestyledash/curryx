# Curryx 基于RPC的面向服务的轻量级框架 

 
## 简介

项目使用maven来构建，框架中的角色分为服务调用者、服务提供者和注册中心，其职能分别如下：

* 服务提供者负责实现具体的业务逻辑，需要向注册中心注册其所提供的服务，并将服务接口暴露给服务调用者
* 服务调用者调用服务提供者提供的服务，需要从注册中心中查找相应的服务
* 注册中心负责管理服务提供者所提供的服务集合并能够被服务调用者所发现

  ├── curryx-client   客户端<br>
  ├── curryx-common   公共工具，包含请求报文格式，编码和解码器<br>
  ├── curryx-distributedLock 分布式锁<br>
  ├── curryx-server   服务端<br>
  ├── curryx-serviceRegistryAndDiscovery  业务注册和业务发现<br>


 
## 使用说明
### 如何引入框架

如果是使用maven来构建项目，下载源码，并将源码打包成为jar包,然后在项目的pom.xml中加入如下依赖：

```
<!-- 服务提供者 -->
<dependency>
    <groupId>com.freestyledash</groupId>
    <artifactId>curry-server</artifactId>
    <version>x.x.x</version>
</dependency>
```
```
<!-- 服务调用者 -->
<dependency>
    <groupId>com.freestyledash</groupId>
    <artifactId>curry-client</artifactId>
    <version>x.x.x</version>
</dependency>
```

如果是其他方式，可以下载相应的jar包并加入到项目的classpath中。

  
### 服务提供者初始化

将方法设置为服务
```
@Service(name = Helloworld.class, version = "develop")
public class HelloworldImpl implements Helloworld {

    @Override
    public String hellow() {
        return "hel";
    }
}
```

启动服务,使用spring来组织各个组件,并启动，ps:服务端可以开启客户端，可以调用其他服务
```
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RPCServer bean = context.getBean(RPCServer.class);
        bean.start();
```
### 服务调用者初始化

使用spring来组织服服务调用者
```
<bean class="com.freestyledash.curryx.balance.impl.RandomBalancer" id="randomBalancer"/>
<bean class="com.freestyledash.curryx.registry.impl.ZooKeeperServiceDiscovery" id="serviceDiscovery"
      scope="singleton">
    <constructor-arg name="balancer" ref="randomBalancer"/>
    <constructor-arg name="serviceRoot" value="/x"/>  <!--名字服务器根路径-->
    <constructor-arg name="zkAddress" value="127.0.0.1:2181"/>   <!--名字服务器路径-->
</bean>

<bean class="com.freestyledash.curryx.client.RPCClient" id="rpcClient" scope="singleton">
    <constructor-arg name="serviceDiscovery" ref="serviceDiscovery"/>
</bean>
 ```
 使用java调用
 ```
 RPCClient client = ApplicationContextHolder.getContext().getBean(RPCClient.class);
 T t = client.create(Class<T> tClazz);
 t.dosth();
 ```