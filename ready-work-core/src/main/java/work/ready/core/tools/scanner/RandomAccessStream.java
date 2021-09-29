package work.ready.core.tools.scanner;

import java.io.InputStream;

public interface RandomAccessStream {
    InputStream newInputStream();
    void setOffset(int offset);
}
