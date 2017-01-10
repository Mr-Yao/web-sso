package ly.sso.server.core.entity;

import java.io.Serializable;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ly.sso.server.core.Configuration;
import ly.sso.server.util.HttpUtil;
import ly.sso.server.util.StringUtil;

/**
 * 客户端应用列表
 * 
 * @author liyao
 *
 * @date 2016年12月28日 下午4:04:13
 *
 */
public class ClientSystem implements Serializable {
	private static final long serialVersionUID = -3245029784758889227L;

	private final StringBuffer URL_BUFF = new StringBuffer(100);
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private String id; // 唯一标识
	private String name; // 系统名称

	private String baseUrl; // 应用基路径，代表应用访问起始点
	private String homeUri; // 应用主页面URI，baseUrl + homeUri = 主页URL
	private String innerAddress; // 系统间内部通信地址

	public String getHomeUrl() {
		return baseUrl + homeUri;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getHomeUri() {
		return homeUri;
	}

	public void setHomeUri(String homeUri) {
		this.homeUri = homeUri;
	}

	public String getInnerAddress() {
		return innerAddress;
	}

	public void setInnerAddress(String innerAddress) {
		this.innerAddress = innerAddress;
	}

	@Override
	public String toString() {
		return "\r\n" + super.toString() + " [" + "\r\n	id = " + id + ",\r\n	name = " + name + ",\r\n	baseUrl = "
				+ baseUrl + ",\r\n	homeUri = " + homeUri + ",\r\n	innerAddress = " + innerAddress + "\r\n]";
	}

	/**
	 * 与客户端系统通信，通知客户端token过期
	 * 
	 * @param tokenTimeout
	 * @return 延期的有效期
	 */
	public Date noticeTimeout(String vt, int tokenTimeout) {

		try {
			URL_BUFF.setLength(0);
			URL_BUFF.append(this.innerAddress).append(Configuration.getInstance().getClientTimeoutUri());
			URL_BUFF.append("?vt=").append(vt).append("&tokenTimeout=").append(tokenTimeout);
			String ret = HttpUtil.executeHttp(URL_BUFF.toString(), "get", null, null, 1);
			LOGGER.debug("notify the client token timeout.{},Token:{},result:{}", this, vt, ret);
			if (StringUtil.isBlank(ret)) {
				return null;
			} else {
				return new Date(Long.parseLong(ret));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 通知客户端用户退出
	 */
	public boolean noticeLogout(String vt) {
		try {
			URL_BUFF.setLength(0);
			URL_BUFF.append(this.innerAddress).append(Configuration.getInstance().getClientLogoutUri());
			URL_BUFF.append("?vt=").append(vt);
			String ret = HttpUtil.executeHttp(URL_BUFF.toString(), "get", null, null, 1);
			LOGGER.debug("notify the client user logout.{},Token:{},result:{}", this, vt, ret);
			return Boolean.parseBoolean(ret);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 通知客户端服务端关闭，客户端收到信息后执行清除缓存操作
	 */
	public boolean noticeShutdown() {
		try {
			URL_BUFF.setLength(0);
			URL_BUFF.append(this.innerAddress).append(Configuration.getInstance().getClientShutdownUri());
			String ret = HttpUtil.executeHttp(URL_BUFF.toString(), "get", null, null, 1);
			LOGGER.debug("notify the client server shut down.{},result:{}", this, ret);
			return Boolean.parseBoolean(ret);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
