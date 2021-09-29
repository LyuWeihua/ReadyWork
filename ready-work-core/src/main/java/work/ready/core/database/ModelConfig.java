/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.database;

import work.ready.core.config.BaseConfig;

public class ModelConfig extends BaseConfig {

    private String createTimeColumn = "CREATE_TIME"; 
    private String createUserColumn = "CREATE_USER"; 
    private String updateTimeColumn = "UPDATE_TIME"; 
    private String updateUserColumn = "UPDATE_USER"; 
    private String versionColumn = "VERSION"; 
    private String statusColumn = "STATUS"; 
    private int invalidStatusUpperBound = 0; 

    public String getCreateTimeColumn() {
        return createTimeColumn;
    }

    public void setCreateTimeColumn(String createTimeColumn) {
        this.createTimeColumn = createTimeColumn;
    }

    public String getCreateUserColumn() {
        return createUserColumn;
    }

    public void setCreateUserColumn(String createUserColumn) {
        this.createUserColumn = createUserColumn;
    }

    public String getUpdateTimeColumn() {
        return updateTimeColumn;
    }

    public void setUpdateTimeColumn(String updateTimeColumn) {
        this.updateTimeColumn = updateTimeColumn;
    }

    public String getUpdateUserColumn() {
        return updateUserColumn;
    }

    public void setUpdateUserColumn(String updateUserColumn) {
        this.updateUserColumn = updateUserColumn;
    }

    public String getVersionColumn() {
        return versionColumn;
    }

    public void setVersionColumn(String versionColumn) {
        this.versionColumn = versionColumn;
    }

    public String getStatusColumn() {
        return statusColumn;
    }

    public void setStatusColumn(String statusColumn) {
        this.statusColumn = statusColumn;
    }

    public int getInvalidStatusUpperBound() {
        return invalidStatusUpperBound;
    }

    public void setInvalidStatusUpperBound(int invalidStatusUpperBound) {
        this.invalidStatusUpperBound = invalidStatusUpperBound;
    }

    @Override
    public void validate() { 

    }
}
