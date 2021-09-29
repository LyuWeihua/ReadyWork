package work.ready.core.tools.scanner;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class FastScanner implements Closeable {
    protected static int MARK_MAX_DEFAULT = 128;
    private InputStream inputStream;
    private RandomAccessStream randomAccessStream;
    private static int buffer_size = 2*1024;
    private byte[] buffer;
    private byte[] lineDelimeter = "\n".getBytes();
    private CurrentByteListener currentByteListener;
    private int readed;
    private int offset;
    private int maxRead;
    private int bufferOffset;
    private int markSize;
    private int markPosition;
    private boolean isStreamTerminated = false;
    private FastByteArrayOutputStream tmpStream = new FastByteArrayOutputStream();
    public FastScanner(RandomAccessFile randomAccessStream) {
        this( randomAccessStream, buffer_size );
    }
    public FastScanner(final RandomAccessFile randomAccessFile, int buffer_size) {
        this(new RandomAccessFileStream(randomAccessFile), buffer_size);
    }

    public FastScanner(RandomAccessStream randomAccessStream, int buffer_size) {
        this( randomAccessStream.newInputStream(), buffer_size );
        this.randomAccessStream = randomAccessStream;
    }

    public FastScanner(InputStream inputStream, int buffer_size) {
        this.inputStream = inputStream;
        buffer = new byte[buffer_size];
        if (MARK_MAX_DEFAULT<buffer_size)
            MARK_MAX_DEFAULT = buffer_size/2;
        this.maxRead = buffer.length;
    }

    public FastScanner(byte[] buffer) {
        this.buffer = buffer;
        if (MARK_MAX_DEFAULT<buffer.length)
            MARK_MAX_DEFAULT = buffer.length/2;
        this.maxRead = buffer.length;
    }

    public FastScanner(InputStream inputStream) {
        this(inputStream, buffer_size);
    }

    public void setLineDelimeter(byte[] lineDelimeter) {
        this.lineDelimeter = lineDelimeter;
    }

    public final String nextStringLine(boolean clean) throws IOException {
        return new String(nextLine(clean));
    }

    public final byte[] nextLine(boolean clean) throws IOException {
        byte[] row = this.readToElement(lineDelimeter, MoveEnum.RIGHT_FROM_ELEMENT);
        if (clean) {
            return cleanLine(row);
        }else
            return row;
    }

    public static final byte[] cleanLine(byte[] row) throws IOException {
        try {
            int remove = 0;
            if (row.length>0 && row[row.length - 1] == '\n')
                remove = 1;
            if (row.length>1 && row[row.length - 2] == '\r')
                remove = 2;
            byte[] result = new byte[row.length - remove];
            System.arraycopy(row, 0, result, 0, row.length - remove);
            return result;
        }catch (RuntimeException e){
            throw e;
        }
    }

    public final byte[] nextLine(byte[] delimiter) throws IOException {
        return this.readToElement(delimiter, MoveEnum.RIGHT_FROM_ELEMENT);
    }

    public final byte[] readToElement(String element, MoveEnum moveEnum) throws IOException {
        return readToElement(element.getBytes(), moveEnum);
    }

    public final byte[] readToElement(byte[] elementIntArray, MoveEnum moveEnum) throws IOException {
        if (MoveEnum.LEFT_FROM_ELEMENT==moveEnum)
            return readToLeftElement(elementIntArray);
        else
            return readToRightElement(elementIntArray);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) throws Exception {
        if (this.randomAccessStream ==null)
            throw new Exception("setOffset supported only with randomAccessStream");
        bufferOffset = bufferOffset-(this.offset-offset);

        if (bufferOffset<0 || bufferOffset>=maxRead || this.offset==0) {
            bufferOffset = 0;
            randomAccessStream.setOffset(offset);
            readed = 0;
        }

        this.offset = offset;
        markSize = 0;
        markPosition = 0;
    }

    public boolean isStreamTerminated() {
        return isStreamTerminated;
    }

    public void setCurrentByteListener(CurrentByteListener currentByteListener) {
        this.currentByteListener = currentByteListener;
    }
    public void setMaxRead(int maxRead) throws Exception {
        if (maxRead>buffer.length)
            maxRead = buffer.length;
        this.maxRead = maxRead;
        if (bufferOffset >= maxRead)
            setOffset(offset-bufferOffset);
    }

    public final void readToLeftElement(String element) throws IOException {
        byte[] elementIntArray = element.getBytes();
        readToLeftElement(elementIntArray);
    }
    public final boolean moveToNextLeftElement(String element) throws IOException {
        byte[] elementIntArray = element.getBytes();
        return moveToNextLeftElement(elementIntArray);
    }

    public final boolean moveToNextRightElement(String element) throws IOException {
        byte[] elementIntArray = element.getBytes();
        return moveToNextRightElement(elementIntArray);
    }

    public final boolean moveToNextElement(String element, MoveEnum moveEnum) throws IOException {
        return moveToNextElement(element.getBytes(), moveEnum);
    }
    public final boolean moveToNextElement(byte[] elements, MoveEnum moveEnum) throws IOException {
        if (MoveEnum.LEFT_FROM_ELEMENT == moveEnum)
            return moveToNextLeftElement(elements);
        else
            return moveToNextRightElement(elements);
    }
    public final byte[] retrieveNextXmlTagBytes(MoveEnum moveEnum) throws IOException {
        return retrieveNextTokenBytes('<', '>', moveEnum, MARK_MAX_DEFAULT);
    }
    public final byte[] retrieveNextXmlTagRightBytes() throws IOException {
        return retrieveNextRightTokenBytes('<', '>');
    }
    public final byte[] retrieveNextXmlTagLeftBytes() throws IOException {
        return retrieveNextLeftTokenBytes('<', '>', MARK_MAX_DEFAULT);
    }
    public final byte[] retrieveNextXmlTag(MoveEnum moveEnum) throws IOException {
        return retrieveNextToken('<', '>', moveEnum, MARK_MAX_DEFAULT);
    }

    public final byte[] retrieveNextToken(String leftAsArray, String rightAsArray, MoveEnum moveEnum) throws IOException {
        return retrieveNextToken(leftAsArray.getBytes(), rightAsArray.getBytes(), moveEnum, MARK_MAX_DEFAULT);
    }

    public final byte[] retrieveNextToken(byte[] leftAsArray, byte[] rightAsArray, MoveEnum moveEnum) throws IOException {
        return retrieveNextToken(leftAsArray, rightAsArray, moveEnum, MARK_MAX_DEFAULT);
    }

    public final byte[] retrieveNextToken(byte[] elementLeft, byte[] elementRight, MoveEnum moveEnum, int markMax) throws IOException {
        byte[] retrieveNextTokenBytes;
        if (moveEnum==MoveEnum.LEFT_FROM_ELEMENT)
            retrieveNextTokenBytes = retrieveNextTokenLeftBytes(elementLeft, elementRight, markMax);
        else
            retrieveNextTokenBytes = retrieveNextTokenRightBytes(elementLeft, elementRight);
        if (retrieveNextTokenBytes == null) {
            return null;
        }
        return retrieveNextTokenBytes;
    }

    public final byte[] retrieveNextToken(char elementLeft, char elementRight, MoveEnum moveEnum, int markMax) throws IOException {
        byte[] retrieveNextTokenBytes = retrieveNextTokenBytes(elementLeft, elementRight, moveEnum, markMax);
        if (retrieveNextTokenBytes == null) {
            return null;
        }
        return retrieveNextTokenBytes;
    }

    public final byte[] retrieveNextTokenBytes(char elementLeft, char elementRight, MoveEnum moveEnum, int markMax) throws IOException {
        if (MoveEnum.LEFT_FROM_ELEMENT==moveEnum)
            return retrieveNextLeftTokenBytes(elementLeft, elementRight, markMax);
        else
            return retrieveNextRightTokenBytes(elementLeft, elementRight);
    }

    public final byte[] retrieveNextRightTokenBytes(char elementLeft, char elementRight) throws IOException {
        try {
            int c;
            tmpStream.reset();
            boolean isElementLeftFounds = false;
            while ((c = read()) != -1) {
                if (c == elementLeft) {
                    isElementLeftFounds = true;
                    break;
                }
            }

            if (!isElementLeftFounds) {
                return null;
            }
            tmpStream.write(elementLeft);

            while ((c = read()) != -1) {
                tmpStream.write(c);
                if (c == elementRight) {
                    return tmpStream.toByteArrayAndReset();
                }
            }
        }finally {
            tmpStream.reset();
        }
        return null;
    }

    public final byte[] retrieveNextLeftTokenBytes(char elementLeft, char elementRight, int markMax) throws IOException {
        try {
            int c;
            tmpStream.reset();
            mark(markMax);
            boolean isElementLeftFounds = false;
            while ((c = read()) != -1) {
                if (c == elementLeft) {
                    isElementLeftFounds = true;
                    break;
                }
                mark(markMax);
            }
            if (!isElementLeftFounds) {
                reset();
                return null;
            }
            tmpStream.write(elementLeft);

            while ((c = read()) != -1) {
                tmpStream.write(c);
                if (c == elementRight) {
                    reset();
                    return tmpStream.toByteArrayAndReset();
                }
            }
            reset();
        }finally {
            tmpStream.reset();
        }
        return null;
    }

    public final byte[] retrieveNextTokenLeftBytes(byte[] elementLeft, byte[] elementRight, int markMax) throws IOException {
        try {
            int c;
            mark(markMax);
            boolean isElementLeftFounds = false;
            int leftIndex = 0;
            while ((c = read()) != -1) {
                if (c == elementLeft[leftIndex]) {
                    leftIndex++;
                } else {
                    mark(markMax);
                    leftIndex = 0;
                    continue;
                }
                if (leftIndex == elementLeft.length) {
                    isElementLeftFounds = true;
                    break;
                }
            }
            if (!isElementLeftFounds) {
                reset();
                return null;
            }
            int rightIndex = 0;
            while ((c = read()) != -1) {
                if (c == elementRight[rightIndex]) {
                    rightIndex++;
                } else {
                    for (int i=0;i<rightIndex;i++)
                        tmpStream.write(elementRight[i]);
                    rightIndex = 0;
                    tmpStream.write(c);
                    continue;
                }
                if (rightIndex == elementRight.length) {
                    reset();
                    return tmpStream.toByteArrayAndReset();
                }
            }
            reset();
        }finally {
            tmpStream.reset();
        }
        return null;
    }

    public final byte[] retrieveNextTokenRightBytes(byte[] elementLeft, byte[] elementRight) throws IOException {
        try {
            int c;
            boolean isElementLeftFounds = false;
            int leftIndex = 0;
            while ((c = read()) != -1) {
                if (c == elementLeft[leftIndex]) {
                    leftIndex++;
                } else {
                    leftIndex = 0;
                    continue;
                }
                if (leftIndex == elementLeft.length) {
                    isElementLeftFounds = true;
                    break;
                }
            }
            if (!isElementLeftFounds) {
                return null;
            }
            int rightIndex = 0;
            while ((c = read()) != -1) {
                if (c == elementRight[rightIndex]) {
                    rightIndex++;
                } else {
                    for (int i=0;i<rightIndex;i++)
                        tmpStream.write(elementRight[i]);
                    rightIndex = 0;
                    tmpStream.write(c);
                    continue;
                }
                if (rightIndex == elementRight.length) {
                    return tmpStream.toByteArrayAndReset();
                }
            }
        }finally {
            tmpStream.reset();
        }
        return null;

    }

    public final byte[] retrieveNextToken(String elementLeft, String elementRight, MoveEnum moveEnum, int markMax) throws IOException {
        byte[] retrieveNextTokenBytes;
        if (moveEnum==MoveEnum.LEFT_FROM_ELEMENT)
            retrieveNextTokenBytes = retrieveNextTokenLeftBytes(elementLeft.getBytes(), elementRight.getBytes(), markMax);
        else
            retrieveNextTokenBytes = retrieveNextTokenRightBytes(elementLeft.getBytes(), elementRight.getBytes());
        if (retrieveNextTokenBytes == null) {
            return null;
        }
        return retrieveNextTokenBytes;
    }

    public final boolean moveToNextLeftElement(byte[] elementIntArray) throws IOException {
        int index = 0;
        int c;
        int elementSize = elementIntArray.length;
        mark(elementSize);
        while ((c = read()) != -1) {
            if (c == (int) elementIntArray[index]) {
                index++;
            } else {
                mark(elementSize);
                index = 0;
            }
            if (index == elementSize) {
                reset();
                return true;
            }
        }
        return false;
    }

    public final boolean moveToNextRightElement(byte[] elementIntArray) throws IOException {
        int index = 0;
        int c;
        int elementSize = elementIntArray.length;
        while ((c = read()) != -1) {
            if (c == (int) elementIntArray[index]) {
                index++;
            } else {
                index = 0;
            }
            if (index == elementSize) {
                return true;
            }
        }
        return false;
    }

    public final byte[] readToLeftElement(byte[] elementIntArray) throws IOException {
        try {
            int elementSize = elementIntArray.length;
            int index = 0;
            byte[] tmp = new byte[elementSize];
            int c;
            mark(elementSize);
            while ((c = read()) != -1) {
                if (c == elementIntArray[index]) {
                    tmp[index++] = (byte) c;
                } else {
                    if (index>0)
                        tmpStream.write(tmp, 0, index);
                    mark(elementSize);
                    index = 0;
                    tmpStream.write(c);
                }
                if (index == elementSize) {
                    reset();
                    return tmpStream.toByteArrayAndReset();
                }
            }
        }finally {
            tmpStream.reset();
        }
        return null;
    }

    public final byte[] readToRightElement(byte[] elementIntArray) throws IOException {
        try {
            int elementSize = elementIntArray.length;
            int index = 0;
            byte[] tmp = new byte[elementSize];
            int c;
            while ((c = read()) != -1) {
                if (c == elementIntArray[index]) {
                    tmp[index++] = (byte) c;
                } else {
                    if (index>0)
                        tmpStream.write(tmp, 0, index);
                    index = 0;
                    tmpStream.write(c);
                }
                if (index == elementSize) {
                    tmpStream.write(tmp);
                    return tmpStream.toByteArrayAndReset();
                }
            }
        }finally {
            tmpStream.reset();
        }
        return null;

    }

    @Override
    public final void close() throws IOException {
        inputStream.close();
    }

    protected final void reset() {
        offset = offset + (markPosition - bufferOffset);
        bufferOffset = markPosition;
        markSize = 0;
        markPosition = 0;
    }

    protected final byte read() throws IOException {
        byte b = read_();
        if (currentByteListener!=null)
            currentByteListener.handle(b);
        return b;
    }
    protected void mark(int markSize) {
        this.markSize = markSize;
        this.markPosition = bufferOffset;
    }
    private final byte read_() throws IOException{

        if (isStreamTerminated)
            return -1;
        if (this.inputStream == null){
            readed = buffer.length;
        }else
        if ( bufferOffset >= maxRead || readed==0 ){

            if (markSize>0 && readed>0){
                offset = offset + markSize;
                bufferOffset = markSize;
                for (int i=0;i<markSize;i++)
                    buffer[i] = buffer[maxRead-markSize+i];
                markPosition = markSize -( maxRead - markPosition );
                if (markPosition<0)
                    markPosition = 0;
            }else{
                bufferOffset = 0;
            }

            readed = -1;
            if (inputStream!=null)
                readed = inputStream.read( buffer, bufferOffset,maxRead - bufferOffset)+bufferOffset;

            if ( readed == -1 ){
                isStreamTerminated = true;
                return -1;
            }
        }

        if( (readed<buffer.length || inputStream==null) && bufferOffset >=readed ) {
            isStreamTerminated = true;
            return -1;
        }
        byte result = buffer[bufferOffset];
        offset++;
        bufferOffset++;

        return result;
    }

}
