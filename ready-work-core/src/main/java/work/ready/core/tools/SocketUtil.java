/**
 *
 * Original work Copyright apache, Spring
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

import work.ready.core.server.Ready;
import work.ready.core.tools.validator.Assert;

import javax.net.ServerSocketFactory;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class SocketUtil {

    public static final int PORT_RANGE_MIN = 1024;

    public static final int PORT_RANGE_MAX = 65535;

    private static final Random random = new Random(Ready.currentTimeMillis());

    public SocketUtil() {
    }

    public static int findAvailableTcpPort() {
        return findAvailableTcpPort(PORT_RANGE_MIN);
    }

    public static int findAvailableTcpPort(int minPort) {
        return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
    }

    public static int findAvailableTcpPort(int minPort, int maxPort) {
        return SocketType.TCP.findAvailablePort(minPort, maxPort);
    }

    public static SortedSet<Integer> findAvailableTcpPorts(int numRequested) {
        return findAvailableTcpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
    }

    public static SortedSet<Integer> findAvailableTcpPorts(int numRequested, int minPort, int maxPort) {
        return SocketType.TCP.findAvailablePorts(numRequested, minPort, maxPort);
    }

    public static int findAvailableUdpPort() {
        return findAvailableUdpPort(PORT_RANGE_MIN);
    }

    public static int findAvailableUdpPort(int minPort) {
        return findAvailableUdpPort(minPort, PORT_RANGE_MAX);
    }

    public static int findAvailableUdpPort(int minPort, int maxPort) {
        return SocketType.UDP.findAvailablePort(minPort, maxPort);
    }

    public static SortedSet<Integer> findAvailableUdpPorts(int numRequested) {
        return findAvailableUdpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
    }

    public static SortedSet<Integer> findAvailableUdpPorts(int numRequested, int minPort, int maxPort) {
        return SocketType.UDP.findAvailablePorts(numRequested, minPort, maxPort);
    }

    private enum SocketType {

        TCP {
            @Override
            protected boolean isPortAvailable(int port) {
                try {
                    ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                            port, 1, InetAddress.getByName("localhost"));
                    serverSocket.close();
                    return true;
                }
                catch (Exception ex) {
                    return false;
                }
            }
        },

        UDP {
            @Override
            protected boolean isPortAvailable(int port) {
                try {
                    DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
                    socket.close();
                    return true;
                }
                catch (Exception ex) {
                    return false;
                }
            }
        };

        protected abstract boolean isPortAvailable(int port);

        private int findRandomPort(int minPort, int maxPort) {
            int portRange = maxPort - minPort;
            return minPort + random.nextInt(portRange + 1);
        }

        int findAvailablePort(int minPort, int maxPort) {
            Assert.that(minPort > 0).isTrue("'minPort' must be greater than 0");
            Assert.that(maxPort >= minPort).isTrue("'maxPort' must be greater than or equal to 'minPort'");
            Assert.that(maxPort <= PORT_RANGE_MAX).isTrue("'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

            int portRange = maxPort - minPort;
            int candidatePort;
            int searchCounter = 0;
            do {
                if (searchCounter > portRange) {
                    throw new IllegalStateException(String.format(
                            "Could not find an available %s port in the range [%d, %d] after %d attempts",
                            name(), minPort, maxPort, searchCounter));
                }
                candidatePort = findRandomPort(minPort, maxPort);
                searchCounter++;
            }
            while (!isPortAvailable(candidatePort));

            return candidatePort;
        }

        SortedSet<Integer> findAvailablePorts(int numRequested, int minPort, int maxPort) {
            Assert.that(minPort > 0).isTrue("'minPort' must be greater than 0");
            Assert.that(maxPort > minPort).isTrue("'maxPort' must be greater than 'minPort'");
            Assert.that(maxPort <= PORT_RANGE_MAX).isTrue("'maxPort' must be less than or equal to " + PORT_RANGE_MAX);
            Assert.that(numRequested > 0).isTrue("'numRequested' must be greater than 0");
            Assert.that((maxPort - minPort) >= numRequested).isTrue("'numRequested' must not be greater than 'maxPort' - 'minPort'");

            SortedSet<Integer> availablePorts = new TreeSet<>();
            int attemptCount = 0;
            while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
                availablePorts.add(findAvailablePort(minPort, maxPort));
            }

            if (availablePorts.size() != numRequested) {
                throw new IllegalStateException(String.format(
                        "Could not find %d available %s ports in the range [%d, %d]",
                        numRequested, name(), minPort, maxPort));
            }

            return availablePorts;
        }
    }

}

