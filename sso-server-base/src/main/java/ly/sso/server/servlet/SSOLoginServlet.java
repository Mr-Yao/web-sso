package ly.sso.server.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.Credential;
import ly.sso.server.core.entity.LoginUser;
import ly.sso.server.util.StaticConstants;

/**
 * 处理用户登录的Servlet，该Servlet中，get请求为登录入口，post请求用于处理用户身份验证。<br>
 * 1、需要在web.xml中配置该Servlet<br>
 * 2、需要实现ly.sso.server.service.AuthenticationHandler接口。并在 <b>Configuration</b>
 * 中配置 <b>authenticationHandler</b> 字段值为该实现类
 * 
 * @author liyao
 *
 * @date 2016年12月29日 下午12:05:15
 *
 */
public class SSOLoginServlet extends HttpServlet {
	private static final long serialVersionUID = -344316708706478386L;
	/**
	 * 配置信息
	 */
	public final Configuration CONFIG = Configuration.getInstance();

	/**
	 * 登录入口
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			LoginMethod.instance().loginEntrance(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final Map<String, String[]> params = request.getParameterMap();

		final HttpSession session = request.getSession();

		Credential credential = new Credential() {

			@Override
			public String getParameter(String name) {
				String[] tmp = params.get(name);
				return tmp != null && tmp.length > 0 ? tmp[0] : null;
			}

			@Override
			public String[] getParameters(String name) {
				return params.get(name);
			}

			@Override
			public Object getSessionValue(String name) {
				return session.getAttribute(name);
			}
		};

		LoginUser loginUser = CONFIG.getAuthenticationHandler().authentication(credential);

		if (loginUser == null) {
			request.setAttribute("errorMsg", credential.getError());
			request.getRequestDispatcher(CONFIG.getLoginPageName()).forward(request, response);
			return;
		}

		Boolean rememberMe = Boolean.parseBoolean(credential.getParameter(StaticConstants.REMEMBER_ME_NAME));
		try {
			String vt = LoginMethod.instance().authSuccess(response, loginUser, rememberMe);
			LoginMethod.instance().validateSuccess(credential.getParameter(StaticConstants.BACK_URL_NAME), vt, null,
					loginUser, request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
