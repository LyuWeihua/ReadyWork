/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package work.ready.cloud.jdbc.common.xcontent;

import java.util.function.Supplier;

public interface DeprecationHandler {
    
    DeprecationHandler THROW_UNSUPPORTED_OPERATION = new DeprecationHandler() {
        @Override
        public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName, String replacedWith) {
            if (parserName != null) {
                throw new UnsupportedOperationException("deprecated fields not supported in [" + parserName + "] but got ["
                    + usedName + "] at [" + location.get() + "] which is a deprecated name for [" + replacedWith + "]");
            } else {
                throw new UnsupportedOperationException("deprecated fields not supported here but got ["
                    + usedName + "] which is a deprecated name for [" + replacedWith + "]");
            }
        }
        @Override
        public void usedDeprecatedName(String parserName, Supplier<XContentLocation> location, String usedName, String modernName) {
            if (parserName != null) {
                throw new UnsupportedOperationException("deprecated fields not supported in [" + parserName + "] but got ["
                    + usedName + "] at [" + location.get() + "] which has been replaced with [" + modernName + "]");
            } else {
                throw new UnsupportedOperationException("deprecated fields not supported here but got ["
                    + usedName + "] which has been replaced with [" + modernName + "]");
            }
        }

        @Override
        public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName) {
            if (parserName != null) {
                throw new UnsupportedOperationException("deprecated fields not supported in [" + parserName + "] but got ["
                    + usedName + "] at [" + location.get() + "] which has been deprecated entirely");
            } else {
                throw new UnsupportedOperationException("deprecated fields not supported here but got ["
                    + usedName + "] which has been deprecated entirely");
            }
        }
    };

    DeprecationHandler IGNORE_DEPRECATIONS = new DeprecationHandler() {
        @Override
        public void usedDeprecatedName(String parserName, Supplier<XContentLocation> location, String usedName, String modernName) {

        }

        @Override
        public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName, String replacedWith) {

        }

        @Override
        public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName) {

        }
    };

    void usedDeprecatedName(String parserName, Supplier<XContentLocation> location, String usedName, String modernName);

    void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName, String replacedWith);

    void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName);

}
