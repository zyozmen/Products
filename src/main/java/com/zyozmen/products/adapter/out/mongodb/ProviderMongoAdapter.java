package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.ProviderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.LoteMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.ProviderMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.TransaccionProveedorMongoRepository;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Provider;
import com.zyozmen.products.domain.model.ProviderTransaction;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import com.zyozmen.products.domain.port.out.TransaccionProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ProviderMongoAdapter
        implements ProveedorRepositoryPort, LoteRepositoryPort, TransaccionProveedorRepositoryPort {

    private final ProviderMongoRepository providerRepository;
    private final LoteMongoRepository loteRepository;
    private final TransaccionProveedorMongoRepository transaccionRepository;
    private final ProviderMongoMapper mapper;

    @Override
    public Provider save(Provider proveedor) {
        return mapper.toDomain(providerRepository.save(mapper.toDocument(proveedor)));
    }

    @Override
    public Optional<Provider> findById(String id) {
        return ObjectId.isValid(id) ? providerRepository.findById(id).map(mapper::toDomain) : Optional.empty();
    }

    @Override
    public boolean existsByNit(String nit) {
        return providerRepository.existsByNit(nit);
    }

    @Override
    public Page<Provider> buscar(String texto, Pageable pageable) {
        Pageable ordenado = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by("nombre", "id"));
        var resultado = texto.isEmpty()
                ? providerRepository.findAll(ordenado)
                : providerRepository.buscarPorNombreONit(Pattern.quote(texto), ordenado);
        return resultado.map(mapper::toDomain);
    }

    @Override
    public Lote save(Lote lote) {
        return mapper.toDomain(loteRepository.save(mapper.toDocument(lote)));
    }

    @Override
    public Optional<Lote> findLoteById(String id) {
        return ObjectId.isValid(id) ? loteRepository.findById(id).map(mapper::toDomain) : Optional.empty();
    }

    @Override
    public Page<Lote> findLotesByProveedorId(String proveedorId, String buscar, Pageable pageable) {
        ObjectId id = new ObjectId(proveedorId);
        var resultado = buscar.isEmpty()
                ? loteRepository.findLotes(id, pageable)
                : loteRepository.buscarLotes(id, Pattern.quote(buscar), pageable);
        return resultado.map(mapper::toDomain);
    }

    @Override
    public long countPagadosByProveedorId(String proveedorId) {
        return loteRepository.countPagados(new ObjectId(proveedorId));
    }

    @Override
    public long countPorPagarByProveedorId(String proveedorId) {
        return loteRepository.countPorPagar(new ObjectId(proveedorId));
    }

    @Override
    public List<Lote> findPendientesByProveedorId(String proveedorId) {
        return loteRepository.findPendientes(new ObjectId(proveedorId)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProviderTransaction insert(ProviderTransaction transaccion) {
        if (transaccion.getId() != null) {
            throw new IllegalArgumentException("Los movimientos son inmutables; solo se permite insertar nuevos");
        }
        return mapper.toDomain(transaccionRepository.insert(mapper.toDocument(transaccion)));
    }

    @Override
    public Page<ProviderTransaction> findAbonosByProveedorId(String proveedorId, Pageable pageable) {
        return transaccionRepository.findAbonos(new ObjectId(proveedorId), pageable).map(mapper::toDomain);
    }
}
