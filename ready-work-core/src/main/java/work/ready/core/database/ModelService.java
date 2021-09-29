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

import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ModelService<M extends Model<M>> {

    protected static final int ACTION_ADD = 1;
    protected static final int ACTION_DEL = 2;
    protected static final int ACTION_UPDATE = 3;

    public static final Pattern sqlPlaceHolderPattern = Pattern.compile("(?i)\\(\\s*\\?\\s*\\)|(\\s+(where|order\\s+by|group\\s+by)\\s+)?\\?(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)");
    protected final DatabaseManager dbManager;
    protected final Db db;
    protected final Class<M> modelClass;

    private final Map<String, M> daoCache = new HashMap<>();
    protected final M dao;

    public ModelService() {
        this.dbManager = Ready.dbManager();
        db = dbManager.getDb();
        modelClass = ClassUtil.getGenericClass(getClass());
        dao = initDao();
    }

    protected M initDao() {
        if (modelClass == null) {
            throw new RuntimeException("can not get model class name");
        }
        return dbManager.createModel(modelClass).dao();
    }

    public M use(String configName) {
        M m = this.daoCache.get(configName);
        if (m == null) {
            synchronized (configName.intern()) {
                m = this.daoCache.get(configName);
                if (m == null) {
                    m = dao.copy()._use(configName).dao();
                    this.daoCache.put(configName, m);
                }
            }
        }
        return m;
    }

    public M getDao() {
        return dao;
    }

    public Class<M> getModelClass(){
        return modelClass;
    }

    public Db getDb() { return db; }

    public M createModel(){
        return dbManager.createModel(modelClass);
    }

    public M findById(Object id) {
        return dao.findById(id);
    }

    public List<M> findAll() {
        return dao.findAll();
    }

    public boolean deleteById(Object id) {
        boolean result = dao.deleteById(id);
        if (result) {
            shouldUpdateCache(ACTION_DEL, null, id);
        }
        return result;
    }

    public boolean delete(M model) {
        boolean result = model.delete();
        if (result) {
            shouldUpdateCache(ACTION_DEL, model, model._getIdValue());
        }
        return result;
    }

    public Object save(M model) {
        boolean result = model.save();
        if (result) {
            shouldUpdateCache(ACTION_ADD, model, model._getIdValue());
            return model._getIdValue();
        }
        return null;
    }

    public Object saveOrUpdate(M model) {
        if (model._getIdValue() == null) {
            return save(model);
        } else if (update(model)) {
            return model._getIdValue();
        }
        return null;
    }

    public boolean update(M model) {
        boolean result = model.update();
        if (result) {
            shouldUpdateCache(ACTION_UPDATE, model, model._getIdValue());
        }
        return result;
    }

    public void shouldUpdateCache(int action, Model model, Object id) {
    }
}
