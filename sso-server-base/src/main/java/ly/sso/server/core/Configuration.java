package ly.sso.server.core;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ly.sso.server.core.entity.ClientSystem;
import ly.sso.server.core.entity.LoginUser;
import ly.sso.server.core.service.AuthenticationHandler;
import ly.sso.server.core.service.TokenRegularValidator;
import ly.sso.server.core.service.UserSerializer;
import ly.sso.server.core.service.impl.DefaultTokenRegularValidator;
import ly.sso.server.core.token.TokenManager;

/**
 * SSO配置信息
 * 
 * @author liyao
 *
 *         2016年12月27日 下午3:47:11
 *
 */
public class Configuration {
	private final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);

	private static Configuration instance = null;

	// 构造单例 Configuration
	private static class Instance {
		public static Configuration instance = new Configuration();
	}

	/**
	 * 
	 * 获取配置信息实例。该类为单例类，如果使用Spring注入属性，请使用：<br>
	 * '&lt;bean id="xxx" class="" factory-method="getInstance">...'
	 * 
	 * @return Configuration Instance
	 */
	// 构造单例 Configuration
	public static Configuration getInstance() {
		if (instance == null) {
			instance = Instance.instance;
		}
		return instance;
	}

	// 构造单例 Configuration
	private Configuration() {
		init();
	}

	private Configuration(String SSOServerConfigName, String clientListConfigName) {
		this.SSOServerConfigName = SSOServerConfigName;
		this.clientListConfigName = clientListConfigName;
		init();
	}

	/**
	 * 初始化
	 */
	private void init() {
		if (instance != null) {
			throw new RuntimeException("Can't create another Configuration's instance.If you use the Spring framework,"
					+ "modify the configuration file as follows:'<bean id=\"xxx\" class=\"" + this.getClass().getName()
					+ "\" factory-method=\"getInstance\">...'");
		}
		instance = this;
		try {
			this.loadConfig();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String SSOServerConfigName = "sso-server.properties";
	private String clientListConfigName = "client_systems.xml";

	/** 登录页面视图名称，默认为：/login */
	String loginPageName = "/login";
	/** 主页视图名称，默认为：/index。当登录成功且没有backUrl时，将跳转至该页面 */
	String homePageName = "/index";
	/** 登出后跳转页面，默认为：/logout。当登出成功且没有backUrl时，将跳转至该页面 */
	String logoutPageName = "/logout";
	/** 页面前缀，默认为：/WEB-INF/jsp */
	String pagePrefix = "/WEB-INF/jsp";
	/** 页面后缀，默认为：.jsp */
	String pageSuffix = ".jsp";

	/**
	 * 通知客户端Token超时的URI，默认为：/sso_client/notice/timeout.do。<br>
	 * 对应客户端处理servlet为： ly.sso.client.servlet.TokenTimeoutServlet<br>
	 * 如果使用默认值可以使用：ly.sso.client.servlet.DefaultServerNoticeServlet
	 */
	String clientTimeoutUri = "/sso_client/notice/timeout.do";
	/**
	 * 通知客户端用户退出的URI，默认为：/sso_client/notice/logout.do。<br>
	 * 对应客户端处理servlet为： ly.sso.client.servlet.UserLogoutServlet<br>
	 * 如果使用默认值可以使用：ly.sso.client.servlet.DefaultServerNoticeServlet
	 */
	String clientLogoutUri = "/sso_client/notice/logout.do";
	/**
	 * 通知客户端服务器关闭的URI，默认为： /sso_client/notice/shutdown.do。<br>
	 * 对应客户端处理servlet为： ly.sso.client.servlet.ServerShutdownServlet<br>
	 * 如果使用默认值可以使用：ly.sso.client.servlet.DefaultServerNoticeServlet
	 */
	String clientShutdownUri = "/sso_client/notice/shutdown.do";

	/** 令牌有效期，单位为分钟。默认30分钟 */
	int tokenTimeout = 30;
	/** 安全模式，请求是否必须为https协议。默认为 false */
	boolean secureMode = false;
	/** 自动登录状态有效期限，单位天。默认7天 */
	int autoLoginExpDays = 7;

	/**
	 * Token 定期验证器 ，默认为：DefaultTokenRegularValidator 。如果需要自定义业务可将该字段值配置为自定义的实现类
	 */
	TokenRegularValidator tokenRegularValidator = new DefaultTokenRegularValidator();
	/** 身份验证处理器，须自行实现该接口并且将该字段值配置为具体的实现类 */
	AuthenticationHandler authenticationHandler;
	/** 用户信息转换序列化实现 ，须自行继承该抽象类并且将该字段值配置为具体的子类 */
	UserSerializer userSerializer;

	/**
	 * 客户端应用列表，由配置文件：client_systems.xml 设置。格式为：
	 * 
	 * <pre>
	 *&lt;systems>
	 *  &lt;system id="test1"  name="Test1 - www.sys1.com">
	 *    &lt;!-- 域名地址，用于页面跳转 -->
	 *    &lt;baseUrl>//www.sys1.com:8081&lt;/baseUrl>
	 *    &lt;!-- 主页 -->
	 *    &lt;homeUri>/index.jsp&lt;/homeUri>
	 *    &lt;!-- 内部地址，用于服务器和客户端之间通信 。建议使用IP地址，因为内部代码会拼接URL地址，-->
	 *    &lt;!-- 使用IP则可以根据最大IP位数，即16位+5位端口号预估一个有初始容量的StringBuffer，如果使用域名则可能会超出预估值-->
	 *    &lt;innerAddress>http://127.0.0.1:8081&lt;/innerAddress>
	 *  &lt;/system>
	 *  &lt;system id="test2"  name="Test2 - www.sys2.com">
	 *    &lt;baseUrl>//www.sys2.com:8082&lt;/baseUrl>
	 *    &lt;homeUri>/index.jsp&lt;/homeUri>
	 *    &lt;innerAddress>http://127.0.0.1:8082&lt;/innerAddress>
	 *  &lt;/system>
	 *&lt;/systems>
	 * </pre>
	 */
	List<ClientSystem> clientSystems = new ArrayList<ClientSystem>();

	/**
	 * 加载配置文件，如果修改了配置文件可直接调用该方法来加载新的配置。配置文件名称固定为：sso-server.properties
	 * 
	 * @throws Exception
	 */
	public void loadConfig() throws Exception {
		LOGGER.debug("SSO服务端开始加载配置文件：{}", this.SSOServerConfigName);
		InputStream in = Configuration.class.getClassLoader().getResourceAsStream(this.SSOServerConfigName);
		try {
			if (in == null) {
				LOGGER.warn("SSO server config {} is not found.Use Spring framework inject!", this.SSOServerConfigName);
				this.loadClientSystem();
				return;
			}
			Properties pro = new Properties();
			pro.load(in);

			Enumeration<Object> keys = pro.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = pro.getProperty(key);
				LOGGER.debug("设置Configuration属性，key：{}，value：{}", key, value);
				Field field = this.getClass().getDeclaredField(key);
				if (field == null) {
					LOGGER.error("{} 中的key（{}）在Configuration实体中不存在", this.SSOServerConfigName, key);
					continue;
				}
				field.setAccessible(true);
				String fieldName = field.getType().getSimpleName();

				if (fieldName.equalsIgnoreCase("String")) {
					field.set(this, value);
				} else if (fieldName.equalsIgnoreCase("boolean")) {
					field.setBoolean(this, Boolean.parseBoolean(value));
				} else if (fieldName.equalsIgnoreCase("int") || fieldName.equalsIgnoreCase("integer")) {
					int tmp = Integer.valueOf(value);
					field.setInt(this, tmp);
				} else {
					field.set(this, Class.forName(value).newInstance());
				}
			}
			LOGGER.debug("{}加载完成：{}", this.SSOServerConfigName, this);
			this.loadClientSystem();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * 加载客户端系统
	 * 
	 * @throws Exception
	 */
	private void loadClientSystem() throws Exception {
		LOGGER.debug("SSO服务端开始加载客户端列表：{}", this.clientListConfigName);
		InputStream in = Configuration.class.getClassLoader().getResourceAsStream(this.clientListConfigName);
		if (in == null) {
			throw new FileNotFoundException("The client list(" + this.clientListConfigName + ") is not found");
		}
		try {
			SAXReader reader = new SAXReader();
			Document doc = reader.read(in);
			Element root = doc.getRootElement();
			@SuppressWarnings("unchecked")
			List<Element> systemElements = root.elements();

			clientSystems.clear();
			for (Element element : systemElements) {
				ClientSystem clientSystem = new ClientSystem();

				clientSystem.setId(element.attributeValue("id"));
				clientSystem.setName(element.attributeValue("name"));
				clientSystem.setBaseUrl(element.elementText("baseUrl"));
				clientSystem.setHomeUri(element.elementText("homeUri"));
				clientSystem.setInnerAddress(element.elementText("innerAddress"));
				clientSystems.add(clientSystem);
			}
			LOGGER.debug("{}加载完成：{}", this.clientListConfigName, clientSystems);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * 应用停止时执行，做清理性工作，如通知客户端服务器停止
	 */
	public void destroy() {
		for (ClientSystem clientSystem : clientSystems) {
			clientSystem.noticeShutdown();
		}
	}

	@Override
	public String toString() {
		return "\r\n" + super.toString() + " [" + "\r\n	loginPageName = " + loginPageName + ", \r\n	homePageName = "
				+ homePageName + ", \r\n	logoutPageName = " + logoutPageName + ", \r\n	pagePrefix = " + pagePrefix
				+ ", \r\n	pageSuffix = " + pageSuffix + ", \r\n	clientTimeoutUri = " + clientTimeoutUri
				+ ", \r\n	clientLogoutUri = " + clientLogoutUri + ", \r\n	clientShutdownUri = " + clientShutdownUri
				+ ", \r\n	tokenTimeout = " + tokenTimeout + ", \r\n	secureMode = " + secureMode
				+ ", \r\n	autoLoginExpDays = " + autoLoginExpDays + ", \r\n	tokenRegularValidator = "
				+ tokenRegularValidator + ", \r\n	authenticationHandler = " + authenticationHandler
				+ ", \r\n	userSerializer = " + userSerializer + "\r\n]";
	}

	public AuthenticationHandler getAuthenticationHandler() {
		return authenticationHandler;
	}

	void setAuthenticationHandler(AuthenticationHandler authenticationHandler) {
		this.authenticationHandler = authenticationHandler;
	}

	public UserSerializer getUserSerializer() {
		return userSerializer;
	}

	void setUserSerializer(UserSerializer userSerializer) {
		this.userSerializer = userSerializer;
	}

	public String getLoginPageName() {
		return this.pagePrefix + this.loginPageName + this.pageSuffix;
	}

	void setLoginPageName(String loginViewName) {
		this.loginPageName = loginViewName;
	}

	public String getHomePageName() {
		return this.pagePrefix + this.homePageName + this.pageSuffix;
	}

	void setHomePageName(String homePageName) {
		this.homePageName = homePageName;
	}

	public String getLogoutPageName() {
		return this.pagePrefix + this.logoutPageName + this.pageSuffix;
	}

	void setLogoutPageName(String logoutPageName) {
		this.logoutPageName = logoutPageName;
	}

	public String getPagePrefix() {
		return pagePrefix;
	}

	void setPagePrefix(String pagePrefix) {
		this.pagePrefix = pagePrefix;
	}

	public String getPageSuffix() {
		return pageSuffix;
	}

	void setPageSuffix(String pageSuffix) {
		this.pageSuffix = pageSuffix;
	}

	public String getClientTimeoutUri() {
		return clientTimeoutUri;
	}

	void setClientTimeoutUri(String clientTimeoutUri) {
		this.clientTimeoutUri = clientTimeoutUri;
	}

	public String getClientLogoutUri() {
		return clientLogoutUri;
	}

	void setClientLogoutUri(String clientLogoutUri) {
		this.clientLogoutUri = clientLogoutUri;
	}

	public String getClientShutdownUri() {
		return clientShutdownUri;
	}

	void setClientShutdownUri(String clientShutdownUri) {
		this.clientShutdownUri = clientShutdownUri;
	}

	public int getTokenTimeout() {
		return tokenTimeout;
	}

	void setTokenTimeout(int tokenTimeout) {
		this.tokenTimeout = tokenTimeout;
	}

	public Boolean isSecureMode() {
		return secureMode;
	}

	void setSecureMode(Boolean secureMode) {
		this.secureMode = secureMode;
	}

	public int getAutoLoginExpDays() {
		return autoLoginExpDays;
	}

	void setAutoLoginExpDays(int autoLoginExpDays) {
		this.autoLoginExpDays = autoLoginExpDays;
	}

	public List<ClientSystem> getClientSystems() {
		return clientSystems;
	}

	/**
	 * 获取指定用户的可用系统列表
	 * 
	 * @param loginUser
	 * @return
	 * @throws Exception
	 */
	public List<ClientSystem> getClientSystems(LoginUser loginUser) throws Exception {
		Set<String> authedSysIds = getAuthenticationHandler().authedSystemIds(loginUser);

		// null表示允许全部
		if (authedSysIds == null) {
			return clientSystems;
		}

		List<ClientSystem> auhtedSystems = new ArrayList<ClientSystem>();
		for (ClientSystem clientSystem : clientSystems) {
			if (authedSysIds.contains(clientSystem.getId())) {
				auhtedSystems.add(clientSystem);
			}
		}

		return auhtedSystems;
	}

	void setClientSystems(List<ClientSystem> clientSystems) {
		this.clientSystems = clientSystems;
	}

	public TokenRegularValidator getTokenRegularValidator() {
		return tokenRegularValidator;
	}

	void setTokenRegularValidator(TokenRegularValidator tokenRegularValidator) {
		this.tokenRegularValidator = tokenRegularValidator;
	}

}
