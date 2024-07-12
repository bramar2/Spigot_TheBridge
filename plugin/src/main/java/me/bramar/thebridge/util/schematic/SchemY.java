package me.bramar.thebridge.util.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class SchemY implements Serializable {
        private static final long serialVersionUID = 1L;
        int y;
        final Set<SchemX> v = new HashSet<>();

        void serialize1(StringBuilder builder) {
            builder.append(y).append("$");
            builder.append("{");
            int i = 0;
            synchronized(v) {
                for(SchemX schemX : v) {
                    schemX.serialize1(builder);
                    if(i != v.size() - 1)
                        builder.append("X");
                    i++;
                }
            }
            builder.append("}");
        }
        static SchemY deserialize1(InputStream stream, AtomicBoolean end) throws IOException {
            int y = SchemFile.readNumber(stream, '$', 5);
            if(stream.read() != '{')
                throw new IllegalArgumentException("Invalid schematic: error 0x110 at [3]");
            SchemY schemY = new SchemY();
            schemY.y = y;
            AtomicBoolean end0 = new AtomicBoolean(false);
            while(!end0.get()) {
                schemY.v.add(SchemX.deserialize1(stream, end0));
            }
            end.set(stream.read() == ')');
            return schemY;
        }
    }