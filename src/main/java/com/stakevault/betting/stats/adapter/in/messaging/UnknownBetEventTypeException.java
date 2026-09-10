package com.stakevault.betting.stats.adapter.in.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

// Mesmo racional de InvalidBetEventException - um eventType sem schema registrado nunca
// vai ficar valido numa proxima tentativa.
class UnknownBetEventTypeException extends AmqpRejectAndDontRequeueException {

	UnknownBetEventTypeException(String eventType) {
		super("no schema registered for eventType: " + eventType);
	}
}
