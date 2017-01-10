package ly.sso.server.core.entity;

/**
 * 登录凭证
 * 
 * @author liyao
 *
 * @date 2017年1月5日 下午4:04:44
 *
 */
public abstract class Credential {

	private String error; // 错误信息

	/**
	 * 获取一个参数值
	 * 
	 * @param name
	 * @return
	 */
	public abstract String getParameter(String name);

	/**
	 * 获取多值参数数组
	 * 
	 * @param name
	 * @return
	 */
	public abstract String[] getParameters(String name);

	/**
	 * 获取session中的值
	 * 
	 * @return
	 */
	public abstract Object getSessionValue(String name);

	/**
	 * 授权失败时，设置失败提示信息
	 * 
	 * @param errorMsg
	 */
	public void setError(String errorMsg) {
		this.error = errorMsg;
	}

	/**
	 * 获取失败提示信息
	 * 
	 * @return
	 */
	public String getError() {
		return this.error;
	}
}
