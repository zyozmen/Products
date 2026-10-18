package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.ProveedorMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.LoteMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.ProveedorMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.TransaccionProveedorMongoRepository;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import com.zyozmen.products.domain.port.out.TransaccionProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProveedorMongoAdapter
        implements ProveedorRepositoryPort, LoteRepositoryPort, TransaccionProveedorRepositoryPort {

    private final ProveedorMongoRepository proveedorRepository;
    private final LoteMongoRepository loteRepository;
    private final TransaccionProveedorMongoRepository transaccionRepository;
    private final ProveedorMongoMapper mapper;

    @Override
    public Proveedor save(Proveedor proveedor) {
        return mapper.toDomain(proveedorRepository.save(mapper.toDocument(proveedor)));
    }

    @Override
    public Optional<Proveedor> findById(String id) {
        return ObjectId.isValid(id) ? proveedorRepository.findById(id).map(mapper::toDomain) : Optional.empty();
    }

    @Override
    public boolean existsByNit(String nit) {
        return proveedorRepository.existsByNit(nit);
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
    public boolean existsByCodigoLote(String codigoLote) {
        return loteRepository.existsByCodigoLote(codigoLote);
    }

    @Override
    public List<Lote> findPendientesByProveedorId(String proveedorId) {
        return loteRepository.findPendientes(new ObjectId(proveedorId)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public TransaccionProveedor save(TransaccionProveedor transaccion) {
        return mapper.toDomain(transaccionRepository.save(mapper.toDocument(transaccion)));
    }
}
