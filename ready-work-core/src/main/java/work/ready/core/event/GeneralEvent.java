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

package work.ready.core.event;

import work.ready.core.tools.DateUtil;
import work.ready.core.tools.StrUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GeneralEvent extends BaseEvent<Map<String, Object>> {

    private final String name;
    private boolean global = false;

    private boolean skip = false; 
    private boolean internal = true;

    public GeneralEvent(String name){
        super(new HashMap<String, Object>());
        this.name = name;
    }

    public <S> GeneralEvent(String name, S sender){
        super(new HashMap<String, Object>());
        this.name = name;
        put("sender", sender);
    }

    public <S,T> GeneralEvent(String name, S sender, T object){
        super(new HashMap<String, Object>());
        this.name = name;
        put("sender", sender);
        put("object", object);
    }

    public String getName() { return name; }

    public boolean isGlobal() {
        return global;
    }

    public GeneralEvent setGlobal(boolean global) {
        this.global = global;
        return this;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isSkip() {
        return skip;
    }

    public GeneralEvent setSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    public <S> GeneralEvent setSender(S sender){
        put("sender", sender);
        return this;
    }

    public <S> S getSender(){
        return (S) get("sender");
    }

    public <T> GeneralEvent setObject(T object){
        put("object", object);
        return this;
    }

    public <T> T getObject(){
        return (T) get("object");
    }

    public GeneralEvent setContextReference(Object object){
        put("contextReference", object);
        return this;
    }

    public Object getContextReference(){
        return get("contextReference");
    }

    public GeneralEvent put(String key, Object value){
        ((Map)source).put(key, value);
        return this;
    }

    public Object get(String key){
        return ((Map)source).get(key);
    }

    public String toMessage(){
        StringBuilder sb = new StringBuilder();
        sb.append(name != null ? name : "");
        sb.append("`");
        sb.append(global);
        source.forEach((key,val)-> {
            sb.append("`");
            sb.append(key);
            if (val instanceof Integer) {
                sb.append("~Integer~");
            } else if (val instanceof String) {
                sb.append("~String~");
                if(((String) val).indexOf('`') > 0 || ((String) val).indexOf('~') > 0){
                    throw new RuntimeException("Global GeneralEvent doesn't support special characters '`' or '~' in this parameter: " + key + "=>" + val);
                }
            } else if (val instanceof Double) {
                sb.append("~Double~");
            } else if (val instanceof Float) {
                sb.append("~Float~");
            } else if (val instanceof Long) {
                sb.append("~Long~");
            } else if (val instanceof Boolean) {
                sb.append("~Boolean,");
            } else if (val instanceof Date) {
                sb.append("~Date~");
            } else {
                throw new RuntimeException("Global GeneralEvent doesn't support this parameter: " + key + "=>" + val +
                        ", Global GeneralEvent currently only support Integer, String, Double, Float, Long, Boolean, and Date type of parameters.");
            }
            sb.append(val);
        });
        return sb.toString();
    }

    public static GeneralEvent fromMessage(String str){
        String[] fields = StrUtil.split(str,'`');
        if(fields.length < 2) throw new RuntimeException("Bad GeneralEvent String: " + str);
        GeneralEvent event = new GeneralEvent(fields[0]);
        event.setGlobal(Boolean.parseBoolean(fields[1]));
        for(int i = 2; i < fields.length; i++) {
            String[] params = StrUtil.split(fields[i], '~');
            if("Integer".equals(params[1])){
                event.put(params[0], Integer.parseInt(params[2]));
            } else if("String".equals(params[1])){
                event.put(params[0], params[2]);
            } else if("Double".equals(params[1])){
                event.put(params[0], Double.parseDouble(params[2]));
            } else if("Float".equals(params[1])){
                event.put(params[0], Float.parseFloat(params[2]));
            } else if("Long".equals(params[1])){
                event.put(params[0], Long.parseLong(params[2]));
            } else if("Boolean".equals(params[1])){
                event.put(params[0], Boolean.parseBoolean(params[2]));
            } else if("Date".equals(params[1])){
                event.put(params[0], DateUtil.parse(params[2]));
            }
        }
        return event;
    }
}

