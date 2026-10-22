package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.model.Contenedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.port.in.PresentacionUseCase;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PresentacionTransactionTest {
    @Test
    void falloAlPublicarSolicitaRollbackDeLaTransaccionDelEmpaquetado() {
        var lotes = mock(LoteRepositoryPort.class);
        var proveedores = mock(ProveedorRepositoryPort.class);
        var marketplace = mock(MarketplaceRepositoryPort.class);
        var cepas = mock(CepaUseCase.class);
        when(cepas.obtener("cepa")).thenReturn(new Cepa("cepa", "Sativa"));
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var lote = Lote.builder().id("l1").proveedorId("p1").contenedores(List.of(
                Contenedor.builder().id("c1").tipoCepa("Sativa").cepaId("cepa").unidadBase("GRAMOS")
                        .capacidadUnidadesBase(BigDecimal.TEN).costoBasePorUnidad(BigDecimal.ONE).build())).build();
        when(lotes.findLoteById("l1")).thenReturn(Optional.of(lote));
        when(proveedores.findById("p1")).thenReturn(Optional.of(Proveedor.builder().nombre("Proveedor").build()));
        when(lotes.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new DuplicateKeyException("publicacion concurrente")).when(marketplace).insert(any());
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(new PresentacionService(lotes, proveedores, marketplace, cepas));
        factory.addAdvice(interceptor);
        var service = (PresentacionUseCase) factory.getProxy();
        var comando = new PresentacionUseCase.CrearPresentacionCommand(null, "SKU", "Bolsa",
                BigDecimal.ONE, 1, BigDecimal.TEN, true, false, null);

        assertThatThrownBy(() -> service.empaquetar("l1", "c1", List.of(comando)))
                .isInstanceOf(DuplicateKeyException.class);

        verify(lotes).save(any());
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }
}
