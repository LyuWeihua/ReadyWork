package work.ready.core.tools.ip2region;

public class Util
{
    
    public static void write( byte[] b, int offset, long v, int bytes)
    {
        for ( int i = 0; i < bytes; i++ ) {
            b[offset++] = (byte)((v >>> (8 * i)) & 0xFF);
        }
    }

    public static void writeIntLong( byte[] b, int offset, long v )
    {
        b[offset++] = (byte)((v >>  0) & 0xFF);
        b[offset++] = (byte)((v >>  8) & 0xFF);
        b[offset++] = (byte)((v >> 16) & 0xFF);
        b[offset  ] = (byte)((v >> 24) & 0xFF);
    }

    public static long getIntLong( byte[] b, int offset )
    {
        return (
            ((b[offset++] & 0x000000FFL)) |
            ((b[offset++] <<  8) & 0x0000FF00L) |
            ((b[offset++] << 16) & 0x00FF0000L) |
            ((b[offset  ] << 24) & 0xFF000000L)
        );
    }

    public static int getInt3( byte[] b, int offset )
    {
        return (
            (b[offset++] & 0x000000FF) |
            (b[offset++] & 0x0000FF00) |
            (b[offset  ] & 0x00FF0000)
        );
    }

    public static int getInt2( byte[] b, int offset )
    {
        return (
            (b[offset++] & 0x000000FF) |
            (b[offset  ] & 0x0000FF00)
        );
    }

    public static int getInt1( byte[] b, int offset )
    {
        return (
            (b[offset] & 0x000000FF)
        );
    }

    public static long ip2long( String ip )
    {
        String[] p = ip.split("\\.");
        if ( p.length != 4 ) return 0;

        int p1 = ((Integer.valueOf(p[0]) << 24) & 0xFF000000);
        int p2 = ((Integer.valueOf(p[1]) << 16) & 0x00FF0000);
        int p3 = ((Integer.valueOf(p[2]) <<  8) & 0x0000FF00);
        int p4 = ((Integer.valueOf(p[3]) <<  0) & 0x000000FF);

        return ((p1 | p2 | p3 | p4) & 0xFFFFFFFFL);
    }

    public static String long2ip( long ip )
    {
        StringBuilder sb = new StringBuilder();

        sb
        .append((ip >> 24) & 0xFF).append('.')
        .append((ip >> 16) & 0xFF).append('.')
        .append((ip >>  8) & 0xFF).append('.')
        .append((ip >>  0) & 0xFF);

        return sb.toString();
    }
}
