package ly.sso.client.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

	/**
	 * 发送HTTP请求
	 * 
	 * @param url
	 *            -- http://127.0.0.1:8080/xxx
	 * @param method
	 *            -- get/post
	 * @param params
	 *            -- Map<参数名, 参数值>
	 * @param header
	 *            -- Map<参数名, 参数值>
	 * @param timeout
	 *            -- 超时时间-单位秒
	 * @return
	 * @throws Exception
	 */
	public static String executeHttp(String url, String method, Map<String, Object> params, Map<String, String> header,
			int timeout) throws Exception {
		URL u = null;
		URLConnection con = null;
		// PrintWriter print = null;
		OutputStreamWriter out = null;
		BufferedReader reader = null;
		String result = "";
		// 构建请求参数
		StringBuffer sb = new StringBuffer();
		if (params != null && !params.isEmpty()) {
			if (method.equalsIgnoreCase("get")) {
				sb.append("?");
			}
			for (Entry<String, Object> e : params.entrySet()) {
				sb.append(e.getKey());
				sb.append("=");
				sb.append(URLEncoder.encode(e.getValue().toString(), "UTF-8"));
				sb.append("&");
			}
		}
		if (method.equalsIgnoreCase("get")) {
			url += sb.toString();
		}
		try {
			u = new URL(url);
			con = u.openConnection();
			con.setUseCaches(false);
			if (header != null) {
				for (Entry<String, String> map : header.entrySet()) {
					con.setRequestProperty(map.getKey(), map.getValue());
				}
			}
			if (timeout > 0) {
				con.setConnectTimeout(timeout * 1000);
				con.setReadTimeout(timeout * 1000);
			}
			if (method.equalsIgnoreCase("post")) {
				// 发送POST请求必须设置如下两行
				con.setDoOutput(true);
				con.setDoInput(true);
				out = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
				// 获取URLConnection对象对应的输出流
				// print = new PrintWriter(con.getOutputStream());
				// 发送请求参数
				out.write(sb.toString());
				// flush输出流的缓冲
				out.flush();
			}
			reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				result += line;
			}
		} finally {
			if (out != null) {
				out.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
		LOGGER.debug("execute http request，URL：{}，Method：{}，Timeout：{}，Params：{}，Header：{}。result：", url, method,
				timeout, params, header, result);
		return result;
	}
}
