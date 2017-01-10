package ly.sso.client.core.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 当前登录用户
 * 
 * @author liyao
 *
 * @date 2016年12月28日 下午10:20:35
 *
 */
public class SSOUser implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 唯一标识 */
	private String id;
	/** 其它属性 */
	private Map<String, Object> properties = new HashMap<String, Object>();

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
