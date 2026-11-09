package com.forentrack.custodia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.custodia.model.ValidacionCadena;

public interface ValidacionRepository extends MongoRepository<ValidacionCadena, String> {

    List<ValidacionCadena> findByEvidenciaIdOrderByFechaDesc(String evidenciaId);

    Optional<ValidacionCadena> findFirstByEvidenciaIdOrderByFechaDesc(String evidenciaId);
}
