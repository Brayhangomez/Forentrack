package com.forentrack.evidencias.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.evidencias.model.Evidencia;

public interface EvidenciaRepository extends MongoRepository<Evidencia, String> {
}
