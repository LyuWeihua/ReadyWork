/**
 *
 * Original work Copyright (C) 2008 The Android Open Source Project
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

package work.ready.core.component.time;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SntpClient {

    private static final Log logger = LogFactory.getLog(SntpClient.class);

    public static final int RESPONSE_INDEX_ORIGINATE_TIME = 0;
    public static final int RESPONSE_INDEX_RECEIVE_TIME = 1;
    public static final int RESPONSE_INDEX_TRANSMIT_TIME = 2;
    public static final int RESPONSE_INDEX_RESPONSE_TIME = 3;
    public static final int RESPONSE_INDEX_ROOT_DELAY = 4;
    public static final int RESPONSE_INDEX_DISPERSION = 5;
    public static final int RESPONSE_INDEX_STRATUM = 6;
    public static final int RESPONSE_INDEX_RESPONSE_TICKS = 7;
    public static final int RESPONSE_INDEX_SIZE = 8;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE = 3;
    private static final int NTP_VERSION = 3;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int INDEX_VERSION = 0;
    private static final int INDEX_ROOT_DELAY = 4;
    private static final int INDEX_ROOT_DISPERSION = 8;
    private static final int INDEX_ORIGINATE_TIME = 24;
    private static final int INDEX_RECEIVE_TIME = 32;
    private static final int INDEX_TRANSMIT_TIME = 40;

    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    private AtomicLong _cachedLastTicks = new AtomicLong();
    private AtomicLong _cachedLastSyncTime = new AtomicLong();
    private AtomicLong _cachedSntpTime = new AtomicLong();
    private AtomicBoolean _sntpInitialized = new AtomicBoolean(false);

    public static long getRoundTripDelay(long[] response) {
        return (response[RESPONSE_INDEX_RESPONSE_TIME] - response[RESPONSE_INDEX_ORIGINATE_TIME]) -
                (response[RESPONSE_INDEX_TRANSMIT_TIME] - response[RESPONSE_INDEX_RECEIVE_TIME]);
    }

    public static long getClockOffset(long[] response) {
        return ((response[RESPONSE_INDEX_RECEIVE_TIME] - response[RESPONSE_INDEX_ORIGINATE_TIME]) +
                (response[RESPONSE_INDEX_TRANSMIT_TIME] - response[RESPONSE_INDEX_RESPONSE_TIME])) / 2;
    }

    synchronized long[] requestTime(String ntpHost,
                                    float rootDelayMax,
                                    float rootDispersionMax,
                                    int serverResponseDelayMax,
                                    int timeoutInMillis
    )
            throws Exception {

        DatagramSocket socket = null;

        try {

            byte[] buffer = new byte[NTP_PACKET_SIZE];
            InetAddress address = InetAddress.getByName(ntpHost);

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

            writeVersion(buffer);

            long requestTime = System.currentTimeMillis();
            long requestTicks = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

            writeTimeStamp(buffer, INDEX_TRANSMIT_TIME, requestTime);

            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutInMillis);
            socket.send(request);

            long t[] = new long[RESPONSE_INDEX_SIZE];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            long responseTicks = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            t[RESPONSE_INDEX_RESPONSE_TICKS] = responseTicks;

            long originateTime = readTimeStamp(buffer, INDEX_ORIGINATE_TIME);     
            long receiveTime = readTimeStamp(buffer, INDEX_RECEIVE_TIME);         
            long transmitTime = readTimeStamp(buffer, INDEX_TRANSMIT_TIME);       
            long responseTime = requestTime + (responseTicks - requestTicks);       

            t[RESPONSE_INDEX_ORIGINATE_TIME] = originateTime;
            t[RESPONSE_INDEX_RECEIVE_TIME] = receiveTime;
            t[RESPONSE_INDEX_TRANSMIT_TIME] = transmitTime;
            t[RESPONSE_INDEX_RESPONSE_TIME] = responseTime;

            t[RESPONSE_INDEX_ROOT_DELAY] = read(buffer, INDEX_ROOT_DELAY);
            double rootDelay = doubleMillis(t[RESPONSE_INDEX_ROOT_DELAY]);
            if (rootDelay > rootDelayMax) {
                throw new NtpResponseException(
                        "Invalid response from NTP server. %s violation. %f [actual] > %f [expected]",
                        "root_delay",
                        (float) rootDelay,
                        rootDelayMax);
            }

            t[RESPONSE_INDEX_DISPERSION] = read(buffer, INDEX_ROOT_DISPERSION);
            double rootDispersion = doubleMillis(t[RESPONSE_INDEX_DISPERSION]);
            if (rootDispersion > rootDispersionMax) {
                throw new NtpResponseException(
                        "Invalid response from NTP server. %s violation. %f [actual] > %f [expected]",
                        "root_dispersion",
                        (float) rootDispersion,
                        rootDispersionMax);
            }

            final byte mode = (byte) (buffer[0] & 0x7);
            if (mode != 4 && mode != 5) {
                throw new NtpResponseException("untrusted mode value for TimeWorker: " + mode);
            }

            final int stratum = buffer[1] & 0xff;
            t[RESPONSE_INDEX_STRATUM] = stratum;
            if (stratum < 1 || stratum > 15) {
                throw new NtpResponseException("untrusted stratum value for TimeWorker: " + stratum);
            }

            final byte leap = (byte) ((buffer[0] >> 6) & 0x3);
            if (leap == 3) {
                throw new NtpResponseException("unsynchronized server responded for TimeWorker");
            }

            double delay = Math.abs((responseTime - originateTime) - (transmitTime - receiveTime));
            if (delay >= serverResponseDelayMax) {
                throw new NtpResponseException(
                        "%s too large for comfort %f [actual] >= %f [expected]",
                        "server_response_delay",
                        (float) delay,
                        serverResponseDelayMax);
            }

            long timeElapsedSinceRequest = Math.abs(originateTime - System.currentTimeMillis());
            if (timeElapsedSinceRequest >= 10_000) {
                throw new NtpResponseException("Request was sent more than 10 seconds back " +
                        timeElapsedSinceRequest);
            }

            cacheTimeInfo(t);
            _sntpInitialized.set(true);
            logger.info("SNTP successful response from " + ntpHost);

            return t;

        } catch (Exception e) {
            logger.warn("SNTP request failed for " + ntpHost);
            throw e;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    void cacheTimeInfo(long[] response) {
        _cachedSntpTime.set(sntpTime(response));
        _cachedLastTicks.set(response[RESPONSE_INDEX_RESPONSE_TICKS]); 
        _cachedLastSyncTime.set(response[RESPONSE_INDEX_RESPONSE_TIME]); 
    }

    long sntpTime(long[] response) {
        long clockOffset = getClockOffset(response);
        long responseTime = response[RESPONSE_INDEX_RESPONSE_TIME];
        return responseTime + clockOffset;
    }

    boolean wasInitialized() {
        return _sntpInitialized.get();
    }

    long getCachedSntpTime() {
        return _cachedSntpTime.get();
    }

    long getCachedLastTicks() {
        return _cachedLastTicks.get();
    }

    long getCachedLastSyncTime() {
        return _cachedLastSyncTime.get();
    }

    private void writeVersion(byte[] buffer) {

        buffer[INDEX_VERSION] = NTP_MODE | (NTP_VERSION << 3);
    }

    private void writeTimeStamp(byte[] buffer, int offset, long time) {

        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;

        seconds += OFFSET_1900_TO_1970;

        buffer[offset++] = (byte) (seconds >> 24);
        buffer[offset++] = (byte) (seconds >> 16);
        buffer[offset++] = (byte) (seconds >> 8);
        buffer[offset++] = (byte) (seconds >> 0);

        long fraction = milliseconds * 0x100000000L / 1000L;

        buffer[offset++] = (byte) (fraction >> 24);
        buffer[offset++] = (byte) (fraction >> 16);
        buffer[offset++] = (byte) (fraction >> 8);

        buffer[offset++] = (byte) (Math.random() * 255.0);
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read(buffer, offset);
        long fraction = read(buffer, offset + 4);

        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    private long read(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset + 1];
        byte b2 = buffer[offset + 2];
        byte b3 = buffer[offset + 3];

        return ((long) ui(b0) << 24) +
                ((long) ui(b1) << 16) +
                ((long) ui(b2) << 8) +
                (long) ui(b3);
    }

    private int ui(byte b) {
        return b & 0xFF;
    }

    private double doubleMillis(long fix) {
        return fix / 65.536D;
    }
}
