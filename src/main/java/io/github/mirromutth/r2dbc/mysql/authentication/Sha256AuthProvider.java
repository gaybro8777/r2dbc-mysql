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

package io.github.mirromutth.r2dbc.mysql.authentication;

import io.github.mirromutth.r2dbc.mysql.collation.CharCollation;
import reactor.util.annotation.Nullable;

import java.nio.CharBuffer;

import static io.github.mirromutth.r2dbc.mysql.constant.AuthTypes.SHA256_PASSWORD;
import static io.github.mirromutth.r2dbc.mysql.constant.DataValues.TERMINAL;
import static io.github.mirromutth.r2dbc.mysql.internal.AssertUtils.requireNonNull;

/**
 * An implementation of {@link MySqlAuthProvider} for type "sha256_password".
 */
final class Sha256AuthProvider implements MySqlAuthProvider {

    static final Sha256AuthProvider INSTANCE = new Sha256AuthProvider();

    private Sha256AuthProvider() {
    }

    @Override
    public boolean isSslNecessary() {
        return true;
    }

    @Override
    public byte[] authentication(@Nullable CharSequence password, @Nullable byte[] salt, CharCollation collation) {
        if (password == null || password.length() <= 0) {
            return new byte[]{TERMINAL};
        }

        requireNonNull(collation, "collation must not be null when password exists");

        return AuthHelper.encodeTerminal(CharBuffer.wrap(password), collation.getCharset());
    }

    @Override
    public MySqlAuthProvider next() {
        return this;
    }

    @Override
    public String getType() {
        return SHA256_PASSWORD;
    }
}
