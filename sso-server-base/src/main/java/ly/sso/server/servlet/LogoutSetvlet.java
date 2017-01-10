package ly.sso.server.servlet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.ClientSystem;
import ly.sso.server.core.entity.LoginUser;
import ly.sso.server.core.token.TokenManager;
import ly.sso.server.util.CookieUtil;
import ly.sso.server.util.StaticConstants;

/**
 * 处理用户注销的Servlet<br>
 * 1、需要在web.xml中配置该Servlet<br>
 * 2、需要实现ly.sso.server.service.AuthenticationHandler接口。并在 <b>Configuration</b>
 * 中配置 <b>authenticationHandler</b> 字段值为该实现类
 * 
 * @author liyao
 *
 * @date 2016年12月27日 下午10:20:22
 *
 */
public class LogoutSetvlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String backUrl = request.getParameter(StaticConstants.BACK_URL_NAME);

		final String vt = CookieUtil.getCookie(StaticConstants.VALIDATE_TOKEN_NAME, request);

		// 清除自动登录信息
		LoginUser loginUser = TokenManager.getInstance().validate(vt);
		if (loginUser != null) {
			// 清除服务端自动登录状态
			Configuration.getInstance().getAuthenticationHandler().clearLoginTicket(loginUser);
			// 清除自动登录cookie
			CookieUtil.deleteCookie(StaticConstants.LOGIN_TICKET_NAME, response, null);
		}

		// 移除token
		TokenManager.getInstance().invalid(vt);

		// 移除server端vt cookie
		Cookie cookie = new Cookie(StaticConstants.VALIDATE_TOKEN_NAME, null);
		cookie.setMaxAge(0);
		response.addCookie(cookie);

		ExecutorService cachedThread = Executors.newCachedThreadPool(new ThreadFactory() {
			AtomicInteger atomic = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "notice-client-logout-thread-" + atomic.getAndIncrement());
			}
		});

		// 通知各客户端logout
		for (final ClientSystem clientSystem : Configuration.getInstance().getClientSystems()) {
			cachedThread.execute(new Runnable() {
				@Override
				public void run() {
					clientSystem.noticeLogout(vt);
				}
			});
		}
		cachedThread.shutdown();

		if (backUrl == null) {
			request.getRequestDispatcher(Configuration.getInstance().getLogoutPageName()).forward(request, response);
		} else {
			response.sendRedirect(backUrl);
		}
	}
}
