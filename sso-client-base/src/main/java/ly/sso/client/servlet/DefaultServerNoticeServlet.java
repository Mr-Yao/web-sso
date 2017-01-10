package ly.sso.client.servlet;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ly.sso.client.core.token.TokenManager;

/**
 * 默认处理服务端通知的servlet。当服务端 <b>Configuration</b> 中的
 * <b>clientTimeoutUri、clientLogoutUri、 clientShutdownUri</b>
 * 几个字段都为默认值时，默认直接使用该servlet。
 * 
 * @author liyao
 *
 * @date 2016年12月28日 下午11:20:31
 *
 */
@WebServlet("/sso_client/notice/*")
public class DefaultServerNoticeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String uri = request.getRequestURI();
		String cmd = uri.substring(uri.lastIndexOf("/") + 1);

		response.setContentType("text/plain");
		response.setCharacterEncoding("utf-8");

		switch (cmd) {
		case "timeout.do": {
			String vt = request.getParameter("vt");
			int tokenTimeout = Integer.parseInt(request.getParameter("tokenTimeout"));
			Date expries = TokenManager.getInstance().timeout(vt, tokenTimeout);
			response.getWriter().write(expries == null ? "" : String.valueOf(expries.getTime()));
			break;
		}
		case "logout.do": {
			String vt = request.getParameter("vt");
			TokenManager.getInstance().invalid(vt);
			response.getWriter().write("true");
			break;
		}
		case "shutdown.do":
			TokenManager.getInstance().clear();
			response.getWriter().write("true");
			break;
		}
	}
}
