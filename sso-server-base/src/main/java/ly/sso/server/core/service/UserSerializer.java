package ly.sso.server.core.service;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import ly.sso.server.core.entity.LoginUser;

/**
 * 用户实体序列化器
 * 
 * @author liyao
 *
 * @date 2016年12月27日 下午5:16:02
 *
 */
public abstract class UserSerializer {
	/**
	 * 
	 * @author liyao
	 *
	 * @date 2016年12月27日 下午5:17:17
	 *
	 */
	protected class UserData {
		/** 唯一标识 */
		private String id;
		/** 其它属性 */
		private Map<String, Object> properties = new HashMap<String, Object>();

		public UserData() {
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, Object> properties) {
			this.properties.putAll(properties);
		}

		/** 新增单个属性 */
		public void setProperty(String key, Object value) {
			this.properties.put(key, value);
		}

		/** 获取单个属性 */
		public void getProperty(String key) {
			this.properties.get(key);
		}
	}

	/**
	 * 数据转换。将继承了LoginUser的实体转为UserData
	 * 
	 * @param loginUser
	 * @return
	 * @throws Exception
	 */
	protected abstract UserData conversion(LoginUser loginUser) throws Exception;

	/**
	 * 序列化
	 * 
	 * @param loginUser
	 * @return
	 * @throws Exception
	 */
	public String serialization(LoginUser loginUser) throws Exception {
		if (loginUser == null) {
			return null;
		}
		UserData userData = this.conversion(loginUser);
		return JSON.toJSONString(userData);
	}
}
