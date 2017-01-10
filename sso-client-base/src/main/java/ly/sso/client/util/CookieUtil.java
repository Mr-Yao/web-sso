package ly.sso.client.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
	private CookieUtil() {
	}

	/**
	 * 查找特定cookie值
	 * 
	 * @param cookieName
	 * @param request
	 * @return
	 */
	public static String getCookie(String cookieName, HttpServletRequest request) {

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName)) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * 删除cookie
	 * 
	 * @param cookieName
	 * @param response
	 * @param path
	 */
	public static void deleteCookie(String cookieName, HttpServletResponse response, String path) {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setMaxAge(0);
		if (path != null) {
			cookie.setPath("/");
		}
		response.addCookie(cookie);
	}
}
