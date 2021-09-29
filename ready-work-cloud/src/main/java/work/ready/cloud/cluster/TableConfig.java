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

package work.ready.cloud.cluster;

import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;

import java.util.*;

public class TableConfig {

    static final String MODE = "MODE"; 
    static final String TEMPLATE = "TEMPLATE";
    static final String BACKUPS = "BACKUPS";
    static final String ATOMICITY = "ATOMICITY";
    static final String WRITE_SYNCHRONIZATION_MODE = "WRITE_SYNCHRONIZATION_MODE";
    static final String CACHE_GROUP = "CACHE_GROUP";
    static final String AFFINITY_KEY = "AFFINITY_KEY";
    static final String CACHE_NAME = "CACHE_NAME";
    static final String DATA_REGION = "DATA_REGION";
    static final String KEY_TYPE = "KEY_TYPE";
    static final String VALUE_TYPE = "VALUE_TYPE";
    static final String WRAP_KEY = "WRAP_KEY";
    static final String WRAP_VALUE = "WRAP_VALUE";

    String schema = "PUBLIC";
    private Collection<String> keyField;
    private LinkedList<QueryIndex> indexField;
    private CloudDb.TableMode mode;
    private String template;
    private Integer backups;
    private String atomicity;
    private String writeSynchronizationMode;
    private String cacheGroup;
    private String affinityKey;
    private String tableName; 
    private String dataRegion;

    public Collection<String> getKeyField() {
        return keyField;
    }

    public TableConfig setKeyField(Collection<String> keyField) {
        this.keyField = keyField;
        return this;
    }

    public TableConfig setKeyField(String... keyField) {
        if(this.keyField == null) this.keyField = new ArrayList<>();
        this.keyField.addAll(Arrays.asList(keyField));
        return this;
    }

    public LinkedList<QueryIndex> getIndexField() {
        return indexField;
    }

    public TableConfig setIndexField(String field){
        return setIndexField(field, QueryIndexType.SORTED, true, null);
    }

    public TableConfig setIndexField(String field, boolean asc){
        return setIndexField(field, QueryIndexType.SORTED, asc, null);
    }

    public TableConfig setIndexField(String field, boolean asc, String name){
        return setIndexField(field, QueryIndexType.SORTED, asc, name);
    }

    public TableConfig setIndexField(String field, QueryIndexType type, boolean asc, String name) {
        QueryIndex index = new QueryIndex(field, type, asc, name);
        if(this.indexField == null) this.indexField = new LinkedList<>();
        this.indexField.add(index);
        return this;
    }

    public TableConfig setIndexField(Collection<String> fields, QueryIndexType type) {
        QueryIndex index = new QueryIndex(fields, type);
        if(this.indexField == null) this.indexField = new LinkedList<>();
        this.indexField.add(index);
        return this;
    }

    public TableConfig setIndexField(LinkedHashMap<String, Boolean> fields, QueryIndexType type) {
        QueryIndex index = new QueryIndex(fields, type);
        if(this.indexField == null) this.indexField = new LinkedList<>();
        this.indexField.add(index);
        return this;
    }

    public CloudDb.TableMode getMode() {
        return mode;
    }

    public TableConfig setMode(CloudDb.TableMode mode) {
        this.mode = mode;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public TableConfig setTemplate(String template) {
        this.template = template;
        return this;
    }

    public Integer getBackups() {
        return backups;
    }

    public TableConfig setBackups(Integer backups) {
        this.backups = backups;
        return this;
    }

    public String getAtomicity() {
        return atomicity;
    }

    public TableConfig setAtomicity(String atomicity) {
        this.atomicity = atomicity;
        return this;
    }

    public String getWriteSynchronizationMode() {
        return writeSynchronizationMode;
    }

    public TableConfig setWriteSynchronizationMode(String writeSynchronizationMode) {
        this.writeSynchronizationMode = writeSynchronizationMode;
        return this;
    }

    public String getCacheGroup() {
        return cacheGroup;
    }

    public TableConfig setCacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        return this;
    }

    public String getAffinityKey() {
        return affinityKey;
    }

    public TableConfig setAffinityKey(String affinityKey) {
        this.affinityKey = affinityKey;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public TableConfig setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public String getDataRegion() {
        return dataRegion;
    }

    public TableConfig setDataRegion(String dataRegion) {
        this.dataRegion = dataRegion;
        return this;
    }
}
