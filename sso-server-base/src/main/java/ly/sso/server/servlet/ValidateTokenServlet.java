package ly.sso.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.LoginUser;
import ly.sso.server.core.service.UserSerializer;
import ly.sso.server.core.token.TokenManager;

/**
 * 处理客户端的Token校验请求的servlet。客户端和服务端内部通信使用
 * 
 * @author liyao
 *
 * @date 2016年12月27日 下午5:55:58
 *
 */
@WebServlet("/sso_server/token_validate.do")
public class ValidateTokenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		// 客户端传来的vt
		String vt = request.getParameter("vt");
		LoginUser user = TokenManager.getInstance().validate(vt);
		if (user == null) {
			response.getWriter().write("");
			return;
		}
		UserSerializer userSerializer = Configuration.getInstance().getUserSerializer();
		try {
			response.getWriter().write(userSerializer.serialization(user));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

}
