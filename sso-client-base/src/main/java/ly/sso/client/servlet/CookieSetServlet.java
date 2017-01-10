package ly.sso.client.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ly.sso.client.util.StringUtil;

@WebServlet("/sso_client/cookie_set.do")
public class CookieSetServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String vt = req.getParameter("vt");
		if (StringUtil.isBlank(vt)) {
			return;
		}
		// P3P信息
		resp.setHeader("P3P",
				"CP=\"CURa ADMa DEVa PSAo PSDo OUR BUS UNI PUR INT DEM STA PRE COM NAV OTC NOI DSP COR\"");
		// 写cookie
		Cookie cookie = new Cookie("VT", vt);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		resp.addCookie(cookie);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doGet(req, resp);
	}

}