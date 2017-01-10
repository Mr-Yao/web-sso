package ly.sso.server.core.service;

import java.util.Map;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.token.TokenManager.Token;

/**
 * Token 定期验证器
 * 
 * @author liyao
 *
 *         2016年12月21日 下午6:33:54
 *
 */
public interface TokenRegularValidator {

	/**
	 * 执行方法
	 * 
	 * @param config
	 * @param dataMap
	 */
	public void execute(final Configuration config, final Map<String, Token> dataMap);
}
