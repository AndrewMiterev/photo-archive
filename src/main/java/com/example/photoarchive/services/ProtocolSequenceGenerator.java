package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Protocol;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class ProtocolSequenceGenerator extends AbstractMongoEventListener<Protocol> {
    private final MongoIdGenerator generator;

    public ProtocolSequenceGenerator(MongoIdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Protocol> event) {
        if (event.getSource().getId() < 1)
            event.getSource().setId(generator.generateSequence(Protocol.SEQUENCE_NAME));
        super.onBeforeConvert(event);
    }
}
