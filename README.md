# PlayChat
使用Java实现的一个类似QQ功能的多人聊天程序。可以发送离线消息和离线文件，可以一对一发送消息和一对所有发送消息。传输消息和传输文件都经过加密处理。

### 使用说明
客户端命令：

	===========Help=============
	COMMAND             DESC
	help                --show help
	list                --show all online user
	@user:msg           --send msg to user
	@ALL:msg            --send msg to all user
	#user:filepath      --send file to user
	#ALL:filepath       --send file to all user
	exit				--exit
	============================

服务端命令：

	list   				-- show all online user
	exit				-- exit

### PlayChat设计说明
> PlatChat的设计目的是用Java实现类似QQ进行登录、个人聊天、群聊、文件传输、数据加密的功能Client和Server。

### 用法
1 通过Eclipse将lib下的javabase64-1.3.1.jar包导入到Maven本地库。<br>
2 进入项目目录，执行mvn package<br>
3 修改客户端代码中的目的IP<br>
4 执行：
>
>服务端：java -jar target/playChat-0.0.1-SNAPSHOT.jar -server  <br>
>客户端：java -jar target/playChat-0.0.1-SNAPSHOT.jar -client

### 主要功能
#### 数据加密
1.在客户端向服务端发起连接的时候会进行initSecureSocket。
> 各自生成私钥，并互换公钥。
> 在后续的会话中，将数据通过公钥加密再传输。
> 公钥交换使用ExchangeMess类进行通信。
> 公钥交换完成后服务端会自动生成一个共享密钥。并加密发送给客户端。
> 客户端解密后再加密发送给服务端，服务端校验收到的共享密钥和发送的共享密钥是否一致。
> 一致则建立安全套接字，并使用AES+共享密钥来加解密数据。
> 
> 使用RSA进行加解密时，对String的长度是有要求的，<128-11=117字节。
> 需要对待加密数据进行分块。在RSAUtils中添加了数据分块加密解密的功能。

2.initSecureSocket完成后，生成一个SecSocket。接下来传消息都是通过ChatMess类进行的。
> 使用SecSocket通信过程所有数据会用AES+共享密钥加密。
> 由于AES只能对String类进行加密，而通信是通过ChatMess进行的，所以需要将ChatMess类通过jackJson转换成jsonString。



#### 登录验证
> 在这一块没有做注册，只要输入了正确的密码，就可以登录成功。后续需要完善注册登录制。


#### 用户状态自动刷新
> 线程RefreshClientList会每隔5s刷新一下clientList中用户的状态，并更新。


#### 显示用户列表
> 显示所有登录成功的用户名、IP和状态

#### 显示帮助命令
> 显示客户端常用命令

#### 发送离线消息
> 发送消息给对方后，会首先存到对方的消息队列中，当对方上线时，自动发送给对方。

#### 发送离线文件（可以传输文本文件和二进制文件）
> 发送文件给对方后，会首先存到服务器，当对方上线时，自动发送给对方，并返回对方接收文件成功的回执。

#### 增加了服务端和客户端的退出命令
> exit 或 quit

#### 挤下线功能
> 两个同样的用户名，先后登录，后登录的用户将会将前面的用户挤下线。

#### 可配置参数
将需要修改的参数放到配置文件中。