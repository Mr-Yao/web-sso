package ly.sso.server.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期工具类
 * 
 * @author liyao
 *
 *         2016年12月21日 下午11:34:23
 *
 */
public class DateUtil {

	public static String dateToString(Date date, String pattern) {
		return simpleDateFormat(pattern).format(date);
	}

	public static String dateToString(Date date) {
		return simpleDateFormat(null).format(date);
	}

	public static Date stringToDate(String strDate, String pattern) throws ParseException {
		return simpleDateFormat(pattern).parse(strDate);
	}

	public static Date stringToDate(String strDate) throws ParseException {
		return simpleDateFormat(null).parse(strDate);
	}

	private static final ThreadLocal<Map<String, SimpleDateFormat>> LOCAL_FORMAT_MAP = new ThreadLocal<Map<String, SimpleDateFormat>>() {
		@Override
		protected Map<String, SimpleDateFormat> initialValue() {
			Map<String, SimpleDateFormat> map = new HashMap<String, SimpleDateFormat>();
			map.put(DEFAULT_PATTERN, new SimpleDateFormat(DEFAULT_PATTERN));
			return map;
		}
	};

	public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private static SimpleDateFormat simpleDateFormat(String pattern) {
		if (StringUtil.isBlank(pattern)) {
			return LOCAL_FORMAT_MAP.get().get(DEFAULT_PATTERN);
		}
		SimpleDateFormat format = LOCAL_FORMAT_MAP.get().get(pattern);
		if (format != null) {
			return format;
		}
		format = new SimpleDateFormat(pattern);
		LOCAL_FORMAT_MAP.get().put(pattern, format);
		return format;
	}

}
