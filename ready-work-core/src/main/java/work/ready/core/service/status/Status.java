/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.core.service.status;

import work.ready.core.server.Ready;
import work.ready.core.component.i18n.Res;

import java.util.IllegalFormatException;
import java.util.Map;
import java.util.MissingResourceException;

import static work.ready.core.tools.StrUtil.format;

public class Status {
    
    private static transient final String defaultSeverity = "ERROR";

    private int httpCode;
    private String code;
    private String severity;
    private String message;
    private String description;

    private transient Res i18nRes;
    private transient Object[] args;

    public Status() {
    }

    public Status(final String code, final Object... args) {
        this.code = code;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) Ready.config().getStatusConfig().get(code);
        if (map != null) {
            this.httpCode = (Integer) map.get("httpCode");
            this.code = (String) map.get("code");
            this.message = (String) map.get("message");
            this.description = (String) map.get("description");
            this.args = args;
            if ((this.severity = (String) map.get("severity")) == null)
                this.severity = defaultSeverity;
        } else {
            throw new RuntimeException("status code " + code + " is not defined in status config file.");
        }
    }

    public Status(int httpCode, String code, String message, String description) {
        this.httpCode = httpCode;
        this.code = code;
        this.severity = defaultSeverity;
        this.message = message;
        this.description = description;
    }

    public Status(int httpCode, String code, String message, String description, String severity) {
        this.httpCode = httpCode;
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.description = description;
    }

    public Status setI18n(Res i18nRes){
        if(i18nRes != null)
            this.i18nRes = i18nRes;
        return this;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public Status setHttpCode(int httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Status setCode(String code) {
        this.code = code;
        return this;
    }

    private String getI18nKey(String type){
        return getI18nKey(type, null);
    }

    private String getI18nKey(String type, String str){
        StringBuilder sb = new StringBuilder();
        sb.append("status.");
        sb.append(this.code);
        sb.append('.');
        sb.append(type);
        if(str != null) {
            sb.append('.');
            char dropChar = 0;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    if (dropChar > 0) sb.append('_');
                    sb.append(c);
                    dropChar = 0;
                } else {
                    dropChar = c;
                }
            }
        }
        return sb.toString();
    }

    public String getMessage() {
        try {
            if(this.i18nRes != null) {
                return this.i18nRes.format(getI18nKey("message"), this.args);
            } else {
                return format(this.message, args);
            }
        } catch (MissingResourceException e) {
            return format(this.message, args);
            
        } catch (IllegalFormatException e) {
            
            return this.message;
        }
    }

    public Status setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getDescription() {
        try {
            if(this.i18nRes != null) {
                return this.i18nRes.format(getI18nKey("description"), this.args);
            } else {
                return format(this.description, args);
            }
        } catch (MissingResourceException e) {
            return format(this.description, args);
            
        } catch (IllegalFormatException e) {
            
            return this.description;
        }
    }

    public Status setDescription(String description) {
        this.description = description;
        return this;
    }

    public Status setSeverity(String severity) {
        this.severity = severity;
        return this;
    }

    public String getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "{\"httpCode\":" + getHttpCode()
                + ",\"code\":\"" + getCode()
                + "\",\"message\":\"" + getMessage()
                + "\",\"description\":\"" + getDescription()
                + "\",\"severity\":\"" + getSeverity() + "\"}";
    }

    public String toXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<status>\n" +
                "<httpCode>" + getHttpCode() + "</httpCode>\n" +
                "<code>" + getCode() + "</code>\n" +
                "<severity>" + getSeverity() + "</severity>\n" +
                "<message>" + getMessage() + "</message>\n" +
                "<description>" + getDescription() + "</description>\n" +
                "</status>";
    }
}
