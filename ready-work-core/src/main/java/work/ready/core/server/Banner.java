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
package work.ready.core.server;

import work.ready.core.module.Version;

import java.io.*;

class Banner {

    public static String getBanner(String file) {
        String path = getRootPath();
        if(path != null) {
            File bannerFile = new File(path, file);
            if (bannerFile.exists() && bannerFile.canRead()) {
                String bannerText = readString(bannerFile);
                if (bannerText != null && bannerText.trim().length() != 0) {
                    return bannerText;
                }
            }
        }
        return "    ____                    __          _       __              __  \n" +
                "   / __ \\ ___   ____ _ ____/ /__  __   | |     / /____   _____ / /__\n" +
                "  / /_/ // _ \\ / __ `// __  // / / /   | | /| / // __ \\ / ___// //_/\n" +
                " / _, _//  __// /_/ // /_/ // /_/ /  _ | |/ |/ // /_/ // /   / ,<   \n" +
                "/_/ |_| \\___/ \\__,_/ \\__,_/ \\__, /  (_)|__/|__/ \\____//_/   /_/|_|  \n" +
                "                           /____/                    ("+ Version.CURRENT +")";
    }

    private static String getRootPath() {
        try {
            String path = Ready.getClassLoader().getResource("").toURI().getPath();
            return new File(path).getAbsolutePath();
        } catch (Exception e) {
            try {
                String path = Banner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                path = java.net.URLDecoder.decode(path, "UTF-8");
                if (path.endsWith(File.separator)) {
                    path = path.substring(0, path.length() - 1);
                }

                if (path.endsWith(".jar")) {
                    path = path.substring(0, path.lastIndexOf("/") + 1);
                }
                return path;
            } catch (UnsupportedEncodingException e1) {
                return null;
            }
        }
    }

    private static String readString(File file) {
        ByteArrayOutputStream baos = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int len = 0; (len = fis.read(buffer)) > 0; ) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) {
        } finally {
            close(fis, baos);
        }
        return null;
    }

    private static void close(InputStream is, OutputStream os) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

}
