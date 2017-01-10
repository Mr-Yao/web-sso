package ly.sso.server.core.token;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.LoginUser;

/**
 * Token 管理器 存储VT_USER信息，并提供操作方法
 * 
 * @author liyao
 *
 *         2016年12月21日 下午1:51:27
 *
 */
public class TokenManager {

	private final Configuration CONFIG = Configuration.getInstance();
	// 构造单例 TokenManager
	private static TokenManager instance = null;

	// 构造单例 TokenManager
	private static class Instance {
		public static TokenManager instance = new TokenManager();
	}

	// 构造单例 TokenManager
	public static TokenManager getInstance() {
		if (instance == null) {
			instance = Instance.instance;
		}
		return instance;
	}

	// 构造单例 TokenManager
	private TokenManager() {
		if (instance != null) {
			throw new RuntimeException("Can't create another TokenManager's instance.If you use the Spring framework,"
					+ "modify the configuration file as follows:'<bean id=\"xxx\" class=\"" + this.getClass().getName()
					+ "\" factory-method=\"getInstance\">...'");
		}
		instance = this;
		startTimer();
	}

	/**
	 * 启动定时器
	 */
	private void startTimer() {
		CONFIG.getTokenRegularValidator().execute(CONFIG, DATA_MAP);
	}

	public class Token {
		private LoginUser loginUser;// 登录用户对象
		private Date expired;// Token 过期时间

		public LoginUser getLoginUser() {
			return loginUser;
		}

		public void setLoginUser(LoginUser loginUser) {
			this.loginUser = loginUser;
		}

		public Date getExpired() {
			return expired;
		}

		public void setExpired(Date expired) {
			this.expired = expired;
		}

	}

	// 令牌存储结构
	private final Map<String, Token> DATA_MAP = new ConcurrentHashMap<String, TokenManager.Token>();

	/**
	 * 验证令牌有效性
	 * 
	 * @param vt
	 * @return
	 */
	public LoginUser validate(String vt) {
		if (vt == null) {
			return null;
		}
		Token token = DATA_MAP.get(vt);
		return token == null ? null : token.loginUser;
	}

	/**
	 * 用户授权成功后将授权信息存入
	 * 
	 * @param vt
	 * @param loginUser
	 */
	public void addToken(String vt, LoginUser loginUser) {
		Token token = new Token();
		token.loginUser = loginUser;

		token.expired = new Date(new Date().getTime() + CONFIG.getTokenTimeout() * 60 * 1000);

		DATA_MAP.put(vt, token);
	}

	public void invalid(String vt) {
		if (vt != null) {
			DATA_MAP.remove(vt);
		}
	}
}
