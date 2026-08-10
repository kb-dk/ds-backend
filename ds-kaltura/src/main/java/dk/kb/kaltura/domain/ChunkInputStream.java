package dk.kb.kaltura.domain;

import java.io.ByteArrayInputStream;

public class ChunkInputStream extends ByteArrayInputStream {
    private final long chunkSize;

    public ChunkInputStream(byte[] buf) {
        super(buf);
        this.chunkSize = buf.length;
    }

    public long getChunkSize() {
        return chunkSize;
    }
}
