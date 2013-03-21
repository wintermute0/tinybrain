package yatan.common;

import java.util.Date;

import org.apache.log4j.Logger;

public class LogUtility {
    public static long logMethodEntrance(Logger logger, String clazz, String method, String[] params, Object... values) {
        long startTime = new Date().getTime();
        if (logger != null) {
            StringBuilder sb = new StringBuilder();
            if (params != null && params.length > 0) {
                sb.append(" [");
                for (int i = 0; i < params.length; i++) {
                    sb.append(params[i]).append(" = ").append(values[i]).append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());
                sb.append("] ");
            }
            logger.debug("Enter method " + clazz + "#" + method + sb.toString());
        }

        return startTime;
    }

    public static void logMethodExit(Logger logger, String clazz, String method, long startTime, boolean hasReturn,
            Object returnValue) {
        if (logger != null) {
            logger.debug("Exit method " + clazz + "#" + method + (hasReturn ? " [return = " + returnValue + "]" : "")
                    + ", cost " + (new Date().getTime() - startTime) + "ms");
        }
    }
}
