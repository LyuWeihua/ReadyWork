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

package work.ready.core.module;

import java.io.Serializable;
import java.security.InvalidParameterException;

public class Version implements Comparable<Version>, Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected byte majorVersion;
    protected byte minorVersion;
    protected byte revision;
    protected String buildDate;
    protected String apiVersion;
    protected String groupId;
    protected String artifactId;
    protected String author;
    protected String description;

    public static final Version V_0_6_6 = new Version(0, 6, 6, "210501", "v0.6", "work.ready", "ready-work-core", "WeiHua Lyu", "ready work framework, http://ready.work");
    public static final Version CURRENT = V_0_6_6;

    public static Version unknown() {
        return new Version(0, null, null, null, null, null, null);
    }

    public Version(int id) {
        this(id, null, null, null, null, null, null);
    }

    public Version(int id, String buildDate, String apiVersion, String groupId, String artifactId, String author, String description) {
        this.majorVersion = (byte) ((id / 1000000) % 100);
        this.minorVersion = (byte) ((id / 10000) % 100);
        this.revision = (byte) ((id / 100) % 100);
        this.id = majorVersion * 1000000 + minorVersion * 10000 + revision * 100;
    }

    public Version(int major, int minor, int revision, String buildDate, String apiVersion, String groupId, String artifactId, String author, String description ) {
        this(Integer.valueOf(major).byteValue(), Integer.valueOf(minor).byteValue(), Integer.valueOf(revision).byteValue(), buildDate, apiVersion, groupId, artifactId, author, description);
        if (major > Byte.MAX_VALUE || minor > Byte.MAX_VALUE || revision > Byte.MAX_VALUE) {
            throw new InvalidParameterException("Invalid version initializers [" + major + ", " + minor + ", " + revision + "]");
        }
    }

    public Version(byte major, byte minor, byte revision) {
        this(major, minor, revision, null, null, null, null, null, null);
    }

    public Version(byte major, byte minor, byte revision, String buildDate, String apiVersion, String groupId, String artifactId, String author, String description) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.revision = revision;
        this.id = major * 1000000 + minor * 10000 + revision * 100;
        this.buildDate = buildDate;
        this.apiVersion = apiVersion;
        this.groupId = groupId == null ? "" : groupId;
        this.artifactId = artifactId == null ? "" : artifactId;
        this.author = author == null ? "" : author;
        this.description = description == null ? "" : description;
    }

    public byte getMajorVersion() {
        return this.majorVersion;
    }

    public byte getMinorVersion() {
        return this.minorVersion;
    }

    public byte getRevision() {
        return this.revision;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getBuildDate() { return this.buildDate; }

    public String getApiVersion() { return this.apiVersion; }

    public String getAuthor() { return this.author; }

    public String getDescription() { return this.description; }

    protected Version(byte... parts) {
        assert parts.length >= 3 : "Version must be initialized with all Major.Minor.Revision.build components";
        this.majorVersion = parts[0];
        this.minorVersion = parts[1];
        this.revision = parts[2];

        if ((majorVersion | minorVersion | revision) < 0) {
            throw new InvalidParameterException("Invalid version initialisers [" + majorVersion + ", " + minorVersion + ", " + revision + "]");
        }

        this.id = majorVersion * 1000000 + minorVersion * 10000 + revision * 100;
    }

    public static Version fromString(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        return new Version(from(version));
    }

    protected static byte[] from(String ver) {
        String[] parts = ver.split("[.-]");
        
        if (parts.length >= 3 && parts.length <= 5) {
            try {
                return new byte[] { Byte.parseByte(parts[0]), Byte.parseByte(parts[1]), Byte.parseByte(parts[2]) };
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid version format [" + ver + "]: " + nfe.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Invalid version format [" + ver + "]");
        }
    }

    private static String toString(Object... parts) {
        assert parts.length >= 1 : "Version must contain at least a Major component";
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 1; i < parts.length; i ++) {
            sb.append(".").append(parts[i]);
        }
        return sb.toString();
    }

    public String toSimpleString() {
        return this.groupId + '/' + this.artifactId + '/' + this.toString();
    }

    public String toFullString() {
        return "{\"groupId\":" + groupId
                + ",\"artifactId\":\"" + artifactId
                + "\",\"version\":\"" + this.toString()
                + "\",\"apiVersion\":\"" + apiVersion
                + "\",\"author\":\"" + author
                + "\",\"description\":\"" + description + "\"}";
    }

    @Override
    public String toString() {
        return toString(majorVersion, minorVersion, revision, buildDate);
    }

    public boolean after(Version version) {
        return version.id < id;
    }

    public boolean onOrAfter(Version version) {
        return version.id <= id;
    }

    public boolean before(Version version) {
        return version.id > id;
    }

    public boolean onOrBefore(Version version) {
        return version.id >= id;
    }

    @Override
    public int hashCode() {
        return this.artifactId.hashCode() ^ this.groupId.hashCode() + this.majorVersion - this.minorVersion + this.revision;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o.getClass() != this.getClass()) {
            return false;
        } else {
            Version other = (Version)o;
            return other.majorVersion == this.majorVersion && other.minorVersion == this.minorVersion && other.revision == this.revision && other.artifactId.equals(this.artifactId) && other.groupId.equals(this.groupId);
        }
    }

    @Override
    public int compareTo(Version other) {
        if (other == this) {
            return 0;
        } else {
            int diff = this.groupId.compareTo(other.groupId);
            if (diff == 0) {
                diff = this.artifactId.compareTo(other.artifactId);
                if (diff == 0) {
                    diff = this.majorVersion - other.majorVersion;
                    if (diff == 0) {
                        diff = this.minorVersion - other.minorVersion;
                        if (diff == 0) {
                            diff = this.revision - other.revision;
                        }
                    }
                }
            }

            return diff;
        }
    }
}
