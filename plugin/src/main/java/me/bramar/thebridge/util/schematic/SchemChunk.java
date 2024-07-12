package me.bramar.thebridge.util.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class SchemChunk implements Serializable {
        private static final long serialVersionUID = 1L;
        int x, z; // schem coords
        final Set<SchemY> v = new HashSet<>();

        void serialize1(StringBuilder builder) {
            builder.append(x).append(".").append(z).append("#");
            builder.append("(");
            int i = 0;
            synchronized(v) {
                for(SchemY schemY : v) {
                    schemY.serialize1(builder);
                    if(i != v.size() - 1)
                        builder.append("Y");
                    i++;
                }
            }
            builder.append(")");
        }
        static SchemChunk deserialize1(InputStream stream, AtomicBoolean end) throws IOException {
            int x = SchemFile.readNumber(stream, '.', 3);
            int z = SchemFile.readNumber(stream, '#', 4);
            if(stream.read() != '(')
                throw new IllegalArgumentException("Invalid schematic: error 0x110 at [2]");
            SchemChunk schemChunk = new SchemChunk();
            schemChunk.x = x;
            schemChunk.z = z;
            AtomicBoolean end0 = new AtomicBoolean(false);
            while(!end0.get()) {
                schemChunk.v.add(SchemY.deserialize1(stream, end0));
            }
            end.set(stream.read() == ']');
            return schemChunk;
        }
    }