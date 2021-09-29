/**
 *
 * Original work Copyright core-ng
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.module.system;

import work.ready.core.handler.Controller;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.scheduler.Scheduler;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

public class SchedulerController extends Controller {
    private static final Log logger = LogFactory.getLog(SchedulerController.class);

    @Autowired
    private Scheduler scheduler;

    public Result jobs() {

        var response = new ListJobResponse();
        scheduler.tasks.forEach((name, trigger) -> {
            var job = new ListJobResponse.JobView();
            job.name = trigger.name();
            job.jobClass = trigger.job().getClass().getCanonicalName();
            job.trigger = trigger.trigger();
            response.jobs.add(job);
        });
        return Success.of(response);
    }

    public Result triggerJob() {

        String job = getRequest().getPathParameter("job");
        logger.warn("trigger job manually, job=%s", job);
        scheduler.triggerNow(job);
        return Success.of("job triggered, job=" + job);
    }
}
