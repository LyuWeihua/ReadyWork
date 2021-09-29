/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.client;

import work.ready.cloud.jdbc.olap.proto.SqlVersion;
import work.ready.core.tools.define.SuppressForbidden;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class ClientVersion {

    public static final SqlVersion CURRENT;
    static boolean readVersionFromJar = false;

    static {
        
        if(!readVersionFromJar) {
            CURRENT = new SqlVersion(8, 0, 0, null, null);
        } else {
            
            String target = ClientVersion.class.getName().replace(".", "/").concat(".class");
            Enumeration<URL> res;

            try {
                res = ClientVersion.class.getClassLoader().getResources(target);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Cannot detect Ready JDBC jar; it typically indicates a deployment issue...");
            }

            if (res != null) {
                List<URL> urls = Collections.list(res);
                Set<String> normalized = new LinkedHashSet<>();

                for (URL url : urls) {
                    normalized.add(StrUtil.pathNormalize(url.toString()));
                }

                int foundJars = 0;
                if (normalized.size() > 1) {
                    StringBuilder sb = new StringBuilder(
                            "Multiple Ready JDBC versions detected in the classpath; please use only one\n");
                    for (String s : normalized) {
                        if (s.contains("jar:")) {
                            foundJars++;
                            sb.append(s.replace("!/" + target, ""));
                            sb.append("\n");
                        }
                    }
                    if (foundJars > 1) {
                        throw new IllegalArgumentException(sb.toString());
                    }
                }
            }

            URL url = SqlVersion.class.getProtectionDomain().getCodeSource().getLocation();
            CURRENT = extractVersion(url);
        }
    }

    @SuppressForbidden(reason="java.util.jar.JarFile must be explicitly closed on Windows")
    static Manifest getManifest(URL url) throws IOException {
        String urlStr = url.toString();
        if (urlStr.endsWith(".jar") || urlStr.endsWith(".jar!/")) {
            URLConnection conn = url.openConnection();
            
            conn.setUseCaches(false);

            if (url.getProtocol().equals("jar")) {
                JarURLConnection jarConn = (JarURLConnection) conn;
                if (jarConn.getEntryName() == null) { 
                    try (JarFile jar = jarConn.getJarFile()) { 
                        return jar.getManifest(); 
                    }
                }
            }
            try (JarInputStream jar = new JarInputStream(conn.getInputStream())) {
                return jar.getManifest();
            }
        }
        return null;
    }

    static SqlVersion extractVersion(URL url) {
        Manifest manifest = null;
        try {
            manifest = getManifest(url);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Detected an Ready JDBC jar but cannot retrieve its version", ex);
        }
        String version = manifest != null ? manifest.getMainAttributes().getValue("X-Compile-Ready-Jdbc-Version") : null;
        return version != null ? SqlVersion.fromString(version) : new SqlVersion(0, 0, 0, null, null);
    }

    public static boolean isServerCompatible(SqlVersion server) {
        
        return SqlVersion.hasVersionCompatibility(server);
    }

    public static int jdbcMajorVersion() {
        return 4;
    }

    public static int jdbcMinorVersion() {
        return 2;
    }

    public static int jdbcRevision() {
        return 0;
    }
}
