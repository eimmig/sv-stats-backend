package com.stakevault.betting.stats.adapter.in.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class UnknownBetEventTypeException extends AmqpRejectAndDontRequeueException {

	UnknownBetEventTypeException(String eventType) {
		super("no schema registered for eventType: " + eventType);
	}
}
