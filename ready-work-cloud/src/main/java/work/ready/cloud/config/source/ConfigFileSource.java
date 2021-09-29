/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.config.source;

import work.ready.cloud.config.Application;
import work.ready.cloud.config.ApplicationConfigs;
import work.ready.core.exception.ApiException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

public interface ConfigFileSource {

    String RESOURCE_TYPE = "resource_type";
    String PROJECT_NAME = "project_name";
    String PROJECT_VERSION = "project_version";
    String APPLICATION_NAME = "application_name";
    String APPLICATION_VERSION = "application_version";
    String ENVIRONMENT = Constant.TAG_ENVIRONMENT;

    String CONFIG ="config";
    String FILE ="file";
    String CERT ="cert";
    String GLOBAL ="global";

    String login(String authorization) throws ApiException;

    ApplicationConfigs getApplicationConfigs(String authToken, Application application) throws ApiException;

    ApplicationConfigs getApplicationCertificates(String authToken, Application application) throws ApiException;

    ApplicationConfigs getApplicationFiles(String authToken, Application application) throws ApiException;

}
