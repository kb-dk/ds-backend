/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.util.reader;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper for converting an {@link OutputStream}-using job to a background task which exposes
 * a buffered {@link InputStream} containing the output.
 */
public class ThreadedPiper {

    private static AtomicLong threads = new AtomicLong(0);
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 50, 1000, TimeUnit.DAYS,
            new ArrayBlockingQueue<Runnable>(100),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ThreadedPiper_" + threads.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
    );

    /**
     * @param producer adds content to the returned InputStream.
     * @return a stream coupled to the producer.
     * @throws IOException if the producer failed to deliver.
     */
    public static InputStream getDeferredStream(final Producer producer) throws IOException {
        final PipedOutputStream source = new PipedOutputStream();
        final PipedInputStream sink = new PipedInputStream(source);
        final SignallingInputStream signal = new SignallingInputStream(sink);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    producer.process(source);
                    source.flush();
                    source.close();
                } catch (IOException e) {
                    signal.raiseException(e);
                }
            }
        });
        return signal;
    }

    public interface Producer {
        /**
         * This will be called in its own Thread. It is the responsibility of the implementer to close out after use.
         * Note that there are no checked Exceptions as execution is Threaded. Any problems should be handled by
         * the implementation.
         * @param out the result of processing must be written to this stream.
         * @throws IOException will be catched by the thread and passed on to the next read from the InputStream
         *                     connected to the OutputStream.
         */
        void process(OutputStream out) throws IOException;
    }

    public static class SignallingInputStream extends InputStream {
        private final InputStream source;
        private IOException exception = null;

        public SignallingInputStream(InputStream source) {
            this.source = source;
        }

        /**
         * Assigns the given exception to the InputStream.
         * On the next read from the stream, the exception will be thrown.
         * @param exception the exception to raise on next read.
         */
        public void raiseException(IOException exception) {
            this.exception = exception;
        }

        @Override
        public int read() throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            return source.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            return source.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            return source.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            return source.skip(n);
        }

        @Override
        public int available() throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            return source.available();
        }

        @Override
        public void close() throws IOException {
            source.close();
        }

        @Override
        public void mark(int readlimit) {
            source.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            if (exception != null) {
                throw new IOException("Exception from connected OutputStream", exception);
            }
            source.reset();
        }

        @Override
        public boolean markSupported() {
            return source.markSupported();
        }
    }
}
