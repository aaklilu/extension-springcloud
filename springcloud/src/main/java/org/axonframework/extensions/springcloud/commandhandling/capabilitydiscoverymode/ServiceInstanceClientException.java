/*
 * Copyright (c) 2010-2020. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.extensions.springcloud.commandhandling.capabilitydiscoverymode;

import org.axonframework.common.AxonException;

/**
 * Exception defining a {@link org.springframework.cloud.client.ServiceInstance} threw a client exception when an
 * attempt was made to make an HTTP connection.
 *
 * @author Steven van Beelen
 * @since 4.4
 */
public class ServiceInstanceClientException extends AxonException {

    private static final long serialVersionUID = -2634037343400252397L;

    /**
     * Initializes a {@link ServiceInstanceClientException} using the given {@code message}.
     *
     * @param message the message describing the exception
     */
    public ServiceInstanceClientException(String message) {
        super(message);
    }

    /**
     * Initializes a {@link ServiceInstanceClientException} using the given {@code message} and {@code cause}.
     *
     * @param message the message describing the exception
     * @param cause   the underlying cause of the exception
     */
    public ServiceInstanceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
