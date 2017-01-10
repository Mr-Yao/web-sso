package ly.sso.server.core.service;

import java.util.Set;

import ly.sso.server.core.entity.Credential;
import ly.sso.server.core.entity.LoginUser;

/**
 * 身份验证处理器
 * 
 * @author liyao
 *
 * @date 2017年1月5日 下午4:06:27
 *
 */
public interface AuthenticationHandler {
	/**
	 * 自动登录验证
	 * 
	 * @param lt
	 * @return
	 * @throws Exception
	 */
	public LoginUser autoLogin(String lt) throws Exception;

	/**
	 * 获取当前登录用户可用系统ID列表
	 * 
	 * @param loginUser
	 * @return 返回null表示全部
	 * @throws Exception
	 */
	public Set<String> authedSystemIds(LoginUser loginUser) throws Exception;

	/**
	 * 生成自动登录标识
	 * 
	 * @param loginUser
	 * @return
	 * @throws Exception
	 */
	public String generateLoginTicket(LoginUser loginUser) throws Exception;

	/**
	 * 身份验证，登录时对用户身份验证
	 * 
	 * @param credential
	 * @return 授权成功返回LoginUser, 否则返回null
	 */
	public LoginUser authentication(Credential credential);

	/**
	 * 清除用户自动登录信息
	 * 
	 * @param loginUser
	 */
	public void clearLoginTicket(LoginUser loginUser);

}
