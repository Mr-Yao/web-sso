package ly.sso.server.core.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ly.sso.server.core.Configuration;
import ly.sso.server.core.entity.ClientSystem;
import ly.sso.server.core.service.TokenRegularValidator;
import ly.sso.server.core.token.TokenManager.Token;
import ly.sso.server.util.DateUtil;

/**
 * 默认Token定期验证器
 * 
 * @author liyao
 *
 *         2016年12月24日 下午2:34:45
 *
 */
public class DefaultTokenRegularValidator implements TokenRegularValidator {

	private final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	});
	private final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	});
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Override
	public void execute(final Configuration config, final Map<String, Token> dataMap) {
		TIMER.scheduleAtFixedRate(new Runnable() {
			public void run() {
				for (final Entry<String, Token> entry : dataMap.entrySet()) {
					CACHED_THREAD_POOL.execute(new Runnable() {
						@Override
						public void run() {
							String vt = entry.getKey();
							Token token = entry.getValue();
							Date expired = token.getExpired();
							Date now = new Date();

							// 当前时间大于过期时间
							if (now.compareTo(expired) < 0) {
								return;
							}
							// 因为令牌支持自动延期服务，并且应用客户端缓存机制后，
							// 令牌最后访问时间是存储在客户端的，所以服务端向所有客户端发起一次timeout通知，
							// 客户端根据lastAccessTime + tokenTimeout计算是否过期，<br>
							// 若未过期，用各客户端最大有效期更新当前过期时间
							List<ClientSystem> clientSystems = config.getClientSystems();
							Date maxClientExpired = expired;
							for (ClientSystem clientSystem : clientSystems) {
								Date clientExpired = clientSystem.noticeTimeout(vt, config.getTokenTimeout());
								if (clientExpired != null && clientExpired.compareTo(now) > 0) {
									maxClientExpired = maxClientExpired.compareTo(clientExpired) < 0 ? clientExpired
											: maxClientExpired;
								}
							}
							if (maxClientExpired.compareTo(now) > 0) { // 客户端最大过期时间大于当前
								LOGGER.info("{} - TOken自动延期至：{}", vt, DateUtil.dateToString(maxClientExpired));
								token.setExpired(maxClientExpired);
							} else {
								LOGGER.info("清除过期Token：{}", vt);
								// 已过期，清除对应token
								dataMap.remove(vt);
							}
						}
					});
				}
			}
		}, 60, 60, TimeUnit.SECONDS);
	}
}
