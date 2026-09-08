package com.stakevault.betting.stats;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Topologia real de producao (infra/rabbitmq/definitions.json, ver docs/API-CONTRACTS.md
	// "Topologia RabbitMQ") - o listener escuta a fila real "stats.bet-events", entao o teste
	// precisa dela existir de verdade, diferente do lado publicador de bets-service (que so
	// precisa de uma fila propria pra inspecionar a mensagem publicada).
	private static final String EXCHANGE = "bets.events";
	private static final String DLX = "bets.events.dlx";
	private static final String QUEUE = "stats.bet-events";
	private static final String DLQ = "stats.bet-events.dlq";

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	@ServiceConnection
	RabbitMQContainer rabbitMQContainer() {
		return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
	}

	@Bean
	@ServiceConnection
	RedisContainer redisContainer() {
		return new RedisContainer(DockerImageName.parse("redis:7-alpine"));
	}

	@Bean
	TopicExchange betsEventsExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	FanoutExchange betsEventsDlx() {
		return new FanoutExchange(DLX, true, false);
	}

	@Bean
	Queue statsBetEventsQueue() {
		return QueueBuilder.durable(QUEUE)
				.quorum()
				.deadLetterExchange(DLX)
				.withArgument("x-delivery-limit", 3)
				.build();
	}

	@Bean
	Queue statsBetEventsDlq() {
		return QueueBuilder.durable(DLQ).quorum().build();
	}

	@Bean
	Binding bindBetCreated() {
		return BindingBuilder.bind(statsBetEventsQueue()).to(betsEventsExchange()).with("bet.created");
	}

	@Bean
	Binding bindBetSettled() {
		return BindingBuilder.bind(statsBetEventsQueue()).to(betsEventsExchange()).with("bet.settled");
	}

	@Bean
	Binding bindDlq() {
		return BindingBuilder.bind(statsBetEventsDlq()).to(betsEventsDlx());
	}
}
