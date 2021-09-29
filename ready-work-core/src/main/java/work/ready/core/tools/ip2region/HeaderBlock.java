package work.ready.core.tools.ip2region;

public class HeaderBlock
{
    
    private long indexStartIp;

    private int indexPtr;

    public HeaderBlock( long indexStartIp, int indexPtr )
    {
        this.indexStartIp = indexStartIp;
        this.indexPtr = indexPtr;
    }

    public long getIndexStartIp()
    {
        return indexStartIp;
    }

    public HeaderBlock setIndexStartIp(long indexStartIp)
    {
        this.indexStartIp = indexStartIp;
        return this;
    }

    public int getIndexPtr()
    {
        return indexPtr;
    }

    public HeaderBlock setIndexPtr(int indexPtr)
    {
        this.indexPtr = indexPtr;
        return this;
    }

    public byte[] getBytes()
    {
        
        byte[] b = new byte[8];

        Util.writeIntLong(b, 0, indexStartIp);
        Util.writeIntLong(b, 4, indexPtr);

        return b;
    }
}
