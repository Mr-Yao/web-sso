服务端：
1、添加sso-server-base.jar
2、在classpath下添加文件：sso-server.properties 和 client_systems.xml
3、实现ly.sso.server.service.AuthenticationHandler 接口。并在sso-server.properties添加 authenticationHandler = 该实现类
4、继承ly.sso.server.service.UserSerializer 类。并在sso-server.properties添加 userSerializer = 该子类
5、根据具体的sso客户端配置 client_systems.xml
6、在web.xml中配置用户登录用的Servlet：ly.sso.server.servlet.SSOLoginServlet
7、在web.xml中配置用户注销用的Servlet：ly.sso.server.servlet.LogoutSetvlet
8、让你具体的用户entity继承 ly.sso.server.core.entity.LoginUser

sso-server.properties的其他配置 和 client_systems.xml 具体配置参见sso-api中的Configuration类所列出的字段