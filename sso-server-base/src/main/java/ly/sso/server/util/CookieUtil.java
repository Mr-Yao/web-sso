package ly.sso.server.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieUtil {
	private CookieUtil() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CookieUtil.class);

	/**
	 * 查找特定cookie值
	 * 
	 * @param cookieName
	 * @param request
	 * @return
	 */
	public static String getCookie(String cookieName, HttpServletRequest request) {

		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName)) {
				LOGGER.debug("从Cookie中获取值，cookieName：{}，cookieValue：{}", cookieName, cookie.getValue());
				return cookie.getValue();
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
		cookie.setPath("/");
		if (path != null) {
			cookie.setPath(path);
		}
		response.addCookie(cookie);
	}
}
