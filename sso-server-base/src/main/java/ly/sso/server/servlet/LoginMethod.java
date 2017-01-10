package ly.sso.server.servlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.LoginUser;
import ly.sso.server.core.token.TokenManager;
import ly.sso.server.util.CookieUtil;
import ly.sso.server.util.StaticConstants;
import ly.sso.server.util.StringUtil;

/**
 * 一些对登录问题的处理方法
 * 
 * @author liyao
 *
 * @date 2016年12月29日 下午2:18:13
 *
 */
public class LoginMethod {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	/** 配置信息 */
	public final Configuration CONFIG = Configuration.getInstance();
	public final TokenManager TOKEN_MANAGER = TokenManager.getInstance();
	// 构造单例 TokenManager
	private static LoginMethod instance = null;

	// 构造单例 TokenManager
	private static class Instance {
		public static LoginMethod instance = new LoginMethod();
	}

	// 构造单例 TokenManager
	public static LoginMethod instance() {
		if (instance == null) {
			instance = Instance.instance;
		}
		return instance;
	}

	// 构造单例 TokenManager
	protected LoginMethod() {
		if (instance != null) {
			throw new RuntimeException("Can't create another LoginMethod's instance.");
		}
		instance = this;
	}

	/**
	 * 登录入口处理方法
	 * 
	 * @throws Exception
	 */
	public void loginEntrance(HttpServletRequest request, HttpServletResponse response) throws Exception {

		System.out.println(Integer.toHexString(CONFIG.hashCode()));

		// 回调地址
		String backUrl = request.getParameter(StaticConstants.BACK_URL_NAME);
		// 过渡页面
		String transitionPage = request.getParameter("transitionPage");
		// 是否到服务器登录页面登录标识，默认为需要登录
		boolean notLogin = Boolean.parseBoolean(request.getParameter("notLogin"));
		// Validate Token
		String vt = CookieUtil.getCookie(StaticConstants.VALIDATE_TOKEN_NAME, request);
		this.vtIsNull(vt, backUrl, transitionPage, notLogin, request, response);
		this.vtIsNotNull(vt, backUrl, transitionPage, notLogin, request, response);
	}

	/**
	 * Validate Token 不为空的处理方法
	 * 
	 * @param vt
	 *            Validate Token
	 * @param backUrl
	 * @param notLogin
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void vtIsNotNull(String vt, String backUrl, String transitionPage, boolean notLogin,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (vt == null) {
			return;
		}
		LoginUser loginUser = TOKEN_MANAGER.validate(vt);
		if (loginUser != null) { // VT有效
			// 验证成功后操作
			this.validateSuccess(backUrl, vt, transitionPage, loginUser, request, response);
		} else {
			// VT 失效，转入登录页
			// this.authFailed(notLogin, backUrl, transitionPage, request,
			// response);
			// VT 失效，验证是否需要自动登录
			this.autoLogin(vt, backUrl, transitionPage, notLogin, request, response);
		}
	}

	/**
	 * Validate Token 为空的处理方法
	 * 
	 * @param vt
	 *            Validate Token
	 * @param backUrl
	 * @param notLogin
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void vtIsNull(String vt, String backUrl, String transitionPage, boolean notLogin,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (vt != null) {
			return;
		}
		// VT 不存在，验证是否需要自动登录
		this.autoLogin(vt, backUrl, transitionPage, notLogin, request, response);
	}

	/**
	 * 自动登录
	 * 
	 * @param vt
	 * @param backUrl
	 * @param transitionPage
	 * @param notLogin
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void autoLogin(String vt, String backUrl, String transitionPage, boolean notLogin,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Login Ticket
		String lt = CookieUtil.getCookie(StaticConstants.LOGIN_TICKET_NAME, request);
		// 自动登录标识不存在
		if (lt == null) {
			this.authFailed(notLogin, backUrl, transitionPage, request, response);
			return;
		}
		// 自动登录
		LoginUser loginUser = CONFIG.getAuthenticationHandler().autoLogin(lt);
		if (loginUser == null) {
			this.authFailed(notLogin, backUrl, transitionPage, request, response);
		} else {
			vt = authSuccess(response, loginUser, true);
			validateSuccess(backUrl, vt, transitionPage, loginUser, request, response);
		}
	}

	/**
	 * 授权认证失败时的操作
	 * 
	 * @param notLogin
	 * @param backUrl
	 * @param transitionPage
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void authFailed(Boolean notLogin, String backUrl, String transitionPage, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (notLogin != null && notLogin) {// 不需要到服务器登录页面登录,重定向到backUrl
			backUrl = generateBackUrl(backUrl, "");
			if (StringUtil.isBlank(transitionPage)) {
				response.sendRedirect(backUrl);
			} else {
				requestSetBackUrl(backUrl, request);
				request.getRequestDispatcher(CONFIG.getPagePrefix() + transitionPage + CONFIG.getPageSuffix())
						.forward(request, response);
			}
		} else {
			request.setAttribute(StaticConstants.BACK_URL_NAME, backUrl);
			request.getRequestDispatcher(CONFIG.getLoginPageName()).forward(request, response);
		}
	}

	/**
	 * 授权成功后生成Token的操作
	 * 
	 * @param response
	 * @param loginUser
	 * @param rememberMe
	 * @return
	 * @throws Exception
	 */
	public String authSuccess(HttpServletResponse response, LoginUser loginUser, Boolean rememberMe) throws Exception {
		// 生成自动登录标识 LT？
		if (rememberMe != null && rememberMe) {
			String lt = CONFIG.getAuthenticationHandler().generateLoginTicket(loginUser);
			Cookie ltCookie = new Cookie(StaticConstants.LOGIN_TICKET_NAME, lt);
			ltCookie.setHttpOnly(true);
			ltCookie.setMaxAge(CONFIG.getAutoLoginExpDays() * 24 * 60 * 60);
			if (CONFIG.isSecureMode()) {
				ltCookie.setSecure(true);
			}
			response.addCookie(ltCookie);
		}

		// 生成 Validate Token
		String vt = StringUtil.uniqueKey();
		// 存入Map
		TOKEN_MANAGER.addToken(vt, loginUser);
		// 写 Cookie
		Cookie cookie = new Cookie(StaticConstants.VALIDATE_TOKEN_NAME, vt);
		cookie.setHttpOnly(true);
		// 是否仅https模式，如果是，设置cookie secure为true
		if (CONFIG.isSecureMode()) {
			cookie.setSecure(true);
		}

		response.setHeader("P3P",
				"CP=\"CURa ADMa DEVa PSAo PSDo OUR BUS UNI PUR INT DEM STA PRE COM NAV OTC NOI DSP COR\"");
		response.addCookie(cookie);

		LOGGER.debug("authorization success。 生成 Token：{}", vt);
		return vt;
	}

