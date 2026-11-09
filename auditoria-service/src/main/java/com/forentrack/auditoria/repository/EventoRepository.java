package com.forentrack.auditoria.repository;

import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.auditoria.model.EventoAuditoria;

public interface EventoRepository extends MongoRepository<EventoAuditoria, String> {

    long countByResultado(String resultado);

    long countByRevisadoFalseAndResultadoIn(Collection<String> resultados);
}
