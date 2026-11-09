package com.forentrack.casos.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.casos.model.Caso;

public interface CasoRepository extends MongoRepository<Caso, String> {

    boolean existsByCodigo(String codigo);
}
