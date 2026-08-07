package dk.kb.kaltura.domain;

import java.io.ByteArrayInputStream;

public class ChunkInputStream extends ByteArrayInputStream {
    final long chunkSize;

    public ChunkInputStream(long chunkSize, byte[] buf) {
        super(buf);
        this.chunkSize = chunkSize;
    }

    public long getChunkSize() {
        return chunkSize;
    }
}