	/**
	 * VT验证成功或登录成功后的操作
	 * 
	 * @param backUrl
	 * @param vt
	 * @param transitionPage
	 * @param loginUser
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void validateSuccess(String backUrl, String vt, String transitionPage, LoginUser loginUser,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setAttribute("token", vt);
		if (StringUtil.isNotBlank(backUrl)) {
			backUrl = this.generateBackUrl(backUrl, vt);
			if (StringUtil.isBlank(transitionPage)) {
				response.sendRedirect(backUrl);
			} else {
				requestSetBackUrl(backUrl, request);
				request.getRequestDispatcher(CONFIG.getPagePrefix() + transitionPage + CONFIG.getPageSuffix())
						.forward(request, response);
			}
		} else {
			request.setAttribute("clientList", CONFIG.getClientSystems(loginUser));
			request.setAttribute("loginUser", loginUser);
			request.getRequestDispatcher(CONFIG.getHomePageName()).forward(request, response);
		}

	}

	/**
	 * 生成BackUrl，给BackUrl拼接__vt_param__=token参数。如果不想将Token暴露在URL中，
	 * 可以继承LoginMethod类并重写本方法，然后将Token放在其他地方，如缓存、DB等，但是要保证客户端可以读取到Token。
	 * 同时在客户端中重写SSOFilter的cookieNoToken方法，并返回重新放置的Token。
	 * 同时要设置客户端中的serverLoginUri，并在服务端新建一个Servlet映射至上面设置的serverLoginUri，
	 * 然后在该新建的Servlet中实例化LoginMethod的子类并调用loginEntrance方法
	 * 
	 * @param backUrl
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public String generateBackUrl(String backUrl, String token) throws Exception {
		return StringUtil.appendUrlParameter(backUrl, StaticConstants.VT_PARAM, token);
	}

	private void requestSetBackUrl(String backUrl, HttpServletRequest request) {
		Map<String, String> map = new HashMap<String, String>();
		request.setAttribute("params", map);
		if (StringUtil.isBlank(backUrl)) {
			return;
		}
		request.setAttribute("redirectUrl", backUrl);
		int index = backUrl.indexOf("?");
		if (index < 0) {
			return;
		}
		request.setAttribute("redirectUrl", backUrl.substring(0, index));
		String[] queryStr = backUrl.substring(index + 1).split("&");
		for (String str : queryStr) {
			int i = str.indexOf("=");
			if (i < 0) {
				continue;
			}
			map.put(str.substring(0, i), str.substring(i + 1));
		}
		request.setAttribute("params", map);
	}

}
