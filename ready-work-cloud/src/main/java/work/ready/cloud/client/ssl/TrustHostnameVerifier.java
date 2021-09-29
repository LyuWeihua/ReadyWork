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

package work.ready.cloud.client.ssl;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.NetUtil;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class TrustHostnameVerifier implements HostnameVerifier {
    private static final Log logger = LogFactory.getLog(TrustHostnameVerifier.class);

    private final PublicSuffixMatcher publicSuffixMatcher;

    enum HostNameType {

        IPv4(7), IPv6(7), DNS(2);

        final int subjectType;

        HostNameType(final int subjectType) {
            this.subjectType = subjectType;
        }

    }

    public TrustHostnameVerifier(final PublicSuffixMatcher publicSuffixMatcher) {
        this.publicSuffixMatcher = publicSuffixMatcher;
    }

    public TrustHostnameVerifier() {
        this(null);
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        try {
            final Certificate[] certs = session.getPeerCertificates();
            final X509Certificate x509 = (X509Certificate) certs[0];
            verify(hostname, x509);
            return true;
        } catch (final SSLException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex, "SSL Exception");
            }
            return false;
        }
    }

    public void verify(
            final String host, final X509Certificate cert) throws SSLException {
        final HostNameType hostType = determineHostFormat(host);
        final List<SubjectName> subjectAlts = getSubjectAltNames(cert);
        if (subjectAlts != null && !subjectAlts.isEmpty()) {
            switch (hostType) {
                case IPv4:
                    matchIPAddress(host, subjectAlts);
                    break;
                case IPv6:
                    matchIPv6Address(host, subjectAlts);
                    break;
                default:
                    matchDNSName(host, subjectAlts, this.publicSuffixMatcher);
            }
        } else {

            final X500Principal subjectPrincipal = cert.getSubjectX500Principal();
            final List<String> cn = extractCN(subjectPrincipal.getName(X500Principal.RFC2253));
            if (cn == null || cn.isEmpty()) {
                throw new SSLException("Certificate subject for <" + host + "> doesn't contain " +
                        "a common name and does not have alternative names");
            }
            matchCN(host, cn, this.publicSuffixMatcher);
        }
    }

    static void matchIPAddress(final String host, final List<SubjectName> subjectAlts) throws SSLException {
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.IP) {
                if (host.equals(subjectAlt.getValue())) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " +
                "of the subject alternative names: " + subjectAlts);
    }

    static void matchIPv6Address(final String host, final List<SubjectName> subjectAlts) throws SSLException {
        final String normalisedHost = normaliseAddress(host);
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.IP) {
                final String normalizedSubjectAlt = normaliseAddress(subjectAlt.getValue());
                if (normalisedHost.equals(normalizedSubjectAlt)) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " +
                "of the subject alternative names: " + subjectAlts);
    }

    static void matchDNSName(final String host, final List<SubjectName> subjectAlts,
                             final PublicSuffixMatcher publicSuffixMatcher) throws SSLException {
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (int i = 0; i < subjectAlts.size(); i++) {
            final SubjectName subjectAlt = subjectAlts.get(i);
            if (subjectAlt.getType() == SubjectName.DNS) {
                final String normalizedSubjectAlt = subjectAlt.getValue().toLowerCase(Locale.ROOT);
                if (matchIdentityStrict(normalizedHost, normalizedSubjectAlt, publicSuffixMatcher)) {
                    return;
                }
            }
        }
        throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " +
                "of the subject alternative names: " + subjectAlts);
    }

    static void matchCN(final String host, final List<String> cnList,
                        final PublicSuffixMatcher publicSuffixMatcher) throws SSLException {
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        boolean matched = false;
        for(String cn : cnList) {
            final String normalizedCn = cn.toLowerCase(Locale.ROOT);
            if(matchIdentityStrict(normalizedHost, normalizedCn, publicSuffixMatcher)){
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match " +
                    "common name of the certificate subject: " + cnList);
        }
    }

    static boolean matchDomainRoot(final String host, final String domainRoot) {
        if (domainRoot == null) {
            return false;
        }
        return host.endsWith(domainRoot) && (host.length() == domainRoot.length()
                || host.charAt(host.length() - domainRoot.length() - 1) == '.');
    }

    private static boolean matchIdentity(final String host, final String identity,
                                         final PublicSuffixMatcher publicSuffixMatcher,
                                         final boolean strict) {
        if (publicSuffixMatcher != null && host.contains(".")) {
            if (!matchDomainRoot(host, publicSuffixMatcher.getDomainRoot(identity, DomainType.ICANN))) {
                return false;
            }
        }

        final int asteriskIdx = identity.indexOf('*');
        if (asteriskIdx != -1) {
            final String prefix = identity.substring(0, asteriskIdx);
            final String suffix = identity.substring(asteriskIdx + 1);
            if (!prefix.isEmpty() && !host.startsWith(prefix)) {
                return false;
            }
            if (!suffix.isEmpty() && !host.endsWith(suffix)) {
                return false;
            }
            
            if (strict) {
                final String remainder = host.substring(
                        prefix.length(), host.length() - suffix.length());
                if (remainder.contains(".")) {
                    return false;
                }
            }
            return true;
        }
        return host.equalsIgnoreCase(identity);
    }

    static boolean matchIdentity(final String host, final String identity,
                                 final PublicSuffixMatcher publicSuffixMatcher) {
        return matchIdentity(host, identity, publicSuffixMatcher, false);
    }

    static boolean matchIdentity(final String host, final String identity) {
        return matchIdentity(host, identity, null, false);
    }

    static boolean matchIdentityStrict(final String host, final String identity,
                                       final PublicSuffixMatcher publicSuffixMatcher) {
        return matchIdentity(host, identity, publicSuffixMatcher, true);
    }

    static boolean matchIdentityStrict(final String host, final String identity) {
        return matchIdentity(host, identity, null, true);
    }

    static List<String> extractCN(final String subjectPrincipal) throws SSLException {
        List<String> cnList = new ArrayList<>();
        if (subjectPrincipal == null) {
            return cnList;
        }
        try {
            final LdapName subjectDN = new LdapName(subjectPrincipal);
            final List<Rdn> rdns = subjectDN.getRdns();
            for (int i = rdns.size() - 1; i >= 0; i--) {
                final Rdn rds = rdns.get(i);
                final Attributes attributes = rds.toAttributes();
                final Attribute cn = attributes.get("cn");
                if (cn != null) {
                    try {
                        final Object value = cn.get();
                        if (value != null) {
                            cnList.add(value.toString());
                        }
                    } catch (final NoSuchElementException ignore) {
                        
                    } catch (final NamingException ignore) {
                        
                    }
                }
            }
            return cnList;
        } catch (final InvalidNameException e) {
            throw new SSLException(subjectPrincipal + " is not a valid X500 distinguished name");
        }
    }

    static HostNameType determineHostFormat(final String host) {
        if (NetUtil.isIPv4Address(host)) {
            return HostNameType.IPv4;
        }
        String s = host;
        if (s.startsWith("[") && s.endsWith("]")) {
            s = host.substring(1, host.length() - 1);
        }
        if (NetUtil.isIPv6Address(s)) {
            return HostNameType.IPv6;
        }
        return HostNameType.DNS;
    }

    static List<SubjectName> getSubjectAltNames(final X509Certificate cert) {
        try {
            final Collection<List<?>> entries = cert.getSubjectAlternativeNames();
            if (entries == null) {
                return Collections.emptyList();
            }
            final List<SubjectName> result = new ArrayList<SubjectName>();
            for (final List<?> entry : entries) {
                final Integer type = entry.size() >= 2 ? (Integer) entry.get(0) : null;
                if (type != null) {
                    if (type == SubjectName.DNS || type == SubjectName.IP) {
                        final Object o = entry.get(1);
                        if (o instanceof String) {
                            result.add(new SubjectName((String) o, type));
                        } else if (o instanceof byte[]) {
                            
                        }
                    }
                }
            }
            return result;
        } catch (final CertificateParsingException ignore) {
            return Collections.emptyList();
        }
    }

    static String normaliseAddress(final String hostname) {
        if (hostname == null) {
            return hostname;
        }
        try {
            final InetAddress inetAddress = InetAddress.getByName(hostname);
            return inetAddress.getHostAddress();
        } catch (final UnknownHostException unexpected) { 
            return hostname;
        }
    }
}
