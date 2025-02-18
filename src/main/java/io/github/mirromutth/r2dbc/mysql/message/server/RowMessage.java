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

package io.github.mirromutth.r2dbc.mysql.message.server;

import io.github.mirromutth.r2dbc.mysql.codec.FieldInformation;
import io.github.mirromutth.r2dbc.mysql.constant.DataTypes;
import io.github.mirromutth.r2dbc.mysql.constant.DataValues;
import io.github.mirromutth.r2dbc.mysql.message.FieldValue;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

import static io.github.mirromutth.r2dbc.mysql.internal.AssertUtils.requireNonNull;

/**
 * A message includes data fields which is a row of result.
 */
public final class RowMessage implements ReferenceCounted, ServerMessage {

    private static final byte BIT_MASK_INIT = 1 << 2;

    private final FieldReader reader;

    RowMessage(FieldReader reader) {
        this.reader = requireNonNull(reader, "reader must not be null");
    }

    public final FieldValue[] decode(boolean isBinary, FieldInformation[] context) {
        if (isBinary) {
            return binary(context);
        } else {
            return text(context.length);
        }
    }

    private FieldValue[] text(int size) {
        FieldValue[] fields = new FieldValue[size];

        try {
            for (int i = 0; i < size; ++i) {
                if (DataValues.NULL_VALUE == reader.getUnsignedByte()) {
                    reader.skipOneByte();
                    fields[i] = FieldValue.nullField();
                } else {
                    fields[i] = reader.readVarIntSizedField();
                }
            }

            return fields;
        } catch (Throwable e) {
            clearFields(fields, size);
            throw e;
        }
    }

    private FieldValue[] binary(FieldInformation[] context) {
        reader.skipOneByte(); // constant 0x00

        int size = context.length;
        // MySQL will make sure columns less than 4096, no need check overflow.
        byte[] nullBitmap = reader.readSizeFixedBytes((size + 9) >> 3);
        int bitmapIndex = 0;
        byte bitMask = BIT_MASK_INIT;
        FieldValue[] fields = new FieldValue[size];

        try {
            for (int i = 0; i < size; ++i) {
                if ((nullBitmap[bitmapIndex] & bitMask) != 0) {
                    fields[i] = FieldValue.nullField();
                } else {
                    int bytes = getFixedBinaryBytes(context[i].getType());
                    if (bytes > 0) {
                        fields[i] = reader.readSizeFixedField(bytes);
                    } else {
                        fields[i] = reader.readVarIntSizedField();
                    }
                }

                bitMask <<= 1;

                // Should make sure it is 0 of byte, it may change to int in future, so try not use `bitMask == 0` only.
                if ((bitMask & 0xFF) == 0) {
                    // An approach to circular left shift 1-bit.
                    bitMask = 1;
                    // Current byte has been completed by read.
                    ++bitmapIndex;
                }
            }

            return fields;
        } catch (Throwable e) {
            clearFields(fields, size);
            throw e;
        }
    }

    @Override
    public int refCnt() {
        return reader.refCnt();
    }

    @Override
    public RowMessage retain() {
        reader.retain();
        return this;
    }

    @Override
    public RowMessage retain(int increment) {
        reader.retain(increment);
        return this;
    }

    @Override
    public RowMessage touch() {
        reader.touch();
        return this;
    }

    @Override
    public final RowMessage touch(Object o) {
        reader.touch(o);
        return this;
    }

    @Override
    public boolean release() {
        return reader.release();
    }

    @Override
    public boolean release(int decrement) {
        return reader.release(decrement);
    }

    /**
     * @return {@literal 0} means field is var integer sized in binary result.
     */
    private static int getFixedBinaryBytes(short type) {
        switch (type) {
            case DataTypes.TINYINT:
                return Byte.BYTES;
            case DataTypes.SMALLINT:
            case DataTypes.YEAR:
                return Short.BYTES;
            case DataTypes.INT:
            case DataTypes.MEDIUMINT:
                return Integer.BYTES;
            case DataTypes.FLOAT:
                return Float.BYTES;
            case DataTypes.DOUBLE:
                return Double.BYTES;
            case DataTypes.BIGINT:
                return Long.BYTES;
            default:
                return 0;
        }
    }

    private static void clearFields(FieldValue[] fields, int size) {
        FieldValue field;

        for (int i = 0; i < size; ++i) {
            field = fields[i];
            if (field != null && !field.isNull()) {
                ReferenceCountUtil.safeRelease(field);
            }
        }
    }
}
