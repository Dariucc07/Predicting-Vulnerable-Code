/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.amqp.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Abstract base class for code based configuration of Spring managed AMQP infrastructure,
 * i.e. Exchanges, Queues, and Bindings.
 * <p>Subclasses are required to provide an implementation of AmqpAdmin and randomNameQueueDefinition.
 * <p>There are several convenience 'declare' methods to make the configuration in subclasses
 * more terse and readable.
 * <p>The BindingBuilder class can be used to provide a fluent API to declare bindings.
 * 
 * @author Mark Pollack
 * @see AbstractExhange
 * @see Queue
 * @see Binding
 * @see BindingBuilder
 */
@Configuration
public abstract class AbstractAmqpConfiguration {

	@Bean
	public abstract AmqpAdmin amqpAdmin();

	@Bean
	public abstract Queue generatedQueue();


	/**
	 * Provides convenient access to the default exchange which is always declared on the broker.
	 */
	public DirectExchange defaultExchange() {
		return new DirectExchange("");
	}

}
