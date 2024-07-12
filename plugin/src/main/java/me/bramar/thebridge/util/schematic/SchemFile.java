package me.bramar.thebridge.util.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

class SchemFile implements Serializable {
    private static final long serialVersionUID = 1L;
        String w; // world
        int ay, by; // minY, maxY
        Set<SchemChunk> v = Collections.synchronizedSet(new HashSet<>());

        public void serialize1(StringBuilder builder) {
            builder.append("BSchematic@1@");
            builder.append(ay).append(",").append(by).append("@");
            builder.append("[");
            int i = 0;
            synchronized(v) {
                for(SchemChunk schemChunk : v) {
                    schemChunk.serialize1(builder);
                    if(i != v.size() - 1)
                        builder.append("C");
                    i++;
                }
            }
            builder.append("]");
        }
        static Integer readNumber(InputStream stream, char firstCharBlacklist, char endingChar, int errorCode) throws IOException {
            int b;
            int n = 0;
            boolean a = false;
            boolean negative = false;
            boolean firstIndex = true;
            while((b = stream.read()) != -1) {
                char c = (char) b;
                if(firstIndex) {
                    firstIndex = false;
                    if(c == firstCharBlacklist)
                        return null;
                    if(c == '-') {
                        negative = true;
                        continue;
                    }
                }
                if(c == endingChar) {
                    if(a)
                        break;
                    else
                        throw new IllegalArgumentException("Invalid schematic: error 0x001 at [" + errorCode + "]");
                }
                if(!Character.isDigit(b))
                    throw new IllegalArgumentException("Invalid schematic: error 0x010 at [" + errorCode + "]");
                n *= 10;
                n += Integer.parseInt("" + c);
                a = true;
            }
            return negative ? -n : n;
        }
        static int readNumber(InputStream stream, Predicate<Character> endingChar, int errorCode) throws IOException {
            int b;
            int n = 0;
            boolean a = false;
            boolean negative = false;
            boolean first = true;
            while((b = stream.read()) != -1) {
                char c = (char) b;
                if(first) {
                    first = false;
                    if(c == '-') {
                        negative = true;
                        continue;
                    }
                }
                if(endingChar.test(c)) {
                    if(a)
                        break;
                    else
                        throw new IllegalArgumentException("Invalid schematic: error 0x001 at [" + errorCode + "]");
                }
                if(!Character.isDigit(b))
                    throw new IllegalArgumentException("Invalid schematic: error 0x010 at [" + errorCode + "]");
                n *= 10;
                n += Integer.parseInt("" + c);
                a = true;
            }
            return negative ? -n : n;
        }
        static int readNumber(InputStream stream, char endingChar, int errorCode) throws IOException {
            int b;
            int n = 0;
            boolean a = false;
            boolean negative = false;
            boolean first = true;
            while((b = stream.read()) != -1) {
                char c = (char) b;
                if(first) {
                    first = false;
                    if(c == '-') {
                        negative = true;
                        continue;
                    }
                }
                if(c == endingChar) {
                    if(a)
                        break;
                    else
                        throw new IllegalArgumentException("Invalid schematic: error 0x001 at [" + errorCode + "]");
                }
                if(!Character.isDigit(b))
                    throw new IllegalArgumentException("Invalid schematic: error 0x010 at [" + errorCode + "]");
                n *= 10;
                n += Integer.parseInt("" + c);
                a = true;
            }
            return negative ? -n : n;
        }
        static SchemFile deserialize1(InputStream stream) throws IOException {
            StringBuilder buffer = new StringBuilder();
            int b;
            while((b = stream.read()) != -1 && b != '@') {
                buffer.appendCodePoint(b);
            }
            if(b == -1)
                throw new IllegalArgumentException("Invalid schematic: error 0x01");
            if(!buffer.toString().equalsIgnoreCase("BSchematic"))
                throw new IllegalArgumentException("Invalid schematic: error 0x10");
            char version = (char) stream.read();
            if(version != '1')
                throw new IllegalArgumentException("Invalid schematic: error 0x11, wrong version");
            if(stream.read() != '@')
                throw new IllegalArgumentException("Invalid schematic: error 0x100 at [1]");
            int minY = readNumber(stream, ',', 1);
            int maxY = readNumber(stream, '@', 2);

            if(stream.read() != '[')
                throw new IllegalArgumentException("Invalid schematic: error 0x110 at [1]");
            AtomicBoolean end = new AtomicBoolean(false);
            SchemFile schemFile = new SchemFile();
            schemFile.ay = minY;
            schemFile.by = maxY;
            while(!end.get()) {
                schemFile.v.add(SchemChunk.deserialize1(stream, end));
            }
            return schemFile;
        }
    }