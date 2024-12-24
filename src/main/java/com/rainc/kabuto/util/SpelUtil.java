package com.rainc.kabuto.util;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.lang.SimpleCache;
import cn.hutool.json.JSONObject;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rainc
 * @date 2023/4/11
 */
public class SpelUtil {
    public static SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
    public static SimpleCache<String, Expression> expressionCache = new SimpleCache<>();

    public static String analyseSpel(Map<String, Object> params, String spel) {
        Expression expression = expressionCache.get(spel);
        if (expression == null) {
            synchronized (expressionCache) {
                expression = expressionCache.get(spel);
                if (expression == null) {
                    expression = spelExpressionParser.parseExpression(spel);
                }
            }
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(params);
        Object value = expression.getValue(context);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

}
