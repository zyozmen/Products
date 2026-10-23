package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ImagenMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMarketplaceMongoRepository;
import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.ProductoCatalogo;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarketplaceMongoAdapter implements MarketplaceRepositoryPort {
    private final ProductoMarketplaceMongoRepository repository;
    private final MarketplaceCatalogoQuery catalogo;
    private final MongoTemplate template;

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public boolean existsByPresentacionId(String presentacionId) {
        return repository.existsByPresentacionId(presentacionId);
    }

    @Override
    public void insert(ProductoMarketplace p) {
        repository.insert(new ProductoMarketplaceDocument(null, p.presentacionId(), p.loteId(), p.proveedorNombre(),
                p.nombreProducto(), p.cepaNombre(), p.sku(), p.precioVenta(), p.stockDisponible(),
                ImagenMongoMapper.toDocument(p.fotografia()), p.estado(), p.cepaId(), p.presentacionNombre(),
                p.contenidoUnidad(), p.unidadBase()));
    }

    @Override
    public Optional<ProductoMarketplace> findByPresentacionId(String presentacionId) {
        if (presentacionId == null || !ObjectId.isValid(presentacionId)) {
            throw new IllegalArgumentException("El ID de presentacion debe ser un ObjectId valido");
        }
        return repository.findByPresentacionId(presentacionId).map(this::toDomain);
    }

    @Override
    public List<ProductoMarketplace> findDisponiblesByCepaId(String cepaId) {
        return repository.findByCepaIdAndEstadoAndStockDisponibleGreaterThan(cepaId, "ACTIVO", 0)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Page<ProductoCatalogo> buscarCatalogo(String buscar, Pageable pageable) {
        return catalogo.buscar(buscar, pageable);
    }

    @Override
    public Page<ProductoCatalogo> buscarCatalogo(MarketplaceCatalogFilter filtro, Pageable pageable) {
        return catalogo.buscar(filtro, pageable);
    }

    @Override
    public void actualizarStock(String presentacionId, String loteId, int anterior, int nuevo) {
        if (anterior < 0 || nuevo < 0 || anterior == nuevo) {
            throw new IllegalArgumentException("El movimiento de inventario debe cambiar existencias no negativas");
        }
        var filtro = Criteria.where("presentacionId").is(presentacionId)
                .and("loteId").is(loteId).and("stockDisponible").is(anterior);
        if (nuevo < anterior) {
            filtro.and("estado").is("ACTIVO");
        }
        var resultado = template.updateFirst(Query.query(filtro),
                Update.update("stockDisponible", nuevo), ProductoMarketplaceDocument.class);
        if (resultado.getModifiedCount() != 1) {
            throw new OrderConflictException(
                    "El stock de marketplace cambio o no coincide con el lote; vuelva a consultar la presentacion");
        }
    }

    private ProductoMarketplace toDomain(ProductoMarketplaceDocument p) {
        return new ProductoMarketplace(p.presentacionId(), p.loteId(), p.proveedorNombre(), p.nombreProducto(),
                p.cepaNombre(), p.sku(), p.precioVenta(), p.stockDisponible(),
                ImagenMongoMapper.toDomain(p.fotografia()), p.estado(), p.cepaId(), p.presentacionNombre(),
                p.contenidoUnidad(), p.unidadBase());
    }
}
