/**
 *
 * Original work Copyright Snowflake
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

package work.ready.core.component.snowflake;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.DateUtil;
import work.ready.core.tools.NetUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.validator.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class IdWorker {
    private static final Log logger = LogFactory.getLog(IdWorker.class);

    public static final int TOTAL_BITS = 1 << 6;

    protected int signBits = 1;

    protected String epochDate = "2020-01-01";
    protected long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(1577808000000L);

    protected int tolerableClockMovedBackwardsSeconds = 2;

    protected int timeBits = 32;

    protected int workerBits = 18;

    protected int seqBits = 13;

    protected long sequence = 0L;
    protected long lastSecond = -1L;

    protected long maxDeltaSeconds;
    protected long maxWorkerId;
    protected long maxSequence;

    protected int timestampShift;
    protected int workerIdShift;

    protected long workerId;

    public IdWorker(long workerId, String epochDate, int timeBits, int workerBits, int seqBits) {
        this.workerId = workerId;

        int allocateTotalBits = signBits + timeBits + workerBits + seqBits;
        Assert.that(allocateTotalBits == TOTAL_BITS).isTrue("allocate not enough 64 bits");

        this.maxDeltaSeconds = ~(-1L << timeBits);
        this.maxWorkerId = ~(-1L << workerBits);
        this.maxSequence = ~(-1L << seqBits);
        Assert.that(workerId <= maxWorkerId).isTrue(String.format("workerId exceed the max value %d", maxWorkerId));

        setEpochDate(epochDate);
        setTimeBits(timeBits);
        setWorkerBits(workerBits);
        setSeqBits(seqBits);

        this.timestampShift = workerBits + seqBits;
        this.workerIdShift = seqBits;
    }

    public IdWorker(long workerId, int timeBits, int workerBits, int seqBits) {
        this(workerId, null, timeBits, workerBits, seqBits);
    }

    public IdWorker(long workerId){
        this.workerId = workerId;

        int allocateTotalBits = signBits + timeBits + workerBits + seqBits;
        Assert.that(allocateTotalBits == TOTAL_BITS).isTrue("allocate not enough 64 bits");

        this.maxDeltaSeconds = ~(-1L << timeBits);
        this.maxWorkerId = ~(-1L << workerBits);
        this.maxSequence = ~(-1L << seqBits);
        Assert.that(workerId <= maxWorkerId).isTrue(String.format("workerId exceed the max value %d", maxWorkerId));

        this.timestampShift = workerBits + seqBits;
        this.workerIdShift = seqBits;
    }

    public long getId() throws IdGenerateException {
        try {
            return nextId();
        } catch (Exception e) {
            logger.error("IdWorker exception: ", e);
            throw new IdGenerateException(e);
        }
    }

    public String parseId(long uid) {
        
        long sequence = (uid << (TOTAL_BITS - seqBits)) >>> (TOTAL_BITS - seqBits);
        long workerId = (uid << (timeBits + signBits)) >>> (TOTAL_BITS - workerBits);
        long deltaSeconds = uid >>> (workerBits + seqBits);

        Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
        String thatTimeStr = DateUtil.format(thatTime);

        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                uid, thatTimeStr, workerId, sequence);
    }

    protected synchronized long nextId() {
        long currentSecond = getCurrentSecond();

        if (currentSecond < lastSecond) {
            long refusedSeconds = lastSecond - currentSecond;
            if(refusedSeconds > tolerableClockMovedBackwardsSeconds) {
                throw new IdGenerateException("Clock moved backwards for %d seconds, exceeds the max tolerable seconds.", refusedSeconds);
            } else {
                logger.error("Clock moved backwards, waiting here and refusing for %s seconds", refusedSeconds);
                getNextSecond(lastSecond);
            }
        }

        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & maxSequence;
            
            if (sequence == 0) {
                currentSecond = getNextSecond(lastSecond);
            }

        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        long deltaSeconds = currentSecond - epochSeconds;
        return (deltaSeconds << timestampShift) | (workerId << workerIdShift) | sequence;
    }

    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(Ready.currentTimeMillis());
        if (currentSecond - epochSeconds > maxDeltaSeconds) {
            throw new IdGenerateException("Timestamp bits is exhausted. Refusing UID generate. Now: " + currentSecond);
        }

        return currentSecond;
    }

    public void setTimeBits(int timeBits) {
        if (timeBits > 0) {
            this.timeBits = timeBits;
        }
    }

    public void setWorkerBits(int workerBits) {
        if (workerBits > 0) {
            this.workerBits = workerBits;
        }
    }

    public void setSeqBits(int seqBits) {
        if (seqBits > 0) {
            this.seqBits = seqBits;
        }
    }

    public void setEpochDate(String epochDate) {
        if(!StrUtil.isBlank(epochDate)) {
            this.epochDate = epochDate;
            this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(DateUtil.parse(epochDate).getTime());
        }
    }

    public void setTolerableClockMovedBackwardsSeconds(int tolerableClockMovedBackwardsSeconds) {
        this.tolerableClockMovedBackwardsSeconds = tolerableClockMovedBackwardsSeconds;
    }

    public static long getWorkerIdByIPV4(int bits) throws IdGenerateException {
        int shift = 64 - bits;
        try {
            InetAddress address = InetAddress.getLocalHost();
            long ip = NetUtil.ipV4ToLong(address.getHostAddress());
            long workerId = (ip << shift) >>> shift;
            return workerId;
        } catch (UnknownHostException e) {
            logger.error("IdWorker exception. ", e);
            throw new IdGenerateException(e);
        }
    }

    public static long getWorkerIdByMacAddress(int bits) throws IdGenerateException {
        int shift = 64 - bits;
        long serverId = NetUtil.getLocalMacAddress().hashCode();
        return (serverId << shift) >>> shift;
    }

}
