# Curry 基于RPC的面向服务的轻量级框架 

  

## 简介

本项目使用maven来构建，框架中的角色分为服务调用者、服务提供者和注册中心，其职能分别如下：

* 服务提供者负责实现具体的业务逻辑，需要向注册中心注册其所提供的服务，并将服务接口暴露给服务调用者
* 服务调用者调用服务提供者提供的服务，需要从注册中心中查找相应的服务
* 注册中心负责管理服务提供者所提供的服务集合并能够被服务调用者所发现

  

## 使用说明

  

### 如何引入框架

如果是使用maven来构建项目，需要在项目的pom.xml中加入如下依赖：

```
<!-- 服务提供者 -->
<dependency>
    <groupId>com.nov21th</groupId>
    <artifactId>curry-server</artifactId>
    <version>x.x.x</version>
</dependency>
```
```
<!-- 服务调用者 -->
<dependency>
    <groupId>com.nov21th</groupId>
    <artifactId>curry-client</artifactId>
    <version>x.x.x</version>
</dependency>
```

如果是其他方式，可以下载相应的jar包并加入到项目的classpath中。

  

### 服务提供者初始化

服务提供者的初始化有两种类型（仅启动Server或同时启动Client）和两种模式（在当前线程启动或新开一个启动线程），其使用方法分别如下：

```java
RPCServerBootstrap.launch(String springPath);				//在当前线程仅启动Server
RPCServerBootstrap.launchInNewThread(String springPath);	//新开一个线程仅启动Server
RPCServerBootstrap.launchAll(String springPath);			//在当前线程同时启动Client
RPCServerBootstrap.launchAllInNewThread(String springPath);	//新开一个线程同时启动Client
```
服务提供者经初始化后就可以在配置的端口侦听请求了。

  

### 服务调用者初始化

服务调用者的初始化有两种方式，分别如下：

```java
RPCClientBootstrap.init(String springPath);					//使用spring注入配置来初始化
RPCClientBootstrap.init(RPCClient rpcClient);				//使用RPCClient实例来初始化
```

服务调用者初始化后，对具体服务的调用需要通过以下代码来完成：

```java
RPCClient rpcClient = RPCClientBootstrap.getRPCClient();
ConcreteService service = rpcClient.create(ConcreteService.class, "version");
service.do(...);
```

