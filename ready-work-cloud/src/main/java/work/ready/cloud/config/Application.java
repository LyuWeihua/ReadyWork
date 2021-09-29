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

public class Application {

    private String projectName;
    private String projectVersion;
    private String applicationVersion;
    private String applicationName;
    private String profile;

    public Application() {
    }

    @JsonProperty("applicationVersion")
    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @JsonProperty("profile")
    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    @JsonProperty("applicationName")
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    @JsonProperty("projectName")
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectId) {
        this.projectName = projectId;
    }
    @JsonProperty("projectVersion")
    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Application Application = (Application) o;

        return Objects.equals(applicationVersion, Application.applicationVersion) &&
        Objects.equals(profile, Application.profile) &&
        Objects.equals(applicationName, Application.applicationName) &&
        Objects.equals(projectName, Application.projectName) &&
        Objects.equals(projectVersion, Application.projectVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationVersion, profile, applicationName, projectName,  projectVersion);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class application {\n");
        sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
        sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
        sb.append("    applicationName: ").append(toIndentedString(applicationName)).append("\n");
        sb.append("    applicationVersion: ").append(toIndentedString(applicationVersion)).append("\n");
        sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
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
