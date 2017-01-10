package ly.sso.server.core.listener;

import java.lang.reflect.Constructor;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ly.sso.server.core.Configuration;
import ly.sso.server.util.StringUtil;

/**
 * SSO服务端监听器。 <br>
 * 1、服务器启动时用来初始化配置文件名称。<br>
 * 2、服务器正常停止时会通知所有客户端清除Token。<br>
 * 如果不想使用默认的配置文件名称可以通过web.xml进行配置（以下配置为默认值，如果不作修改可不进行配置）：
 * 
 * <pre>
 *&lt;context-param>
 *  &lt;param-name>SSOServerConfigName&lt;/param-name>
 *  &lt;param-value>sso-server.properties&lt;/param-value>
 *&lt;/context-param>
 *&lt;context-param>
 *  &lt;param-name>clientListConfigName&lt;/param-name>
 *  &lt;param-value>client_systems.xml&lt;/param-value>
 *&lt;/context-param>
 * </pre>
 * 
 * 如果使用Spring框架管理Configuration时，使用destroy-method="destroy"，则可以不用配置该监听器
 * 
 * @author liyao
 *
 * @date 2017年1月9日 下午3:34:51
 *
 */
public class SSOListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		String SSOServerConfigName = sce.getServletContext().getInitParameter("SSOServerConfigName");
		String clientListConfigName = sce.getServletContext().getInitParameter("clientListConfigName");
		if (StringUtil.isBlank(SSOServerConfigName)) {
			SSOServerConfigName = "sso-server.properties";
		}
		if (StringUtil.isBlank(clientListConfigName)) {
			clientListConfigName = "client_systems.xml";
		}

		try {
			Constructor<Configuration> config = Configuration.class.getDeclaredConstructor(String.class, String.class);
			config.setAccessible(true);
			config.newInstance(SSOServerConfigName, clientListConfigName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Configuration.getInstance().destroy();
	}
}
