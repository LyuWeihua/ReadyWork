package work.ready.core.tools.ip2region;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DbSearcher
{
    public static final int BTREE_ALGORITHM  = 1;
    public static final int BINARY_ALGORITHM = 2;
    public static final int MEMORY_ALGORITYM = 3;

    private DbConfig dbConfig = null;

    private RandomAccessFile raf = null;

    private long[] HeaderSip = null;
    private int[]  HeaderPtr = null;
    private int headerLength;

    private long firstIndexPtr = 0;
    private long lastIndexPtr = 0;
    private int totalIndexBlocks = 0;

    private byte[] dbBinStr = null;

    public DbSearcher( DbConfig dbConfig, String dbFile ) throws FileNotFoundException
    {
        this.dbConfig = dbConfig;
        raf = new RandomAccessFile(dbFile, "r");
    }

    public DbSearcher(DbConfig dbConfig, byte[] dbBinStr)
    {
        this.dbConfig = dbConfig;
        this.dbBinStr = dbBinStr;

        firstIndexPtr = Util.getIntLong(dbBinStr, 0);
        lastIndexPtr  = Util.getIntLong(dbBinStr, 4);
        totalIndexBlocks = (int)((lastIndexPtr - firstIndexPtr)/IndexBlock.getIndexBlockLength()) + 1;
    }

    public DataBlock memorySearch(long ip) throws IOException
    {
        int blen = IndexBlock.getIndexBlockLength();
        if ( dbBinStr == null ) {
            dbBinStr = new byte[(int)raf.length()];
            raf.seek(0L);
            raf.readFully(dbBinStr, 0, dbBinStr.length);

            firstIndexPtr    = Util.getIntLong(dbBinStr, 0);
            lastIndexPtr     = Util.getIntLong(dbBinStr, 4);
            totalIndexBlocks = (int)((lastIndexPtr - firstIndexPtr)/blen) + 1;
        }

        int l = 0, h = totalIndexBlocks;
        long sip, eip, dataptr = 0;
        while ( l <= h ) {
            int m = (l + h) >> 1;
            int p = (int)(firstIndexPtr + m * blen);

            sip = Util.getIntLong(dbBinStr, p);
            if ( ip < sip ) {
                h = m - 1;
            } else {
                eip = Util.getIntLong(dbBinStr, p + 4);
                if ( ip > eip ) {
                    l = m + 1;
                } else {
                    dataptr = Util.getIntLong(dbBinStr, p + 8);
                    break;
                }
            }
        }

        if ( dataptr == 0 ) return null;

        int dataLen = (int)((dataptr >> 24) & 0xFF);
        int dataPtr = (int)((dataptr & 0x00FFFFFF));
        int city_id = (int)Util.getIntLong(dbBinStr, dataPtr);
        String region = new String(dbBinStr, dataPtr + 4, dataLen - 4, "UTF-8");

        return new DataBlock(city_id, region, dataPtr);
    }

    public DataBlock memorySearch( String ip ) throws IOException
    {
        return memorySearch(Util.ip2long(ip));
    }

    public DataBlock getByIndexPtr( long ptr ) throws IOException
    {
        raf.seek(ptr);
        byte[] buffer = new byte[12];
        raf.readFully(buffer, 0, buffer.length);

        long extra = Util.getIntLong(buffer, 8);

        int dataLen = (int)((extra >> 24) & 0xFF);
        int dataPtr = (int)((extra & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);

        int city_id = (int)Util.getIntLong(data, 0);
        String region = new String(data, 4, data.length - 4, "UTF-8");

        return new DataBlock(city_id, region, dataPtr);
    }

    public DataBlock btreeSearch( long ip ) throws IOException
    {
        
        if ( HeaderSip == null )  {
            raf.seek(8L);    
            byte[] b = new byte[dbConfig.getTotalHeaderSize()];
            
            raf.readFully(b, 0, b.length);

            int len = b.length >> 3, idx = 0;  
            HeaderSip = new long[len];
            HeaderPtr = new int [len];
            long startIp, dataPtr;
            for ( int i = 0; i < b.length; i += 8 ) {
                startIp = Util.getIntLong(b, i);
                dataPtr = Util.getIntLong(b, i + 4);
                if ( dataPtr == 0 ) break;

                HeaderSip[idx] = startIp;
                HeaderPtr[idx] = (int)dataPtr;
                idx++;
            }

            headerLength = idx;
        }

        if ( ip == HeaderSip[0] ) {
            return getByIndexPtr(HeaderPtr[0]);
        } else if ( ip == HeaderSip[headerLength-1] ) {
            return getByIndexPtr(HeaderPtr[headerLength-1]);
        }

        int l = 0, h = headerLength, sptr = 0, eptr = 0;
        while ( l <= h ) {
            int m = (l + h) >> 1;

            if ( ip == HeaderSip[m] ) {
                if ( m > 0 ) {
                    sptr = HeaderPtr[m-1];
                    eptr = HeaderPtr[m  ];
                } else {
                    sptr = HeaderPtr[m ];
                    eptr = HeaderPtr[m+1];
                }

                break;
            }

            if ( ip < HeaderSip[m] ) {
                if ( m == 0 ) {
                    sptr = HeaderPtr[m  ];
                    eptr = HeaderPtr[m+1];
                    break;
                } else if ( ip > HeaderSip[m-1] ) {
                    sptr = HeaderPtr[m-1];
                    eptr = HeaderPtr[m  ];
                    break;
                }
                h = m - 1;
            } else {
                if ( m == headerLength - 1 ) {
                    sptr = HeaderPtr[m-1];
                    eptr = HeaderPtr[m  ];
                    break;
                } else if ( ip <= HeaderSip[m+1] ) {
                    sptr = HeaderPtr[m  ];
                    eptr = HeaderPtr[m+1];
                    break;
                }
                l = m + 1;
            }
        }

        if ( sptr == 0 ) return null;

        int blockLen = eptr - sptr, blen = IndexBlock.getIndexBlockLength();
        byte[] iBuffer = new byte[blockLen + blen];    
        raf.seek(sptr);
        raf.readFully(iBuffer, 0, iBuffer.length);

        l = 0; h = blockLen / blen;
        long sip, eip, dataptr = 0;
        while ( l <= h ) {
            int m = (l + h) >> 1;
            int p = m * blen;
            sip = Util.getIntLong(iBuffer, p);
            if ( ip < sip ) {
                h = m - 1;
            } else {
                eip = Util.getIntLong(iBuffer, p + 4);
                if ( ip > eip ) {
                    l = m + 1;
                } else {
                    dataptr = Util.getIntLong(iBuffer, p + 8);
                    break;
                }
            }
        }

        if ( dataptr == 0 ) return null;

        int dataLen = (int)((dataptr >> 24) & 0xFF);
        int dataPtr = (int)((dataptr & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);

        int city_id = (int)Util.getIntLong(data, 0);
        String region = new String(data, 4, data.length - 4, "UTF-8");

        return new DataBlock(city_id, region, dataPtr);
    }

    public DataBlock btreeSearch( String ip ) throws IOException
    {
        return btreeSearch(Util.ip2long(ip));
    }

    public DataBlock binarySearch( long ip ) throws IOException
    {
        int blen = IndexBlock.getIndexBlockLength();
        if ( totalIndexBlocks == 0 ) {
            raf.seek(0L);
            byte[] superBytes = new byte[8];
            raf.readFully(superBytes, 0, superBytes.length);
            
            firstIndexPtr = Util.getIntLong(superBytes, 0);
            lastIndexPtr = Util.getIntLong(superBytes, 4);
            totalIndexBlocks = (int)((lastIndexPtr - firstIndexPtr)/blen) + 1;
        }

        int l = 0, h = totalIndexBlocks;
        byte[] buffer = new byte[blen];
        long sip, eip, dataptr = 0;
        while ( l <= h ) {
            int m = (l + h) >> 1;
            raf.seek(firstIndexPtr + m * blen);    
            raf.readFully(buffer, 0, buffer.length);
            sip = Util.getIntLong(buffer, 0);
            if ( ip < sip ) {
                h = m - 1;
            } else {
                eip = Util.getIntLong(buffer, 4);
                if ( ip > eip ) {
                    l = m + 1;
                } else {
                    dataptr = Util.getIntLong(buffer, 8);
                    break;
                }
            }
        }

        if ( dataptr == 0 ) return null;

        int dataLen = (int)((dataptr >> 24) & 0xFF);
        int dataPtr = (int)((dataptr & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);

        int city_id = (int)Util.getIntLong(data, 0);
        String region = new String(data, 4, data.length - 4, "UTF-8");

        return new DataBlock(city_id, region, dataPtr);
    }

    public DataBlock binarySearch( String ip ) throws IOException
    {
        return binarySearch(Util.ip2long(ip));
    }

    public DbConfig getDbConfig()
    {
        return dbConfig;
    }

    public void close() throws IOException
    {
        HeaderSip = null;    
        HeaderPtr = null;
        dbBinStr  = null;
        if ( raf != null ) {
            raf.close();
        }
    }

}
