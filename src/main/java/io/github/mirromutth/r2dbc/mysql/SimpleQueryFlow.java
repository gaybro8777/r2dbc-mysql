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

package io.github.mirromutth.r2dbc.mysql;

import io.github.mirromutth.r2dbc.mysql.client.Client;
import io.github.mirromutth.r2dbc.mysql.message.client.SimpleQueryMessage;
import io.github.mirromutth.r2dbc.mysql.message.server.ErrorMessage;
import io.github.mirromutth.r2dbc.mysql.message.server.OkMessage;
import io.github.mirromutth.r2dbc.mysql.message.server.CompleteMessage;
import io.github.mirromutth.r2dbc.mysql.message.server.ServerMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Predicate;

/**
 * Simple (direct) query message flow for {@link MySqlBatch} and {@link SimpleMySqlStatement}.
 */
final class SimpleQueryFlow {

    // Metadata EOF message will be not receive in here.
    static final Predicate<ServerMessage> RESULT_DONE = message -> message instanceof CompleteMessage;

    private static final Predicate<ServerMessage> EXECUTE_DONE = message ->
        message instanceof ErrorMessage || (message instanceof CompleteMessage && ((CompleteMessage) message).isDone());

    /**
     * Execute multi-query with one-by-one. Query execution terminates with a
     * {@link ErrorMessage} and send Exception to signal.
     *
     * @param client     the {@link Client} to exchange messages with.
     * @param statements bundled sql for execute.
     * @return the messages received in response to this exchange, and will be
     * completed by {@link OkMessage} for each statement.
     */
    static Flux<ServerMessage> execute(Client client, List<String> statements) {
        return Flux.defer(() -> {
            int size = statements.size();

            switch (size) {
                case 0:
                    return Flux.empty();
                case 1:
                    return execute(client, statements.get(0));
                default:
                    return Flux.fromIterable(statements).concatMap(sql -> execute(client, sql));
            }
        });
    }

    /**
     * Execute a simple query. Query execution terminates with a {@link ErrorMessage}
     * and send Exception to signal.
     *
     * @param client the {@link Client} to exchange messages with.
     * @param sql    the query to execute, must contain only one statement.
     * @return the messages received in response to this exchange, and will be
     * completed by {@link CompleteMessage} when it is last result.
     */
    static Flux<ServerMessage> execute(Client client, String sql) {
        return client.exchange(new SimpleQueryMessage(sql), EXECUTE_DONE).handle((message, sink) -> {
            if (message instanceof ErrorMessage) {
                sink.error(ExceptionFactory.createException((ErrorMessage) message, sql));
            } else {
                sink.next(message);
            }
        });
    }

    private SimpleQueryFlow() {
    }
}
