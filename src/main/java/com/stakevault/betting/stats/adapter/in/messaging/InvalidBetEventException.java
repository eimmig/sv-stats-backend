package com.stakevault.betting.stats.adapter.in.messaging;

import java.util.Set;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.networknt.schema.ValidationMessage;

class InvalidBetEventException extends AmqpRejectAndDontRequeueException {

	InvalidBetEventException(String eventType, Set<ValidationMessage> errors) {
		super("invalid " + eventType + " event, schema validation errors: " + errors);
	}
}
