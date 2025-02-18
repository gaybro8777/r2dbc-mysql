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

/**
 * Decode context with static creators.
 */
public interface DecodeContext {

    static DecodeContext connection() {
        return ConnectionDecodeContext.INSTANCE;
    }

    static DecodeContext command() {
        return CommandDecodeContext.INSTANCE;
    }

    static DecodeContext result(boolean deprecateEof, int totalColumns) {
        return new ResultDecodeContext(deprecateEof, totalColumns);
    }

    static DecodeContext preparedMetadata(boolean deprecateEof, int totalColumns, int totalParameters) {
        return new PreparedMetadataDecodeContext(deprecateEof, totalColumns, totalParameters);
    }
}
