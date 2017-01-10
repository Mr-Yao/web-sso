package ly.sso.client.core.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ly.sso.client.core.entity.SSOUser;
import ly.sso.client.core.token.TokenManager;
import ly.sso.client.util.CookieUtil;
import ly.sso.client.util.StringUtil;

/**
 * 登录状态验证拦截器。需要在web.xml中配置该Filter时同时设置初始化参数（具体参见所列出的字段）。因为参数数量不多，
 * 所以没有使用配置文件而在此处进行配置。在web. xml中添加如下配置：
 * 
 * <pre>
 * &lt;filter> 
 *   &lt;filter-name>SSOFilter&lt;/filter-name>
 *   &lt;filter-class>ly.sso.client.core.filter.SSOFilter&lt;/filter-class>
 * 
 *   &lt;init-param> 
 *     &lt;param-name>serverBaseUrl&lt;/param-name>
 *     &lt;param-value>http://www.ca.com:8080&lt;/param-value>
 *   &lt;/init-param>
 *   &lt;init-param>
 *     &lt;param-name>serverInnerAddress&lt;/param-name>
 *     &lt;param-value>http://127.0.0.1:8080&lt;/param-value>
 *   &lt;/init-param>
 *   &lt;init-param>
 *     &lt;param-name>notLoginOnFail&lt;/param-name>
 *     &lt;param-value>true&lt;/param-value> 
 *   &lt;/init-param> 
 * &lt;/filter>
 * &lt;filter-mapping>
 *   &lt;filter-name>SSOFilter&lt;/filter-name>
 *   &lt;url-pattern>/*.do&lt;/url-pattern> 
 * &lt;/filter-mapping>
 * </pre>
 * 
 * 
 * @author liyao
 *
 * @date 2016年12月29日 上午12:02:16
 *
 */
public class SSOFilter implements Filter {
	private final String PARANAME = "__vt_param__";
	private final Logger LOGGER = LoggerFactory.getLogger(SSOFilter.class);

	/** 不需要拦截的URI模式，以正则表达式表示 */
	String excludes;
	/** 服务端公网访问地址。必须设置 */
	String serverBaseUrl;
	/** 服务端系统间通信用内网地址 。必须设置 */
	String serverInnerAddress;
	/** 服务端用于处理登录入口的servlet的地址，默认为： /login_entrance.do。 一般不需要设置 */
	String serverLoginUri = "/sso_server/login_entrance.do";
	/** 当授权失败时是否让浏览器跳转到服务端登录页 */
	protected boolean notLoginOnFail;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		excludes = filterConfig.getInitParameter("excludes");
		serverBaseUrl = filterConfig.getInitParameter("serverBaseUrl");
		serverInnerAddress = filterConfig.getInitParameter("serverInnerAddress");
		notLoginOnFail = Boolean.parseBoolean(filterConfig.getInitParameter("notLoginOnFail"));

		if (StringUtil.isBlank(serverBaseUrl) || StringUtil.isBlank(serverInnerAddress)
				|| StringUtil.isBlank(serverLoginUri)) {
			throw new ServletException("SSOFilter初始化参数配置错误，必须设置serverBaseUrl、serverInnerAddress、serverLoginUri参数!");
		}

		TokenManager tm = TokenManager.getInstance();
		try {
			Field field = tm.getClass().getDeclaredField("serverInnerAddress");
			field.setAccessible(true);
			field.set(tm, serverInnerAddress);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// 如果是不需要拦截的请求，直接通过
		if (requestIsExclude(request)) {
			chain.doFilter(request, response);
			return;
		}
		LOGGER.debug("进入SSOFilter,当前请求url:{}", request.getRequestURL());

		// 进行登录状态验证
		String vt = CookieUtil.getCookie("VT", request);
		if (vt != null) {
			SSOUser user = null;

			try {
				user = TokenManager.getInstance().validate(vt);
			} catch (Exception e) {
				throw new ServletException(e);
			}

			if (user != null) {// 表示身份验证通过
				UserHolder.setUser(user, request);// 将user存放，供业务系统使用
				chain.doFilter(request, response); // 请求继续向下执行
			} else {
				// 删除无效的VT cookie
				CookieUtil.deleteCookie("VT", response, "/");
				// 引导浏览器重定向到服务端执行登录校验
				loginCheckFromServer(this.serverBaseUrl + this.serverLoginUri, request, response);
			}
			return;
		}
		String vtParam = this.cookieNoToken(request, response);
		if (vtParam == null) {
			// 请求中中没有vtParam，引导浏览器重定向到服务端执行登录校验
			loginCheckFromServer(serverBaseUrl + serverLoginUri, request, response);
		} else if (vtParam.length() == 0) {
			// 有vtParam，但内容为空，表示到服务端loginCheck后，得到的结果是未登录
			response.sendError(403);
		} else {
			// 让浏览器向本链接发起一次重定向，此过程去除vtParam，将vt写入cookie
			redirectToSelf(vtParam, request, response);
		}
	}

