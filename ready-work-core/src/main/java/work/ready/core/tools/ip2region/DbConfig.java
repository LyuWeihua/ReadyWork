package work.ready.core.tools.ip2region;

public class DbConfig
{
    
    private int totalHeaderSize;

    private int indexBlockSize;

    public DbConfig( int totalHeaderSize ) throws DbMakerConfigException
    {
        if ( (totalHeaderSize % 8) != 0 ) {
            throw new DbMakerConfigException("totalHeaderSize must be times of 8");
        }

        this.totalHeaderSize = totalHeaderSize;
        this.indexBlockSize  = 8192; 
    }

    public DbConfig() throws DbMakerConfigException
    {
        this(8 * 2048);
    }

    public int getTotalHeaderSize()
    {
        return totalHeaderSize;
    }

    public DbConfig setTotalHeaderSize(int totalHeaderSize)
    {
        this.totalHeaderSize = totalHeaderSize;
        return this;
    }

    public int getIndexBlockSize()
    {
        return indexBlockSize;
    }

    public DbConfig setIndexBlockSize(int dataBlockSize)
    {
        this.indexBlockSize = dataBlockSize;
        return this;
    }
}
