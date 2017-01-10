package ly.sso.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ly.sso.server.core.Configuration;

/**
 * 刷新SSO配置文件的Servlet。包括sso-server.perproties和client_systems.xml
 * 
 * @author liyao
 *
 * @date 2017年1月5日 下午5:02:10
 *
 */
@WebServlet("/sso_server/refresh_sso_config.do")
public class RefreshSSOConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Boolean flag = true;
		try {
			Configuration.getInstance().loadConfig();
		} catch (Exception e) {
			flag = false;
			e.printStackTrace();
		}
		resp.getWriter().write(flag.toString());
	}

}
