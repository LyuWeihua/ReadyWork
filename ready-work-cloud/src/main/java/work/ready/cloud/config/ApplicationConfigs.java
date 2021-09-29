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
package work.ready.cloud.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ApplicationConfigs {

    private Object configProperties;
    private Application application;

    public ApplicationConfigs() {
    }

    @JsonProperty("configProperties")
    public Object getConfigProperties() {
        return configProperties;
    }

    public void setConfigProperties(Object configProperties) {
        this.configProperties = configProperties;
    }
    @JsonProperty("application")
    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationConfigs ApplicationConfigs = (ApplicationConfigs) o;

        return Objects.equals(configProperties, ApplicationConfigs.configProperties) &&

        Objects.equals(application, ApplicationConfigs.application);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configProperties, application);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApplicationConfigs {\n");
        sb.append("    configProperties: ").append(toIndentedString(configProperties)).append("\n");        sb.append("    application: ").append(toIndentedString(application)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
