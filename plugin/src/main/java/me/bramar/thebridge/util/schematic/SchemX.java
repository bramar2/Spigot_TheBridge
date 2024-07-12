package me.bramar.thebridge.util.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class SchemX implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        final Set<SchemBlock> v = new HashSet<>();

        void serialize1(StringBuilder builder) {
            builder.append(x).append("%");
            builder.append("<");
            int i = 0;
            synchronized(v) {
                for(SchemBlock schemBlock : v) {
                    schemBlock.serialize1(builder);
                    if(i != v.size() - 1)
                        builder.append("B");
                    i++;
                }
            }
            builder.append(">");
        }
        static SchemX deserialize1(InputStream stream, AtomicBoolean end) throws IOException {
            int x = SchemFile.readNumber(stream, '%', 6);
            if(stream.read() != '<')
                throw new IllegalArgumentException("Invalid schematic: error 0x110 at [4]");
            SchemX schemX = new SchemX();
            schemX.x = x;
            AtomicBoolean end0 = new AtomicBoolean(false);
            while(!end0.get()) {
                SchemBlock schemBlock = SchemBlock.deserialize1(stream, end0);
                if(schemBlock != null)
                    schemX.v.add(schemBlock);
            }
            end.set(stream.read() == '}');
            return schemX;
        }
    }