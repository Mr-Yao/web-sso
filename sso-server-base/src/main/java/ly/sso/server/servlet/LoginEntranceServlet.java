package ly.sso.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 客户端和服务端内部通信使用
 * 
 * @author liyao
 *
 * @date 2016年12月29日 下午3:37:51
 *
 */
@WebServlet("/sso_server/login_entrance.do")
public class LoginEntranceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			LoginMethod.instance().loginEntrance(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
