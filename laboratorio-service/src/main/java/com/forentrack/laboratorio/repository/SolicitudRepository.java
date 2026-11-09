package com.forentrack.laboratorio.repository;

import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.laboratorio.model.EstadoSolicitud;
import com.forentrack.laboratorio.model.SolicitudAnalisis;

public interface SolicitudRepository extends MongoRepository<SolicitudAnalisis, String> {

    boolean existsByEvidenciaIdAndEstadoIn(String evidenciaId, Collection<EstadoSolicitud> estados);
}
