package ly.sso.client.core.token;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import ly.sso.client.core.entity.SSOUser;
import ly.sso.client.util.HttpUtil;
import ly.sso.client.util.StringUtil;

/**
 * 令牌管理工具
 * 
 * @author preach
 *
 */
public class TokenManager {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

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
	}

	// 复合结构体，含SSOUser与最后访问时间lastAccessTime两个成员
	private class Token {
		private SSOUser user;
		private Date lastAccessTime;
	}

	// 缓存Map
	private final Map<String, Token> LOCAL_CACHE = new ConcurrentHashMap<String, TokenManager.Token>();

	/**
	 * 验证vt有效性
	 * 
	 * @param vt
	 * @return
	 * @throws Exception
	 */
	public SSOUser validate(String vt) throws Exception {

		SSOUser user = localValidate(vt);

		if (user == null) {
			user = remoteValidate(vt);
		}

		return user;
	}

	// 在本地缓存验证有效性
	private SSOUser localValidate(String vt) {

		// 从缓存中查找数据
		Token token = LOCAL_CACHE.get(vt);

		if (token != null) { // 用户数据存在
			// 更新最后访问时间
			token.lastAccessTime = new Date();
			// 返回结果
			return token.user;
		}

		return null;
	}

	// 远程验证成功后将信息写入本地缓存
	private void cacheUser(String vt, SSOUser user) {
		Token token = new Token();
		token.user = user;
		token.lastAccessTime = new Date();
		LOCAL_CACHE.put(vt, token);
	}

	/** 服务端TOken验证地址 */
	private String serverTokenValidationUri = "/sso_server/token_validate.do?vt=";
	private String serverInnerAddress;

	// 远程验证vt有效性
	private SSOUser remoteValidate(String vt) {
		String url = serverInnerAddress + serverTokenValidationUri + vt;
		String ret = "";
		try {
			ret = HttpUtil.executeHttp(url, "get", null, null, 1);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		// 服务端无返回
		if (StringUtil.isBlank(ret)) {
			return null;
		}
		SSOUser user = JSON.parseObject(ret, SSOUser.class);
		if (user != null) {
			// 处理本地缓存
			cacheUser(vt, user);
		}
		return user;
	}

	/**
	 * 处理服务端发送的timeout通知
	 * 
	 * @param vt
	 * @param tokenTimeout
	 * @return 返回最终过期时间，为null 表示没有token或者客户端缓存已经过期
	 */
	public Date timeout(String vt, int tokenTimeout) {

		Token token = LOCAL_CACHE.get(vt);

		if (token == null) {
			return null;
		}
		Date lastAccessTime = token.lastAccessTime;
		// 最终过期时间
		Date expires = new Date(lastAccessTime.getTime() + tokenTimeout * 60 * 1000);
		// 最终过期时间 小于 当前时间。则表名Token已经过期
		if (expires.compareTo(new Date()) < 0) {
			// 从本地缓存移除
			LOCAL_CACHE.remove(vt);
			// 返回null表示此客户端缓存已过期
			return null;
		} else {
			return expires;
		}
	}

	/**
	 * 处理服务端发送的用户注销通知<br>
	 * 用户退出时失效对应缓存
	 * 
	 * @param vt
	 */
	public void invalid(String vt) {
		// 从本地缓存移除
		LOCAL_CACHE.remove(vt);
	}

	/**
	 * 处理服务端发送的服务端shutdown通知<br>
	 * 服务端应用关闭时清空本地缓存，失效所有信息
	 */
	public void clear() {
		LOCAL_CACHE.clear();
	}

}
