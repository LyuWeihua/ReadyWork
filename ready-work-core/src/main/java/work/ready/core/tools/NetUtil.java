/**
 *
 * Original work Copyright 2009-2016 Weibo, Inc.
 * Modified Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.tools;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.ip2region.DataBlock;
import work.ready.core.tools.ip2region.DbConfig;
import work.ready.core.tools.ip2region.DbSearcher;
import work.ready.core.tools.validator.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class NetUtil {
    private static final Log logger = LogFactory.getLog(NetUtil.class);

    public static final String LOCALHOST_V4 = "127.0.0.1";
    public static final String LOCALHOST_V6 = "0:0:0:0:0:0:0:1";
    public static final String LOCALHOST_V6_SHORT = "::1";

    public static final String ANYHOST_V4 = "0.0.0.0";
    public static final String ANYHOST_V6 = "0:0:0:0:0:0:0:0";
    public static final String ANYHOST_V6_SHORT = "::";

    private static volatile InetAddress LOCAL_ADDRESS = null;

    private static final Pattern LOOPBACK_IPV4_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    private static final String IPV4_BASIC_PATTERN_STRING =
            "(([1-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){1}" + 
                    "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){2}" + 
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])"; 

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^" + IPV4_BASIC_PATTERN_STRING + "$");

    private static final Pattern IPV4_MAPPED_IPV6_PATTERN = 
            Pattern.compile("^::[fF]{4}:" + IPV4_BASIC_PATTERN_STRING + "$");

    private static final Pattern IPV6_STD_PATTERN =
            Pattern.compile(
                    "^[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN =
            Pattern.compile(
                    "^(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)" + 
                            "::" +
                            "(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)$"); 

    private static byte[] ip2region;

    private static final char COLON_CHAR = ':';

    private static final int MAX_COLON_COUNT = 7;

    public static final boolean SUPPORTS_V6;
    static {
        boolean v = false;
        try {
            for (NetworkInterface nic : getInterfaces()) {
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet6Address) {
                        v = true;
                        break;
                    }
                }
            }
        } catch (SecurityException | SocketException misconfiguration) {
            v = true; 
        }
        SUPPORTS_V6 = v;
    }

    public static InetAddress getLocalAddress() {
        return getLocalAddress(null);
    }

    public static InetAddress getLocalAddress(Map<String, Integer> destHostPorts) {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }

        InetAddress localAddress = getLocalAddressByHostname();
        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressBySocket(destHostPorts);
        }

        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByNetworkInterface();
        }

        try {
            if (localAddress == null) localAddress = InetAddress.getByName(null);
        } catch (UnknownHostException e) {}

        if (isValidAddress(localAddress)) {
            LOCAL_ADDRESS = localAddress;
        }

        return localAddress;
    }

    private static InetAddress getLocalAddressByHostname() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.error("Failed to retrieving local address by hostname: " + e);
        }
        return null;
    }

    private static InetAddress getLocalAddressBySocket(Map<String, Integer> destHostPorts) {
        if (destHostPorts == null || destHostPorts.size() == 0) {
            return null;
        }

        for (Map.Entry<String, Integer> entry : destHostPorts.entrySet()) {
            String host = entry.getKey();
            int port = entry.getValue();
            try {
                Socket socket = new Socket();
                try {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    socket.connect(addr, 1000);
                    return socket.getLocalAddress();
                } finally {
                    try {
                        socket.close();
                    } catch (Throwable e) {}
                }
            } catch (Exception e) {
                logger.error(String.format("Failed to retrieving local address by connecting to dest host:port(%s:%s) false, e=%s", host,
                        port, e));
            }
        }
        return null;
    }

    private static InetAddress getLocalAddressByNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress address = addresses.nextElement();
                            if (isValidAddress(address)) {
                                return address;
                            }
                        } catch (Throwable e) {
                            logger.error("Failed to retrieving ip address, " + e.getMessage());
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Failed to retrieving ip address, " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            logger.error("Failed to retrieving ip address, " + e.getMessage());
        }
        return null;
    }

    public static List<NetworkInterface> getInterfaces() throws SocketException {
        List<NetworkInterface> all = new ArrayList<>();
        addAllInterfaces(all, Collections.list(NetworkInterface.getNetworkInterfaces()));
        Collections.sort(all, new Comparator<NetworkInterface>() {
            @Override
            public int compare(NetworkInterface left, NetworkInterface right) {
                return Integer.compare(left.getIndex(), right.getIndex());
            }
        });
        return all;
    }

    private static void addAllInterfaces(List<NetworkInterface> target, List<NetworkInterface> level) {
        if (!level.isEmpty()) {
            target.addAll(level);
            for (NetworkInterface intf : level) {
                addAllInterfaces(target, Collections.list(intf.getSubInterfaces()));
            }
        }
    }

    private static InetAddress[] filterAllAddresses(final Predicate<InetAddress> predicate, final String message) throws IOException {
        final List<NetworkInterface> interfaces = getInterfaces();
        final List<InetAddress> list = new ArrayList<>();
        for (final NetworkInterface intf : interfaces) {
            for (final InetAddress address : Collections.list(intf.getInetAddresses())) {
                if (predicate.test(address) && isUp(intf)) {
                    list.add(address);
                }
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException(message + ", got " + interfaces);
        }
        return list.toArray(new InetAddress[0]);
    }

    private static boolean isUp(final NetworkInterface intf) throws IOException {
        try {
            return intf.isUp();
        } catch (final SocketException e) {
            
            if (intf.getName().startsWith("veth") && e.getMessage().equals("No such device (getFlags() failed)")) {
                return false;
            }
            throw new IOException("failed to check if interface [" + intf.getName() + "] is up", e);
        }
    }

    public static InetAddress[] getLoopbackAddresses() throws IOException {
        return filterAllAddresses(InetAddress::isLoopbackAddress, "no up-and-running loopback addresses found");
    }

    public static InetAddress[] getSiteLocalAddresses() throws IOException {
        return filterAllAddresses(InetAddress::isSiteLocalAddress, "No up-and-running site-local (private) addresses found");
    }

    public static InetAddress[] getGlobalAddresses() throws IOException {
        return filterAllAddresses(
                address -> !address.isLoopbackAddress()
                        && !address.isSiteLocalAddress()
                        && !address.isLinkLocalAddress(),
                "no up-and-running global-scope (public) addresses found");
    }

    public static InetAddress[] getAllAddresses() throws IOException {
        return filterAllAddresses(address -> true, "no up-and-running addresses found");
    }

    public static Optional<NetworkInterface> getInterfaceByName(List<NetworkInterface> networkInterfaces, String name) {
        return networkInterfaces.stream().filter(netIf -> name.equals(netIf.getName())).findFirst();
    }

    public static InetAddress[] getAddressesForInterface(String name) throws SocketException {
        Optional<NetworkInterface> networkInterface = getInterfaceByName(getInterfaces(), name);

        if (networkInterface.isEmpty()) {
            throw new IllegalArgumentException("No interface named '" + name + "' found, got " + getInterfaces());
        }
        if (!networkInterface.get().isUp()) {
            throw new IllegalArgumentException("Interface '" + name + "' is not up and running");
        }
        List<InetAddress> list = Collections.list(networkInterface.get().getInetAddresses());
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Interface '" + name + "' has no internet addresses");
        }
        return list.toArray(new InetAddress[0]);
    }

    public static InetAddress[] filterIPV4(InetAddress[] addresses) {
        List<InetAddress> list = new ArrayList<>();
        for (InetAddress address : addresses) {
            if (address instanceof Inet4Address) {
                list.add(address);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No ipv4 addresses found in " + Arrays.toString(addresses));
        }
        return list.toArray(new InetAddress[0]);
    }

    public static InetAddress[] filterIPV6(InetAddress[] addresses) {
        List<InetAddress> list = new ArrayList<>();
        for (InetAddress address : addresses) {
            if (address instanceof Inet6Address) {
                list.add(address);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No ipv6 addresses found in " + Arrays.toString(addresses));
        }
        return list.toArray(new InetAddress[0]);
    }

    public static boolean isValidAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return isValidAddress(inetAddress);
        } catch (UnknownHostException e) {}
        return false;
    }

    public static boolean isValidAddress(InetAddress address) {
        return (address != null && !address.isLoopbackAddress() && !address.isAnyLocalAddress());
    }

    public static String getMacAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return getMacAddress(inetAddress);
        } catch (Exception e) {}
        return null;
    }

    public static String getMacAddress(InetAddress inetAddress) {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(inetAddress);
            byte[] hardwareAddress = ni.getHardwareAddress();
            String[] hexadecimal = new String[hardwareAddress.length];
            for (int i = 0; i < hardwareAddress.length; i++) {
                hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
            }
            return String.join("-", hexadecimal);
        } catch (Exception e) {}
        return null;
    }

    public static String getLocalMacAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return getMacAddress(localHost);
        } catch (Exception e) {}
        return null;
    }

    public static String getHostName(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }

        if (socketAddress instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();
            if(addr != null){
                return addr.getHostAddress();
            }
        }

        return null;
    }

    public static String getLocalAddressByDatagram() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            logger.error(e,"Failed to retrieving ip address.");
        }
        return null;
    }

    public static int getAvailablePort() {
        for (int i = 0; i < 50; i++) {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                int port = serverSocket.getLocalPort();
                if (port != 0) {
                    return port;
                }
            }
            catch (IOException ignored) {}
        }

        throw new RuntimeException("Could not find a free permitted port on the machine.");
    }

    public static String hostAndPortToUrlString(String host, int port) throws UnknownHostException {
        return ipAddressAndPortToUrlString(InetAddress.getByName(host), port);
    }

    public static String ipAddressAndPortToUrlString(InetAddress address, int port) {
        return ipAddressToUrlString(address) + ':' + port;
    }

    public static String ipAddressToUrlString(InetAddress address) {
        if (address == null) {
            throw new NullPointerException("address is null");
        }
        else if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        else if (address instanceof Inet6Address) {
            return getIPv6UrlRepresentation((Inet6Address) address);
        }
        else {
            throw new IllegalArgumentException("Unrecognized type of InetAddress: " + address);
        }
    }

    private static String getIPv6UrlRepresentation(Inet6Address address) {
        return getIPv6UrlRepresentation(address.getAddress());
    }

    private static String getIPv6UrlRepresentation(byte[] addressBytes) {
        
        int[] hextets = new int[8];
        for (int i = 0; i < hextets.length; i++) {
            hextets[i] = (addressBytes[2 * i] & 0xFF) << 8 | (addressBytes[2 * i + 1] & 0xFF);
        }

        int bestRunStart = -1;
        int bestRunLength = -1;
        int runStart = -1;
        for (int i = 0; i < hextets.length + 1; i++) {
            if (i < hextets.length && hextets[i] == 0) {
                if (runStart < 0) {
                    runStart = i;
                }
            } else if (runStart >= 0) {
                int runLength = i - runStart;
                if (runLength > bestRunLength) {
                    bestRunStart = runStart;
                    bestRunLength = runLength;
                }
                runStart = -1;
            }
        }
        if (bestRunLength >= 2) {
            Arrays.fill(hextets, bestRunStart, bestRunStart + bestRunLength, -1);
        }

        StringBuilder buf = new StringBuilder(40);
        buf.append('[');

        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean thisIsNumber = hextets[i] >= 0;
            if (thisIsNumber) {
                if (lastWasNumber) {
                    buf.append(':');
                }
                buf.append(Integer.toHexString(hextets[i]));
            } else {
                if (i == 0 || lastWasNumber) {
                    buf.append("::");
                }
            }
            lastWasNumber = thisIsNumber;
        }
        buf.append(']');
        return buf.toString();
    }

    public static boolean isIPv4Address(final String input) {   
        return IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv4AddressExcept127(final String input) {
        return IPV4_PATTERN.matcher(input).matches() && !LOOPBACK_IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv4MappedIPv64Address(final String input) {
        return IPV4_MAPPED_IPV6_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(final String input) {
        int colonCount = 0;
        for(int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == COLON_CHAR) {
                colonCount++;
            }
        }
        return  colonCount <= MAX_COLON_COUNT && IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6Address(final String input) {   
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }

    public static void formatAddress(
            final StringBuilder buffer,
            final SocketAddress socketAddress) {
        Assert.that(buffer).notNull("buffer cannot be null");
        if (socketAddress instanceof InetSocketAddress) {
            final InetSocketAddress socketaddr = (InetSocketAddress) socketAddress;
            final InetAddress inetaddr = socketaddr.getAddress();
            if (inetaddr != null) {
                buffer.append(inetaddr.getHostAddress()).append(':').append(socketaddr.getPort());
            } else {
                buffer.append(socketAddress);
            }
        } else {
            buffer.append(socketAddress);
        }
    }

    public static String getCanonicalLocalHostName() {
        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getCanonicalHostName();
        } catch (final UnknownHostException ex) {
            return "localhost";
        }
    }

    public static String longToIpV4(long longIp) {
        int octet3 = (int) ((longIp >> 24) % 256);
        int octet2 = (int) ((longIp >> 16) % 256);
        int octet1 = (int) ((longIp >> 8) % 256);
        int octet0 = (int) ((longIp) % 256);
        return octet3 + "." + octet2 + "." + octet1 + "." + octet0;
    }

    public static long ipV4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        return (Long.parseLong(octets[0]) << 24) + (Integer.parseInt(octets[1]) << 16)
                + (Integer.parseInt(octets[2]) << 8) + Integer.parseInt(octets[3]);
    }

    public static String getIpLocation(String ip) {
        DbSearcher searcher = null;
        try {
            if(ip2region == null) {
                synchronized (NetUtil.class) {
                    if(ip2region == null) {
                        InputStream is = Ready.config().getInputStreamFromFile("ip2region.db");
                        ip2region = is.readAllBytes();
                    }
                }
            }
            DbConfig config = new DbConfig();
            searcher = new DbSearcher(config, ip2region);
            DataBlock dataBlock = searcher.memorySearch(ip);
            String address = dataBlock.getRegion().replace("0|","");
            char symbol = '|';
            if(address.charAt(address.length()-1) == symbol){
                address = address.substring(0,address.length() - 1);
            }
            return address.equals("内网IP|内网IP") ? "内网IP" : address;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(searcher != null){
                try {
                    searcher.close();
                } catch (IOException ignored) {
                }
            }
        }
        return "";
    }

    public static KeyStore loadKeyStore(final String name, final char[] password) {
        try (InputStream stream = Ready.config().getInputStreamFromFile(name)) {
            if (stream == null) {
                String message = "Unable to load keystore '" + name + "', please provide the keystore matching the configuration in bootstrap.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
            loadedKeystore.load(stream, password);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error(e,"Unable to load keystore " + name);
            throw new RuntimeException("Unable to load keystore " + name, e);
        }
    }

    public static KeyStore loadTrustStore(final String name, final char[] password) {
        try (InputStream stream = Ready.config().getInputStreamFromFile(name)) {
            if (stream == null) {
                String message = "Unable to load truststore '" + name + "', please provide the truststore matching the configuration in bootstrap.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
            loadedKeystore.load(stream, password);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load truststore " + name, e);
            throw new RuntimeException("Unable to load truststore " + name, e);
        }
    }
}
