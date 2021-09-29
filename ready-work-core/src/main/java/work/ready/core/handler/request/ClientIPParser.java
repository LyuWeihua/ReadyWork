/**
 *
 * Original work Copyright core-ng
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
package work.ready.core.handler.request;

import work.ready.core.exception.BadRequestException;
import work.ready.core.tools.StrUtil;

public class ClientIPParser {

    public int maxForwardedIPs = 2;

    public String parse(String remoteAddress, String xForwardedFor) {
        if (StrUtil.isBlank(xForwardedFor)) return remoteAddress;

        int foundForwardedIPs = 1;
        int index = xForwardedFor.length() - 1;
        int start;
        int end = index + 1;    
        while (true) {
            char ch = xForwardedFor.charAt(index);
            if (ch == ',') {
                foundForwardedIPs++;
                if (foundForwardedIPs > maxForwardedIPs) {
                    start = index + 1;
                    break;
                } else {
                    end = index;
                }
            }
            if (index == 0) {
                start = index;
                break;
            }
            index--;
        }

        String clientIP = xForwardedFor.substring(start, end).trim();
        if (!isValidIP(clientIP)) throw new BadRequestException("invalid client ip address");
        return clientIP;
    }

    boolean isValidIP(String clientIP) {
        int dots = 0;
        for (int i = 0; i < clientIP.length(); i++) {
            char ch = clientIP.charAt(i);
            if (ch == '.') {
                dots++;
            } else if (ch == ':') {
                if (dots > 0) return false; 
            } else if (Character.digit(ch, 16) == -1) {
                return false; 
            }
        }

        return dots == 0 || dots == 3;
    }
}