	/**
	 * 客户端cookie中没有Token的处理方法
	 * 
	 * @param serverBaseUrl
	 * @param serverLoginUri
	 * @param serverInnerAddress
	 * @param request
	 * @param response
	 * @return Token
	 * @throws IOException
	 */
	protected String cookieNoToken(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		return request.getParameter(PARANAME); // 从请求中
	}

	// 从参数中获取服务端传来的vt后，执行一个到本链接的重定向，将vt写入cookie
	// 重定向后再发来的请求就存在有效vt参数了
	private void redirectToSelf(String vt, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		// 此处拼接redirect的url，去除vt参数部分
		StringBuffer location = request.getRequestURL();

		String qstr = request.getQueryString();
		int index = qstr.indexOf(PARANAME);
		// http://www.sys1.com:8081/test/tt?a=2&b=xxx
		if (index > 0) { // 还有其它参数，para1=param1&param2=param2&__vt_param__=xxx是最后一个参数
			qstr = "?" + qstr.substring(0, qstr.indexOf(PARANAME) - 1);
		} else { // 没有其它参数 qstr = __vt_param__=xxx
			qstr = "";
		}

		location.append(qstr);

		Cookie cookie = new Cookie("VT", vt);
		cookie.setPath("/");
		cookie.setHttpOnly(true);// 不允许JS修改cookie
		response.addCookie(cookie);

		response.sendRedirect(location.toString());
	}

