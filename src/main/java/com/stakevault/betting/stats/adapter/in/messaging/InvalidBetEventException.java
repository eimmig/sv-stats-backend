package com.stakevault.betting.stats.adapter.in.messaging;

import java.util.Set;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.networknt.schema.ValidationMessage;

// Extends AmqpRejectAndDontRequeueException porque uma mensagem que nao bate com o schema
// nunca vai passar a bater numa proxima tentativa - retentar (x-delivery-limit) so faz
// sentido pra falha transitoria. Rejeita direto pra DLQ (bets.events.dlx) sem gastar as
// tentativas de redelivery, ver docs/API-CONTRACTS.md "Topologia RabbitMQ".
class InvalidBetEventException extends AmqpRejectAndDontRequeueException {

	InvalidBetEventException(String eventType, Set<ValidationMessage> errors) {
		super("invalid " + eventType + " event, schema validation errors: " + errors);
	}
}
