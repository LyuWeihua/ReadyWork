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

package work.ready.core.security;

import io.undertow.util.StatusCodes;
import org.xnio.Bits;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

class AccessControl {
    private static final Log logger = LogFactory.getLog(AccessControl.class);

    private static final Pattern IP4_EXACT = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");

    private static final Pattern IP4_WILDCARD = Pattern.compile("(?:(?:\\d{1,3}|\\*)\\.){3}(?:\\d{1,3}|\\*)");

    private static final Pattern IP4_SLASH = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}\\/\\d\\d?");

    private static final Pattern IP6_EXACT = Pattern.compile("(?:[a-zA-Z0-9]{1,4}:){7}[a-zA-Z0-9]{1,4}");

    private static final Pattern IP6_WILDCARD = Pattern.compile("(?:(?:[a-zA-Z0-9]{1,4}|\\*):){7}(?:[a-zA-Z0-9]{1,4}|\\*)");

    private static final Pattern IP6_SLASH = Pattern.compile("(?:[a-zA-Z0-9]{1,4}:){7}[a-zA-Z0-9]{1,4}\\/\\d{1,3}");

    private SecurityManager securityManager;
    private int denyResponseCode = StatusCodes.FORBIDDEN;
    private LimiterConfig config;
    private List<PeerMatch> ipv6AllowAcl = new CopyOnWriteArrayList<>();
    private List<PeerMatch> ipv4AllowAcl = new CopyOnWriteArrayList<>();
    private List<PeerMatch> ipv6DenyAcl = new CopyOnWriteArrayList<>();
    private List<PeerMatch> ipv4DenyAcl = new CopyOnWriteArrayList<>();
    private boolean haveIpRules = false;
    private List<HeadAclMatch> headAllowAcl = new CopyOnWriteArrayList<>();
    private List<HeadAclMatch> headDenyAcl = new CopyOnWriteArrayList<>();
    private boolean haveHeadRules = false;

    boolean haveRules(){
        return haveIpRules || haveHeadRules;
    }

    boolean haveIpRules(){
        return haveIpRules;
    }

    boolean haveHeadRules(){
        return haveHeadRules;
    }

    AccessControl(SecurityManager securityManager) {
        this.securityManager = securityManager;
        this.config = securityManager.getConfig().getLimiter();
    }

    boolean validateIp(InetAddress address) {
        return isIpAllowed(address);
    }

    boolean validateIp(String clientIP) {
        try {
            InetAddress address = InetAddress.getByName(clientIP);
            if (config.isEnableSkipLocalIp() && isLocal(address)) {
                logger.debug("allow local client address");
                return true;
            }
            return validateIp(address);
        } catch (UnknownHostException e) {
            throw new Error(e);  
        }
    }

    boolean validateHead(Map<String, String> headMap) {
        return isHeadAllowed(headMap);
    }

    public static boolean isLocal(InetAddress address) {

        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    private boolean isIpAllowed(InetAddress address) {
        if (address instanceof Inet4Address) {
            if(ipv4AllowAcl.size() > 0) {   
                for (PeerMatch rule : ipv4AllowAcl) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Comparing rule [%s] to IPv4 address %s.", rule.toPredicateString(), address.getHostAddress());
                    }
                    if (rule.matches(address)) {
                        return true;
                    }
                }
            }
            if(ipv4DenyAcl.size() > 0) {    
                for (PeerMatch rule : ipv4DenyAcl) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Comparing rule [%s] to IPv4 address %s.", rule.toPredicateString(), address.getHostAddress());
                    }
                    if (rule.matches(address)) {
                        return false;
                    }
                }
                return true; 
            } else {
                if(ipv4AllowAcl.size() > 0 || ipv6AllowAcl.size() > 0)
                return false; 
            }
        } else if (address instanceof Inet6Address) {
            if(ipv6AllowAcl.size() > 0) {   
                for (PeerMatch rule : ipv6AllowAcl) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Comparing rule [%s] to IPv6 address %s.", rule.toPredicateString(), address.getHostAddress());
                    }
                    if (rule.matches(address)) {
                        return true;
                    }
                }
            }
            if(ipv6DenyAcl.size() > 0) {    
                for (PeerMatch rule : ipv6DenyAcl) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Comparing rule [%s] to IPv6 address %s.", rule.toPredicateString(), address.getHostAddress());
                    }
                    if (rule.matches(address)) {
                        return false;
                    }
                }
                return true; 
            } else {
                if(ipv4AllowAcl.size() > 0 || ipv6AllowAcl.size() > 0)
                return false; 
            }
        }
        return true;    
    }

    private boolean isHeadAllowed(Map<String, String> headMap) {
        if(headAllowAcl.size() > 0) {   
            for (HeadAclMatch rule : headAllowAcl) {
                String value = headMap.get(rule.headName);
                if (value == null) continue;
                if (logger.isTraceEnabled()) {
                    logger.trace("Comparing rule [%s] to Head %s, value=%s.", rule.toString(), rule.headName, value);
                }
                if (rule.matches(value)) {
                    return true;
                }
            }
        }
        if(headDenyAcl.size() > 0) {    
            for (HeadAclMatch rule : headDenyAcl) {
                String value = headMap.get(rule.headName);
                if (value == null) continue;
                if (logger.isTraceEnabled()) {
                    logger.trace("Comparing rule [%s] to Head %s, value=%s.", rule.toString(), rule.headName, value);
                }
                if (rule.matches(value)) {
                    return false;
                }
            }
            return true; 
        } else {
            if(headAllowAcl.size() > 0)
            return false; 
        }
        return true;    
    }

    int getDenyResponseCode() {
        return denyResponseCode;
    }

    AccessControl setDenyResponseCode(int denyResponseCode) {
        this.denyResponseCode = denyResponseCode;
        return this;
    }

    AccessControl addAllowIp(final String peer) {
        return addIpRule(peer, false);
    }

    AccessControl removeAllowIp(final String peer) {
        return removeIpRule(peer, false);
    }

    AccessControl addDenyIp(final String peer) {
        return addIpRule(peer, true);
    }

    AccessControl removeDenyIp(final String peer) {
        return removeIpRule(peer, true);
    }

    public AccessControl clearRules() {
        return clearIpRules().clearHeadRules();
    }

    AccessControl clearDenyRules() {
        return clearIpDenyRules().clearHeadDenyRules();
    }

    synchronized AccessControl clearIpDenyRules() {
        this.ipv6DenyAcl.clear();
        this.ipv4DenyAcl.clear();
        haveIpRules = ipv6AllowAcl.size() > 0 || ipv4AllowAcl.size() > 0;
        return this;
    }

    synchronized AccessControl clearHeadDenyRules() {
        this.headDenyAcl.clear();
        haveHeadRules = headAllowAcl.size() > 0;
        return this;
    }

    AccessControl clearAllowRules() {
        return clearIpAllowRules().clearHeadAllowRules();
    }

    synchronized AccessControl clearIpAllowRules() {
        this.ipv6AllowAcl.clear();
        this.ipv4AllowAcl.clear();
        haveIpRules = ipv6DenyAcl.size() > 0 || ipv4DenyAcl.size() > 0;
        return this;
    }

    synchronized AccessControl clearHeadAllowRules() {
        this.headAllowAcl.clear();
        haveHeadRules = headDenyAcl.size() > 0;
        return this;
    }

    synchronized AccessControl clearIpRules() {
        this.ipv6AllowAcl.clear();
        this.ipv4AllowAcl.clear();
        this.ipv6DenyAcl.clear();
        this.ipv4DenyAcl.clear();
        haveIpRules = false;
        return this;
    }

    synchronized AccessControl clearHeadRules() {
        this.headAllowAcl.clear();
        this.headDenyAcl.clear();
        haveHeadRules = false;
        return this;
    }
    
    public AccessControl addAllowHead(final String headName, final String match) {
        return addHeadRule(headName, match, false);
    }

    public AccessControl removeAllowHead(final String headName) {
        return removeHeadRule(headName, false);
    }

    public AccessControl addDenyHead(final String headName, final String match) {
        return addHeadRule(headName, match, true);
    }

    public AccessControl removeDenyHead(final String headName) {
        return removeHeadRule(headName, true);
    }

    private synchronized AccessControl addHeadRule(final String headName, final String match, final boolean deny) {
        if(deny) {
            this.headDenyAcl.add(new HeadAclMatch(deny, headName, match));
        } else {
            this.headAllowAcl.add(new HeadAclMatch(deny, headName, match));
        }
        haveHeadRules = true;
        return this;
    }

    private synchronized AccessControl removeHeadRule(final String headName, final boolean deny) {
        if(deny){
            headDenyAcl.removeIf(match -> match.headName.equals(headName));
            haveHeadRules = headDenyAcl.size() > 0;
        } else {
            headAllowAcl.removeIf(match -> match.headName.equals(headName));
            haveHeadRules = headAllowAcl.size() > 0;
        }
        return this;
    }

    private synchronized AccessControl removeIpRule(final String peer, final boolean deny) {
        if(deny){
            ipv4DenyAcl.removeIf(peerMatch -> peerMatch.pattern.equals(peer));
            ipv6DenyAcl.removeIf(peerMatch -> peerMatch.pattern.equals(peer));
            haveIpRules = ipv4DenyAcl.size() > 0 || ipv6DenyAcl.size() > 0;
        } else {
            ipv4AllowAcl.removeIf(peerMatch -> peerMatch.pattern.equals(peer));
            ipv6AllowAcl.removeIf(peerMatch -> peerMatch.pattern.equals(peer));
            haveIpRules = ipv4AllowAcl.size() > 0 || ipv6AllowAcl.size() > 0;
        }
        return this;
    }

    private synchronized AccessControl addIpRule(final String peer, final boolean deny) {
        if (IP4_EXACT.matcher(peer).matches()) {
            addIpV4ExactMatch(peer, deny);
        } else if (IP4_WILDCARD.matcher(peer).matches()) {
            addIpV4WildcardMatch(peer, deny);
        } else if (IP4_SLASH.matcher(peer).matches()) {
            addIpV4SlashPrefix(peer, deny);
        } else if (IP6_EXACT.matcher(peer).matches()) {
            addIpV6ExactMatch(peer, deny);
        } else if (IP6_WILDCARD.matcher(peer).matches()) {
            addIpV6WildcardMatch(peer, deny);
        } else if (IP6_SLASH.matcher(peer).matches()) {
            addIpV6SlashPrefix(peer, deny);
        } else {
            throw new RuntimeException("Not a valid IP pattern " + peer);
        }
        haveIpRules = true;
        return this;
    }

    private void addIpV6SlashPrefix(final String peer, final boolean deny) {
        String[] components = peer.split("\\/");
        String[] parts = components[0].split("\\:");
        int maskLen = Integer.parseInt(components[1]);
        assert parts.length == 8;

        byte[] pattern = new byte[16];
        byte[] mask = new byte[16];

        for (int i = 0; i < 8; ++i) {
            int val = Integer.parseInt(parts[i], 16);
            pattern[i * 2] = (byte) (val >> 8);
            pattern[i * 2 + 1] = (byte) (val & 0xFF);
        }
        for (int i = 0; i < 16; ++i) {
            if (maskLen > 8) {
                mask[i] = (byte) (0xFF);
                maskLen -= 8;
            } else if (maskLen != 0) {
                mask[i] = (byte) (Bits.intBitMask(8 - maskLen, 7) & 0xFF);
                maskLen = 0;
            } else {
                break;
            }
        }
        if(deny) {
            ipv6DenyAcl.add(new PrefixIpV6PeerMatch(deny, peer, mask, pattern));
        } else {
            ipv6AllowAcl.add(new PrefixIpV6PeerMatch(deny, peer, mask, pattern));
        }
    }

    private void addIpV4SlashPrefix(final String peer, final boolean deny) {
        String[] components = peer.split("\\/");
        String[] parts = components[0].split("\\.");
        int maskLen = Integer.parseInt(components[1]);
        final int mask = Bits.intBitMask(32 - maskLen, 31);
        int prefix = 0;
        for (int i = 0; i < 4; ++i) {
            prefix <<= 8;
            String part = parts[i];
            int no = Integer.parseInt(part);
            prefix |= no;
        }
        if(deny) {
            ipv4DenyAcl.add(new PrefixIpV4PeerMatch(deny, peer, mask, prefix));
        } else {
            ipv4AllowAcl.add(new PrefixIpV4PeerMatch(deny, peer, mask, prefix));
        }
    }

    private void addIpV6WildcardMatch(final String peer, final boolean deny) {
        byte[] pattern = new byte[16];
        byte[] mask = new byte[16];
        String[] parts = peer.split("\\:");
        assert parts.length == 8;
        for (int i = 0; i < 8; ++i) {
            if (!parts[i].equals("*")) {
                int val = Integer.parseInt(parts[i], 16);
                pattern[i * 2] = (byte) (val >> 8);
                pattern[i * 2 + 1] = (byte) (val & 0xFF);
                mask[i * 2] = (byte) (0xFF);
                mask[i * 2 + 1] = (byte) (0xFF);
            }
        }
        if(deny) {
            ipv6DenyAcl.add(new PrefixIpV6PeerMatch(deny, peer, mask, pattern));
        } else {
            ipv6AllowAcl.add(new PrefixIpV6PeerMatch(deny, peer, mask, pattern));
        }
    }

    private void addIpV4WildcardMatch(final String peer, final boolean deny) {
        String[] parts = peer.split("\\.");
        int mask = 0;
        int prefix = 0;
        for (int i = 0; i < 4; ++i) {
            mask <<= 8;
            prefix <<= 8;
            String part = parts[i];
            if (!part.equals("*")) {
                int no = Integer.parseInt(part);
                mask |= 0xFF;
                prefix |= no;
            }
        }
        if(deny) {
            ipv4DenyAcl.add(new PrefixIpV4PeerMatch(deny, peer, mask, prefix));
        } else {
            ipv4AllowAcl.add(new PrefixIpV4PeerMatch(deny, peer, mask, prefix));
        }
    }

    private void addIpV6ExactMatch(final String peer, final boolean deny) {
        byte[] bytes = new byte[16];
        String[] parts = peer.split("\\:");
        assert parts.length == 8;
        for (int i = 0; i < 8; ++i) {
            int val = Integer.parseInt(parts[i], 16);
            bytes[i * 2] = (byte) (val >> 8);
            bytes[i * 2 + 1] = (byte) (val & 0xFF);
        }
        if(deny) {
            ipv6DenyAcl.add(new ExactIpV6PeerMatch(deny, peer, bytes));
        } else {
            ipv6AllowAcl.add(new ExactIpV6PeerMatch(deny, peer, bytes));
        }
    }

    private void addIpV4ExactMatch(final String peer, final boolean deny) {
        String[] parts = peer.split("\\.");
        byte[] bytes = {(byte) Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]), (byte) Integer.parseInt(parts[2]), (byte) Integer.parseInt(parts[3])};
        if(deny) {
            ipv4DenyAcl.add(new ExactIpV4PeerMatch(deny, peer, bytes));
        } else {
            ipv4AllowAcl.add(new ExactIpV4PeerMatch(deny, peer, bytes));
        }
    }

    @Override
    public String toString() {
        
        String predicate = "ip-access-control( acl={ ";
        List<PeerMatch> acl = new ArrayList<>();
        acl.addAll(ipv4AllowAcl);
        acl.addAll(ipv6AllowAcl);
        acl.addAll(ipv4DenyAcl);
        acl.addAll(ipv6DenyAcl);
        predicate += acl.stream().map(s -> "'" + s.toPredicateString() + "'").collect(Collectors.joining(", "));
        predicate += " }";
        if (denyResponseCode != StatusCodes.FORBIDDEN) {
            predicate += ", failure-status=" + denyResponseCode;
        }
        predicate += " )";
        return predicate;
    }

    abstract static class PeerMatch {

        private final boolean deny;
        private final String pattern;

        protected PeerMatch(final boolean deny, final String pattern) {
            this.deny = deny;
            this.pattern = pattern;
        }

        abstract boolean matches(final InetAddress address);

        boolean isDeny() {
            return deny;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{"
                    + "deny=" + deny
                    + ", pattern='" + pattern + '\''
                    + '}';
        }

        public String toPredicateString() {
            return pattern + " " + (deny ? "deny" : "allow");
        }
    }

    static class ExactIpV4PeerMatch extends PeerMatch {

        private final byte[] address;

        protected ExactIpV4PeerMatch(final boolean deny, final String pattern, final byte[] address) {
            super(deny, pattern);
            this.address = address;
        }

        @Override
        boolean matches(final InetAddress address) {
            return Arrays.equals(address.getAddress(), this.address);
        }
    }

    static class ExactIpV6PeerMatch extends PeerMatch {

        private final byte[] address;

        protected ExactIpV6PeerMatch(final boolean deny, final String pattern, final byte[] address) {
            super(deny, pattern);
            this.address = address;
        }

        @Override
        boolean matches(final InetAddress address) {
            return Arrays.equals(address.getAddress(), this.address);
        }
    }

    private static class PrefixIpV4PeerMatch extends PeerMatch {

        private final int mask;
        private final int prefix;

        protected PrefixIpV4PeerMatch(final boolean deny, final String pattern, final int mask, final int prefix) {
            super(deny, pattern);
            this.mask = mask;
            this.prefix = prefix;
        }

        @Override
        boolean matches(final InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes == null) {
                return false;
            }
            int addressInt = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
            return (addressInt & mask) == prefix;
        }
    }

    static class PrefixIpV6PeerMatch extends PeerMatch {

        private final byte[] mask;
        private final byte[] prefix;

        protected PrefixIpV6PeerMatch(final boolean deny, final String pattern, final byte[] mask, final byte[] prefix) {
            super(deny, pattern);
            this.mask = mask;
            this.prefix = prefix;
            assert mask.length == prefix.length;
        }

        @Override
        boolean matches(final InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes == null) {
                return false;
            }
            if (bytes.length != mask.length) {
                return false;
            }
            for (int i = 0; i < mask.length; ++i) {
                if ((bytes[i] & mask[i]) != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    static class HeadAclMatch {

        private final boolean deny;
        private final String headName;
        private final Pattern pattern;

        protected HeadAclMatch(final boolean deny, final String headName, final String pattern) {
            this.deny = deny;
            this.headName = headName;
            this.pattern = createPattern(pattern);
        }

        private Pattern createPattern(final String pattern) {
            try {
                return Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new RuntimeException("Not a valid regular expression pattern %s" + pattern);
            }
        }

        boolean matches(final String attribute) {
            return pattern.matcher(attribute).matches();
        }

        boolean isDeny() {
            return deny;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + "{"
                    + "deny=" + deny
                    + ", headName=" + headName
                    + ", pattern='" + pattern + '\''
                    + '}';
        }
    }

}