	/**
	 * 引导浏览器重定向到服务端执行登录校验。<br>
	 * 问题1、Ajax类型请求涉及跨域问题，所以默认不接受Ajax请求。建议Ajax请求前，先让业务系统获取到Token，
	 * 这样发起ajax请求时就不会执行跳转验证操作，避免跨域操作产生<br>
	 * 问题2、此处使用redirect到服务端执行登录校验，redirect只能是get请求，所以如果当前是post请求，
	 * 会将post过来的请求参数变成url querystring，即get形式参数。这种情况，此处实现就会有一个局限性 ——
	 * 请求参数长度的限制，因为浏览器对get请求的长度都会有所限制，如果post过来的内容过大，就会造成请求参数丢失。 目前的解决方法是：<br>
	 * 1）同Ajax请求一样，在发起这类请求前任意时间点发起一次任意类型的请求去获取到Token<br>
	 * 2）通过新建中间页面解决，大致步骤如下：<br>
	 * a、在客户端新建一个过渡用的jsp页面（下面简称a.jsp）<br>
	 * b、在服务端新建一个过渡用的jsp页面（下面简称b.jsp）。注意服务端 <b>Configuration</b> 中的
	 * <b>pagePrefix</b> 字段值 <br>
	 * c、自定义类继承该Filter，重写notAjaxRequestDisposal，将收到的请求转发到a.jsp<br>
	 * d、在a.jsp中自动提交一个Post表单到redirectUrl，提交表单时将backUrl作为参数提交。并且新增参数：
	 * transitionPage，参数值为a.jsp名称（如：a.jsp名称为test.jsp，则transitionPage=test） <br>
	 * e、当步骤d将请求提交到redirectUrl时，服务端会将请求处理结束后转发到b.jsp。
	 * 同时会在requestScope中携带redirectUrl和params(Map结构)<br>
	 * h、在b.jsp中自动提交一个Post表单到redirectUrl，提交表单时将params中的参数遍历出来作为参数提交
	 * 
	 * @param redirectUrl
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void loginCheckFromServer(String redirectUrl, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String qstr = makeQueryString(request); // 将所有请求参数重新拼接成queryString
		String backUrl = request.getRequestURL() + qstr; // 回调url
		if ("XMLHttpRequest".equals(request.getHeader("x-requested-with"))) {
			// ajax类型请求涉及跨域问题
			// CORS方案解决跨域操作时，无法携带Cookie，所以无法完成验证，此处不适合
			// jsonp方案可以处理Cookie问题，但jsonp方式对后端代码有影响，能实现但复杂不理想，大家可以课后练习实现
			// 所以ajax请求前建议先让业务系统获取到vt，这样发起ajax请求时就不会执行跳转验证操作，避免跨域操作产生
			this.ajaxRequestDisposal(redirectUrl, backUrl, request, response);
		} else {
			this.notAjaxRequestDisposal(redirectUrl, backUrl, request, response);
		}

	}

	/**
	 * 重定向到服务端执行登录校验时，Ajax请求处理方法。
	 * 
	 * @param redirectUrl
	 * @param backUrl
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void ajaxRequestDisposal(String redirectUrl, String backUrl, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if ("XMLHttpRequest".equals(request.getHeader("x-requested-with"))) {
			// 400 状态表示请求格式错误，服务器没有理解请求，此处返回400状态表示未登录时服务器拒绝此ajax请求
			response.sendError(400);
		}
	}

	/**
	 * 重定向到服务端执行登录校验时，非Ajax请求处理方法。
	 * 
	 * @param redirectUrl
	 * @param backUrl
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void notAjaxRequestDisposal(String redirectUrl, String backUrl, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if ("XMLHttpRequest".equals(request.getHeader("x-requested-with"))) {
			return;
		}
		// redirect只能是get请求，所以如果当前是post请求，会将post过来的请求参数变成url
		// querystring，即get形式参数
		// 这种情况，此处实现就会有一个局限性 —— 请求参数长度的限制，因为浏览器对get请求的长度都会有所限制。
		// 如果post过来的内容过大，就会造成请求参数丢失
		// 解决这个问题，只能是让用户系统去避免这种情况发生.
		// 可以在发送这类请求前任意时间点发起一次任意get类型请求，这个get请求通过loginCheck
		// 的引导从服务端获取到vt，当再发起post请求时，vt已存在并有效，就不会进入到这个过程，从而避免了问题出现
		// http://www.sys1.com:8081/test/tt?a=2&b=xxx&__vt_param__=

		String location = redirectUrl + "?backUrl=" + URLEncoder.encode(backUrl, "UTF-8");
		// 普通类型请求 http://www.ca.com:8080/login?backUrl=
		if (notLoginOnFail) {
			location += "&notLogin=true";
		}
		response.sendRedirect(location);
	}

	// 将所有请求参数重新拼接成queryString
	private String makeQueryString(HttpServletRequest request) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		// ? a= 1&a=2&b=xx [1,2][] ?a=1&a=2&b=xxx
		Enumeration<String> paraNames = request.getParameterNames();
		while (paraNames.hasMoreElements()) {
			String paraName = paraNames.nextElement();
			String[] paraVals = request.getParameterValues(paraName);
			for (String paraVal : paraVals) {
				builder.append("&").append(paraName).append("=").append(URLEncoder.encode(paraVal, "UTF-8"));
			}
		}

		if (builder.length() > 0) {
			builder.replace(0, 1, "?");
		}

		return builder.toString();
	}

	// 判断请求是否不需要拦截
	private boolean requestIsExclude(HttpServletRequest request) {
		// 默认例外的URL，系统间通信的URL
		String defaultExclude = "^.*[(/notice/[(timeout)(logout)(shutdown)]{1})(/cookie_set)]{1}\\.do.*$";
		// 获取去除context path后的请求路径
		String contextPath = request.getContextPath();
		String uri = request.getRequestURI();
		uri = uri.substring(contextPath.length());
		// 正则模式匹配的uri被排除，不需要拦截
		boolean isSysUrl = uri.matches(defaultExclude);

		if (isSysUrl) {
			LOGGER.debug("request path: {} is excluded!", uri);
			return true;
		}

		// 没有设定excludes时，所以经过filter的请求都需要被处理
		if (StringUtil.isEmpty(excludes)) {
			return false;
		}

		// 正则模式匹配的uri被排除，不需要拦截
		boolean isExcluded = uri.matches(excludes);

		if (isExcluded) {
			LOGGER.debug("request path: {} is excluded!", uri);
		}

		return isExcluded;
	}

	@Override
	public void destroy() {
		// do nothing
	}

}
