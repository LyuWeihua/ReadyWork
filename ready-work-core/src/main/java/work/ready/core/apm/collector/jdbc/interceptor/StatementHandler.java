/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.core.apm.collector.jdbc.interceptor;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.jdbc.JdbcConfig;
import work.ready.core.apm.collector.jdbc.JdbcContext;
import work.ready.core.apm.common.SamplingUtil;
import work.ready.core.apm.common.TraceHandler;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.model.Tags;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatementHandler implements TraceHandler {

    private static final Log logger = LogFactory.getLog(StatementHandler.class);

    @Override
    public Span before(String className, String methodName, Object[] arguments, Object[] extraParam) {
        if(methodName.startsWith("execute")) {
            
            return handleBeforeExecute(arguments);
        } else if(methodName.startsWith("set")) {
            
            return handleBeforeSet(arguments);
        }
        return null;
    }

    @Override
    public Object after(String className, String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam) {
        if(methodName.startsWith("execute")) {
            
            handleAfterExecute(result, t);
        } else if(methodName.startsWith("set")) {
            
        }
        return result;
    }

    private Span handleBeforeSet(Object[] arguments) {
        if(!ApmManager.getConfig(JdbcConfig.class).isEnabled() || SamplingUtil.NO()){
            return null;
        }
        Span span = JdbcContext.getJdbcSpan();
        if(span != null && arguments.length > 1 && ApmManager.getConfig(JdbcConfig.class).isEnableParam()) {
            LinkedHashMap<String,Object> params = (LinkedHashMap<String,Object>)span.getTag(Tags.PARAMS);
            if(params == null){
                params = new LinkedHashMap<>();
                span.addTag(Tags.PARAMS, params);
            }
            String indexKey = arguments[0] + "";
            if(!params.containsKey(indexKey)){
                Object val = arguments[1];
                if(isExcludeType(val)){
                    params.put(indexKey, "{!}");
                } else {
                    params.put(indexKey, val);
                }
            }
        }
        return span;
    }

    public boolean isExcludeType(Object val){
        if(val == null || ClassUtil.isSimpleType(val.getClass())){
            return false;
        }
        return true;
    }

    private Span handleBeforeExecute(Object[] arguments) {
        if (!ApmManager.getConfig(JdbcConfig.class).isEnabled() || SamplingUtil.NO()) {
            return null;
        }
        Span span = JdbcContext.getJdbcSpan();
        if (span != null) {
            span.setTime(Ready.now());
        }
        return span;
    }

    private Span handleAfterExecute(Object result, Throwable t) {
        Span span = JdbcContext.getJdbcSpan();
        JdbcContext.remove();
        if (span == null || !ApmManager.getConfig(JdbcConfig.class).isEnabled() || SamplingUtil.NO()) {
            return null;
        }
        
        if (t == null) {
            span.addTag("status", "Y");
        } else {
            span.addTag("status", "N");
        }
        calculateSpend(span);
        if (span.getSpend() > ApmManager.getConfig(JdbcConfig.class).getSpend()) {
            Map<String, Object> params = (Map<String, Object>) span.getTag(Tags.PARAMS);

            span.removeTag(Tags.PARAMS);
            if (params != null) {
                Span paramSpan = new Span(SpanType.SQL_PARAM);
                paramSpan.setId(span.getId());
                paramSpan.addTag("args", params.toString());
                ReporterManager.report(paramSpan);
            }
            
            span.addTag("count", calcResultCount(result));
            ApmManager.getConfig().fillEnvInfo(span);
            ReporterManager.report(span);
        }
        return span;
    }

    public static String calcResultCount(Object result) {
        if (result == null) {
            return null;
        }
        Object count = null;
        if (result instanceof ResultSet) {
            ResultSet rs = (ResultSet) result;
            try {
                
                if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
                    return "only";
                }
            } catch (Exception e) {
                return "error";
            }
            try {
                rs.last();
                count = rs.getRow();
            } catch (Exception e) {
                logger.warn(e,"ResultSet::last exception");
            } finally {
                try {
                    
                    rs.first();
                } catch (Exception e) {
                    logger.error(e, "ResultSet::absolute exception");
                }
            }
        } else {
            count = result;
        }
        return count.toString();
    }
}
