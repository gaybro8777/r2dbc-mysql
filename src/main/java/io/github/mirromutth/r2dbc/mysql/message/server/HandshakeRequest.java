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

import io.netty.buffer.ByteBuf;
import io.r2dbc.spi.R2dbcPermissionDeniedException;

/**
 * A MySQL Handshake Request message for multi-versions.
 */
public interface HandshakeRequest extends ServerMessage {

    HandshakeHeader getHeader();

    int getServerCapabilities();

    String getAuthType();

    byte[] getSalt();

    static HandshakeRequest decode(ByteBuf buf) {
        HandshakeHeader header = HandshakeHeader.decode(buf);
        int version = header.getProtocolVersion();

        switch (version) {
            case 10:
                return HandshakeV10Request.decodeV10(buf, header);
            case 9:
                return HandshakeV9Request.decodeV9(buf, header);
            default:
                throw new R2dbcPermissionDeniedException(String.format("Handshake protocol version %d not support.", version));
        }
    }
}
