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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelCopier {

    public static <M extends Model> List<M> copy(List<M> modelList) {
        if (modelList == null || modelList.isEmpty()) {
            return modelList;
        }

        List<M> list = modelList instanceof ArrayList
                ? new ArrayList<>(modelList.size())
                : newInstance(modelList.getClass());

        for (M m : modelList) {
            list.add(copy(m));
        }

        return list;
    }

    public static <M extends Model> Set<M> copy(Set<M> modelSet) {
        if (modelSet == null || modelSet.isEmpty()) {
            return modelSet;
        }

        Set<M> set = modelSet instanceof HashSet
                ? new HashSet<>(modelSet.size())
                : newInstance(modelSet.getClass());

        for (M m : modelSet) {
            set.add(copy(m));
        }

        return set;
    }

    public static <M extends Model> M[] copy(M[] models) {
        if (models == null || models.length == 0) {
            return models;
        }

        M[] array = (M[]) Array.newInstance(models.getClass().getComponentType(), models.length);
        int i = 0;
        for (M m : models) {
            array[i++] = copy(m);
        }
        return array;
    }

    public static <M extends Model> Page<M> copy(Page<M> modelPage) {
        if (modelPage == null) {
            return null;
        }

        List<M> modelList = modelPage.getList();
        if (modelList == null || modelList.isEmpty()) {
            return modelPage;
        }

        modelPage.setList(copy(modelList));
        return modelPage;
    }

    public static <M extends Model> M copy(M model) {
        return model == null ? null : (M) model.copy();
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("can not newInstance class:" + clazz + "\n" + e.toString(), e);
        }
    }

}
