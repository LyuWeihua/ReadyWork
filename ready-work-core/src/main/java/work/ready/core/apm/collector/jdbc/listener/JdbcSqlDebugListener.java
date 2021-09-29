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

package work.ready.core.apm.collector.jdbc.listener;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.jdbc.JdbcConfig;
import work.ready.core.apm.collector.jdbc.JdbcContext;
import work.ready.core.apm.common.SamplingUtil;
import work.ready.core.apm.common.SpanManager;
import work.ready.core.apm.common.TraceContext;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.database.SqlDebugger;

import java.sql.SQLException;
import java.time.Duration;

public class JdbcSqlDebugListener implements SqlDebugger {

    @Override
    public void beforeAudit(String datasource, String sql) {
        if(!ApmManager.getConfig(JdbcConfig.class).isEnabled() || !JdbcContext.isOn() || SamplingUtil.NO()){
            return;
        }
        Span span = JdbcContext.getJdbcSpan();
        String gid = TraceContext.getCorrelationId();
        if(span == null || !gid.equals(span.getCorrelationId())) {
            span = SpanManager.createLocalSpan(SpanType.SQL);
            JdbcContext.setJdbcSpan(span);
            span.addTag("source", datasource);
            span.addTag("sql", sql);
        }
    }

    @Override
    public void beforeExecute(String datasource, String sql) {
        Span span = JdbcContext.getJdbcSpan();
        if (span == null || !ApmManager.getConfig(JdbcConfig.class).isEnabled() || SamplingUtil.NO()) {
            return;
        }
        span.addTag("sqlWithValues", sql);
    }

    @Override
    public void afterExecute(long timeElapsedNanos, SQLException e) {
        Span span = JdbcContext.getJdbcSpan();
        JdbcContext.remove();
        if (span == null || !ApmManager.getConfig(JdbcConfig.class).isEnabled() || SamplingUtil.NO()) {
            return;
        }

        if (e == null) {
            span.addTag("status", "Y");
        } else {
            span.addTag("status", "N");
        }

        if (Duration.ofNanos(timeElapsedNanos).toMillis() > ApmManager.getConfig(JdbcConfig.class).getSpend()) {
            span.addTag("elapsed", Duration.ofNanos(timeElapsedNanos).toMillis() + "ms");
            ApmManager.getConfig().fillEnvInfo(span);
            ReporterManager.report(span);
        }
    }

}
