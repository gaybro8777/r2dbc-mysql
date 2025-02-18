/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mirromutth.r2dbc.mysql.codec;

import io.github.mirromutth.r2dbc.mysql.constant.ColumnDefinitions;
import io.github.mirromutth.r2dbc.mysql.constant.DataTypes;
import io.github.mirromutth.r2dbc.mysql.internal.ConnectionContext;
import io.github.mirromutth.r2dbc.mysql.message.NormalFieldValue;
import io.github.mirromutth.r2dbc.mysql.message.ParameterValue;
import io.github.mirromutth.r2dbc.mysql.message.client.ParameterWriter;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;

/**
 * Codec for {@link int}.
 */
final class IntegerCodec extends AbstractPrimitiveCodec<Integer> {

    static final IntegerCodec INSTANCE = new IntegerCodec();

    private IntegerCodec() {
        super(Integer.TYPE, Integer.class);
    }

    @Override
    public Integer decode(NormalFieldValue value, FieldInformation info, Class<? super Integer> target, boolean binary, ConnectionContext context) {
        ByteBuf buf = value.getBufferSlice();

        if (binary) {
            boolean isUnsigned = (info.getDefinitions() & ColumnDefinitions.UNSIGNED) != 0;
            return decodeBinary(buf, info.getType(), isUnsigned);
        } else {
            return parse(buf);
        }
    }

    @Override
    public boolean canEncode(Object value) {
        return value instanceof Integer;
    }

    @Override
    public ParameterValue encode(Object value, ConnectionContext context) {
        int v = (Integer) value;

        if ((byte) v == v) {
            return new ByteCodec.ByteValue((byte) v);
        }

        if ((short) v == v) {
            return new ShortCodec.ShortValue((short) v);
        }

        return new IntValue(v);
    }

    @Override
    protected boolean doCanDecode(FieldInformation info) {
        short type = info.getType();
        return isLowerInt(type) || (DataTypes.INT == type && (info.getDefinitions() & ColumnDefinitions.UNSIGNED) == 0);
    }

    /**
     * Fast parse a negotiable integer from {@link ByteBuf} without copy.
     *
     * @param buf a {@link ByteBuf} include an integer that maybe has sign.
     * @return an integer from {@code buf}.
     */
    static int parse(ByteBuf buf) {
        byte first = buf.readByte();
        boolean isNegative;
        int value;

        if (first == '-') {
            isNegative = true;
            value = 0;
        } else if (first >= '0' && first <= '9') {
            isNegative = false;
            value = first - '0';
        } else {
            // Must be '+'.
            isNegative = false;
            value = 0;
        }

        while (buf.isReadable()) {
            value = value * 10 + (buf.readByte() - '0');
        }

        return isNegative ? -value : value;
    }

    private static boolean isLowerInt(short type) {
        return DataTypes.TINYINT == type ||
            DataTypes.YEAR == type ||
            DataTypes.SMALLINT == type ||
            DataTypes.MEDIUMINT == type;
    }

    private static int decodeBinary(ByteBuf buf, short type, boolean isUnsigned) {
        switch (type) {
            case DataTypes.INT: // Already check overflow in `doCanDecode`
            case DataTypes.MEDIUMINT:
                return buf.readIntLE();
            case DataTypes.SMALLINT:
                if (isUnsigned) {
                    return buf.readUnsignedShortLE();
                } else {
                    return buf.readShortLE();
                }
            case DataTypes.YEAR:
                return buf.readShortLE();
            default: // TINYINT
                if (isUnsigned) {
                    return buf.readUnsignedByte();
                } else {
                    return buf.readByte();
                }
        }
    }

    static final class IntValue extends AbstractParameterValue {

        private final int value;

        IntValue(int value) {
            this.value = value;
        }

        @Override
        public Mono<Void> writeTo(ParameterWriter writer) {
            return Mono.fromRunnable(() -> writer.writeInt(value));
        }

        @Override
        public short getType() {
            return DataTypes.INT;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IntValue)) {
                return false;
            }

            IntValue intValue = (IntValue) o;

            return value == intValue.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }
}
