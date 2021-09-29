/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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
package work.ready.core.handler;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.HttpUtil;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public final class ContentType {

    private static Charset defaultCharset = Ready.getMainApplicationConfig().getStandardCharset();

    public static final ContentType ANY_TYPE = create("*/*", defaultCharset);
    public static final ContentType TEXT_HTML = create("text/html", defaultCharset);
    public static final ContentType TEXT_CSS = create("text/css", defaultCharset);
    public static final ContentType TEXT_PLAIN = create("text/plain", defaultCharset);
    public static final ContentType TEXT_XML = create("text/xml", defaultCharset);
    public static final ContentType APPLICATION_JSON = create("application/json", defaultCharset);
    public static final ContentType APPLICATION_CBOR = create("application/cbor", defaultCharset);
    public static final ContentType APPLICATION_SMILE = create("application/smile", defaultCharset);
    public static final ContentType APPLICATION_JAVASCRIPT = create("application/javascript", defaultCharset);
    // form body content type doesn't use charset normally, refer to https://www.w3.org/TR/html5/sec-forms.html#urlencoded-form-data
    public static final ContentType APPLICATION_FORM_URLENCODED = create("application/x-www-form-urlencoded", null);
    public static final ContentType APPLICATION_OCTET_STREAM = create("application/octet-stream", null);
    public static final ContentType IMAGE_PNG = create("image/png", null);

    private static final Log logger = LogFactory.getLog(ContentType.class);
    // cache most common ones to save paring time
    private static final Map<String, ContentType> CACHE = new HashMap<>(Map.of(
        APPLICATION_JSON.contentType, APPLICATION_JSON,
        TEXT_HTML.contentType, TEXT_HTML
    ));
    private static final Map<String, ContentType> TYPE_FOR_TRANSMISSION = Map.of(
            APPLICATION_JSON.mediaType, APPLICATION_JSON,
            APPLICATION_CBOR.mediaType, APPLICATION_CBOR,
            APPLICATION_SMILE.mediaType, APPLICATION_SMILE
    );

    public static Charset charset(){
        return defaultCharset;
    }

    // only cover common case, assume pattern is "media-type; charset=" or "multipart/form-data; boundary="
    public static ContentType parse(String contentType) {
        ContentType type = CACHE.get(contentType);
        if (type != null) return type;

        synchronized (ContentType.class) {
            type = CACHE.get(contentType);
            if (type != null) return type;

            String mediaType = contentType;
            Charset charset = null;

            int firstSemicolon = contentType.indexOf(';');
            if (firstSemicolon > 0) {
                mediaType = contentType.substring(0, firstSemicolon);

                int charsetStartIndex = contentType.indexOf("charset=", firstSemicolon + 1);
                if (charsetStartIndex > 0) {
                    int charsetEndIndex = contentType.indexOf(';', charsetStartIndex + 8);
                    charset = HttpUtil.parseCharset(contentType.substring(charsetStartIndex + 8, charsetEndIndex == -1 ? contentType.length() : charsetEndIndex));
                }
            }
            type = new ContentType(contentType, mediaType, charset);
            CACHE.put(contentType, type);
        }
        return type;
    }

    public static ContentType parseForTransmission(String contentType) {
        ContentType type = parse(contentType);
        if(TYPE_FOR_TRANSMISSION.containsKey(type.getMediaType())) {
            return type;
        } else {
            return null;
        }
    }

    public static ContentType create(String mediaType, Charset charset) {
        String contentType = charset == null ? mediaType : mediaType + "; charset=" + charset.name().toLowerCase();
        return new ContentType(contentType, mediaType, charset);
    }

    private final String mediaType;
    private final String contentType;
    private final Charset charset;

    private ContentType(String contentType, String mediaType, Charset charset) {
        this.contentType = contentType;
        this.mediaType = mediaType;
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }

    public String getMediaType() {
        return mediaType;
    }

    @Override
    public String toString() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentType type = (ContentType) o;

        if (charset != null ? !charset.equals(type.getCharset()) : type.getCharset() != null) return false;
        return mediaType != null ? mediaType.equals(type.getMediaType()) : type.getMediaType() == null;
    }

    @Override
    public int hashCode() {
        int result = mediaType != null ? mediaType.hashCode() : 0;
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        return result;
    }

    private static final Map<String, ContentType> mapping = initMapping();

    /**
     * 将简写的文本射到 context type，方便在 Controller.renderText(String, String)
     * 之中取用，例如：
     *   renderText(..., "xml")
     *   比下面的用法要省代码
     *   renderText(..., "text/xml")
     */
    private static Map<String, ContentType> initMapping() {
        Map<String, ContentType> ret = new HashMap<>();
        ret.put("txt", TEXT_PLAIN);
        ret.put("text", TEXT_PLAIN);
        ret.put("plain", TEXT_PLAIN);
        ret.put("html", TEXT_HTML);
        ret.put("htm", TEXT_HTML);
        ret.put("xml", TEXT_XML);
        ret.put("json", APPLICATION_JSON);
        ret.put("cbor", APPLICATION_CBOR);
        ret.put("smile", APPLICATION_SMILE);
        ret.put("javascript", APPLICATION_JAVASCRIPT);
        ret.put("js", APPLICATION_JAVASCRIPT);

        return ret;
    }

    public static ContentType create(String str) {
        return mapping.get(str.toLowerCase());
    }
}
