package me.bramar.thebridge.util.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

class SchemBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        int z; // coordinate
        int a; // a = material
        int b; // b = data of material
        void serialize1(StringBuilder builder) {
            builder.append(z).append("^").append(a).append(":").append(b);
        }
        static SchemBlock deserialize1(InputStream stream, AtomicBoolean end) throws IOException {
            Integer z = SchemFile.readNumber(stream, '>', '^', 7);
            if(z == null) {
                end.set(true);
                return null;
            }
            int a = SchemFile.readNumber(stream, ':', 8);
            int b = SchemFile.readNumber(stream, ch -> {
                if(ch == '>')
                    end.set(true);
                return ch == '>' || ch == 'B';
            }, 9);
            SchemBlock schemBlock = new SchemBlock();
            schemBlock.z = z;
            schemBlock.a = a;
            schemBlock.b = b;
            return schemBlock;
        }
    }