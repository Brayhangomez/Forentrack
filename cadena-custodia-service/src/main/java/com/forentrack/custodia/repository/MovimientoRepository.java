package com.forentrack.custodia.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.custodia.model.Movimiento;
import com.forentrack.custodia.model.TipoMovimiento;

public interface MovimientoRepository extends MongoRepository<Movimiento, String> {

    List<Movimiento> findByEvidenciaIdOrderByFechaAsc(String evidenciaId);

    boolean existsByEvidenciaIdAndTipo(String evidenciaId, TipoMovimiento tipo);

    Optional<Movimiento> findFirstByEvidenciaIdAndTipoInOrderByFechaDesc(String evidenciaId,
                                                                          Collection<TipoMovimiento> tipos);

    List<Movimiento> findByTipoOrderByFechaDesc(TipoMovimiento tipo, Pageable pagina);

    List<Movimiento> findAllByOrderByFechaDesc(Pageable pagina);
}
