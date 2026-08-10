package dk.kb.kaltura.fileHandling;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChunkedFileReader implements Iterator<ByteArrayInputStream>, Closeable {

    private final RandomAccessFile file;
    private final long chunkSize;
    private final long fileLength;
    private long bytesReadSoFar = 0;

    public ChunkedFileReader(File videoFile, long chunkSizeBytes) throws IOException {
        this.file = new RandomAccessFile(videoFile, "r");
        this.chunkSize = chunkSizeBytes;
        this.fileLength = file.length();
    }

    @Override
    public boolean hasNext() {
        return bytesReadSoFar < fileLength;
    }

    @Override
    public ByteArrayInputStream next() {
        if (!hasNext()) throw new NoSuchElementException("No more chunks");
        try {
            long remaining = fileLength - bytesReadSoFar;
            long thisChunkSize = Math.min(chunkSize, remaining);

            byte[] buffer = new byte[(int) thisChunkSize];
            file.readFully(buffer);          // advances the file pointer for us
            bytesReadSoFar += thisChunkSize;
            return new ByteArrayInputStream(buffer); // self-bounded — no extra logic needed
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public long getFileLength() {
        return fileLength;
    }

    public long getBytesReadSoFar() {
        return bytesReadSoFar;
    }
}