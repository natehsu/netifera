/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.handler.codec.serialization;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;

/**
 * An {@link ObjectInput} which is interoperable with {@link ObjectEncoder}
 * and {@link ObjectEncoderOutputStream}.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 */
public class ObjectDecoderInputStream extends InputStream implements
        ObjectInput {

    private final DataInputStream in;
    private final ClassLoader classLoader;
    private final int maxObjectSize;

    /**
     * Creates a new {@link ObjectInput}.
     *
     * @param in
     *        the {@link InputStream} where the serialized form will be
     *        read from
     */
    public ObjectDecoderInputStream(InputStream in) {
        this(in, null);
    }

    /**
     * Creates a new {@link ObjectInput}.
     *
     * @param in
     *        the {@link InputStream} where the serialized form will be
     *        read from
     * @param classLoader
     *        the {@link ClassLoader} which will load the class of the
     *        serialized object
     */
    public ObjectDecoderInputStream(InputStream in, ClassLoader classLoader) {
        this(in, classLoader, 1048576);
    }

    /**
     * Creates a new {@link ObjectInput}.
     *
     * @param in
     *        the {@link InputStream} where the serialized form will be
     *        read from
     * @param maxObjectSize
     *        the maximum byte length of the serialized object.  if the length
     *        of the received object is greater than this value,
     *        a {@link StreamCorruptedException} will be raised.
     */
    public ObjectDecoderInputStream(InputStream in, int maxObjectSize) {
        this(in, null, maxObjectSize);
    }

    /**
     * Creates a new {@link ObjectInput}.
     *
     * @param in
     *        the {@link InputStream} where the serialized form will be
     *        read from
     * @param classLoader
     *        the {@link ClassLoader} which will load the class of the
     *        serialized object
     * @param maxObjectSize
     *        the maximum byte length of the serialized object.  if the length
     *        of the received object is greater than this value,
     *        a {@link StreamCorruptedException} will be raised.
     */
    public ObjectDecoderInputStream(InputStream in, ClassLoader classLoader, int maxObjectSize) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
        }
        if (in instanceof DataInputStream) {
            this.in = (DataInputStream) in;
        } else {
            this.in = new DataInputStream(in);
        }
        this.classLoader = classLoader;
        this.maxObjectSize = maxObjectSize;
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        int dataLen = readInt();
        if (dataLen <= 0) {
            throw new StreamCorruptedException("invalid data length: " + dataLen);
        }
        if (dataLen > maxObjectSize) {
            throw new StreamCorruptedException(
                    "data length too big: " + dataLen + " (max: " + maxObjectSize + ')');
        }

        return new CompactObjectInputStream(in, classLoader).readObject();
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public final int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public final int read(byte[] b) throws IOException {
        return in.read(b);
    }

    public final boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public final byte readByte() throws IOException {
        return in.readByte();
    }

    public final char readChar() throws IOException {
        return in.readChar();
    }

    public final double readDouble() throws IOException {
        return in.readDouble();
    }

    public final float readFloat() throws IOException {
        return in.readFloat();
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    public final int readInt() throws IOException {
        return in.readInt();
    }

    @Deprecated
    public final String readLine() throws IOException {
        return in.readLine();
    }

    public final long readLong() throws IOException {
        return in.readLong();
    }

    public final short readShort() throws IOException {
        return in.readShort();
    }

    public final int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    public final int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    public final String readUTF() throws IOException {
        return in.readUTF();
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public final int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }
}
