package com.zyozmen.products.adapter.out.persistence;

import com.zyozmen.products.domain.exception.ServiceUnavailableException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import com.zyozmen.products.adapter.out.persistence.mapper.ProductoPersistenceMapper;
import com.zyozmen.products.adapter.out.persistence.repository.ProductoJpaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida (Outbound Adapter / Driven Adapter).
 *
 * Implementa el puerto de salida definido en el dominio usando
 * Spring Data JPA. El dominio nunca ve esta clase: solo conoce la
 * interfaz ProductoRepositoryPort.
 * Si se cambia MySQL por MongoDB, solo se reemplaza este adaptador;
 * el dominio y la aplicación no se modifican.
 *
 * Resilience4j:
 * - @Retry   → reintenta hasta 3 veces en errores transitorios de BD (timeout, conexión).
 * - @CircuitBreaker → abre el circuito al 50% de fallos en ventana de 10 llamadas;
 *                     protege la BD de recibir solicitudes mientras no está disponible.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductoRepositoryAdapter implements ProductoRepositoryPort {

    private static final String CB_NAME = "productoRepository";

    private final ProductoJpaRepository jpaRepository;
    private final ProductoPersistenceMapper mapper;

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findAllFallback")
    public List<Producto> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    public List<Producto> findAllFallback(Throwable t) {
        log.warn("[Circuit Breaker] findAll no disponible. Causa: {}", t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByIdFallback")
    public Optional<Producto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    public Optional<Producto> findByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker] findById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "saveFallback")
    public Producto save(Producto producto) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(producto)));
    }

    public Producto saveFallback(Producto producto, Throwable t) {
        log.warn("[Circuit Breaker] save no disponible. Causa: {}", t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "existsByIdFallback")
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    public boolean existsByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker] existsById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "deleteByIdFallback")
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    public void deleteByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker] deleteById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }
}

