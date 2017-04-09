# Curry 基于RPC的面向服务的轻量级框架 

## Introduction

本项目使用maven来构建，框架中的角色分为服务调用者、服务提供者和注册中心，其职能分别如下：

* 服务提供者负责实现具体的业务逻辑，需要向注册中心注册其所提供的服务，并将服务接口暴露给服务调用者
* 服务调用者调用服务提供者提供的服务，需要从注册中心中查找相应的服务
* 注册中心负责管理服务提供者所提供的服务集合并能够被服务调用者所发现

## Usage

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

之后，在服务提供者/调用端启动之初分别需要进行如下调用来初始化RPC服务端：

```
RPCServerBootstrap.launchRPCServer();   // 服务提供者
```
```
RPCClientBootstrap.getInstance();       // 服务调用者
```

服务调用者调用具体服务的方法为：

```
RPCClient proxy = RPCClientBootstrap.getInstance().getRPCClient();
Service service = proxy.create(Service.class, "version");
service.do(...);
```