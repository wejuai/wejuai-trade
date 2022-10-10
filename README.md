# wejuai-trade
支付服务

### 结构
- core：gateway和sync通用内容
- gateway：请求第三方支付服务
- sync：同步第三方支付状态并通知调用者

### 外部关联
- aliyun mns：拉取方式的消息队列，比传统mq价格便宜
- 支付宝
- 微信支付

### 配置项
- 详情参考`wejuai-config-server`中的配置文件
- 两个`bootstrap.yml`都需要配置
- gateway中需要在`resources`文件夹下放置微信支付中向用户转账的证书，文件名为`weixin.p12`
- core中`HttpTraceActuatorConfig.java`的spring`actuator/httptrace`账号密码，错误的账号密码会在日志中记录访问ip

## 运行
gateway和sync需要分开独立运行

### 本地运行
1. 配置项以及其中的第三方服务开通
gradle build，其中github的仓库必须使用key才可以下载，需要在个人文件夹下的.gradle/gradle.properties中添加对应的key=value方式配置，如果不行，就去下载对应仓库的代码本地install一下
2. 启动配置项中的数据库
3. 分别运行`xxApplication.java`的`main()`方法

### docker build以及运行
- 运行gradle中的docker build task
- 如果配置了其中的第三方仓库可以运行docker push，会先build再push
- 运行方式 docker run {image name:tag}，默认是运行的profile为dev，可以通过环境变量的方式修改，默认启动配置参数在Dockerfile中