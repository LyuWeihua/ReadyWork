package work.ready.core.tools.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

public class RandomAccessFileStream implements RandomAccessStream {
    private RandomAccessFile randomAccessFile;
    public RandomAccessFileStream(RandomAccessFile randomAccessFile){
        this.randomAccessFile = randomAccessFile;
    }
    @Override
    public InputStream newInputStream() {
        return Channels.newInputStream((randomAccessFile.getChannel()));
    }
    @Override
    public void setOffset(int offset) {
        try {
            randomAccessFile.getChannel().position(offset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
