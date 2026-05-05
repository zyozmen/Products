package com.zyozmen.products.adapter.out.persistence;

import com.zyozmen.products.domain.exception.ServiceUnavailableException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.adapter.out.persistence.mapper.ProductoPersistenceMapper;
import com.zyozmen.products.adapter.out.persistence.repository.ProductoJpaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoRepositoryAdapter - Resilience unit tests")
class ProductoRepositoryAdapterResilienceTest {

    @Mock
    private ProductoJpaRepository jpaRepository;

    @Mock
    private ProductoPersistenceMapper mapper;

    private ProductoRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductoRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("Métodos CRUD tienen @Retry y @CircuitBreaker configurados")
    void resilienceAnnotations_estanPresentesEnMetodosCrud() throws NoSuchMethodException {
        assertResilienceAnnotations("findAll", new Class<?>[]{}, "findAllFallback");
        assertResilienceAnnotations("findById", new Class<?>[]{Long.class}, "findByIdFallback");
        assertResilienceAnnotations("save", new Class<?>[]{Producto.class}, "saveFallback");
        assertResilienceAnnotations("existsById", new Class<?>[]{Long.class}, "existsByIdFallback");
        assertResilienceAnnotations("deleteById", new Class<?>[]{Long.class}, "deleteByIdFallback");
    }

    @Test
    @DisplayName("findAllFallback - traduce fallo técnico a ServiceUnavailableException")
    void findAllFallback_lanzaServiceUnavailableException() {
        RuntimeException cause = new RuntimeException("db timeout");

        assertThatThrownBy(() -> adapter.findAllFallback(cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está disponible")
                .hasCause(cause);
    }

    @Test
    @DisplayName("findByIdFallback - traduce fallo técnico a ServiceUnavailableException")
    void findByIdFallback_lanzaServiceUnavailableException() {
        RuntimeException cause = new RuntimeException("db timeout");

        assertThatThrownBy(() -> adapter.findByIdFallback(1L, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está disponible")
                .hasCause(cause);
    }

    @Test
    @DisplayName("saveFallback - traduce fallo técnico a ServiceUnavailableException")
    void saveFallback_lanzaServiceUnavailableException() {
        RuntimeException cause = new RuntimeException("db timeout");
        Producto producto = new Producto();

        assertThatThrownBy(() -> adapter.saveFallback(producto, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está disponible")
                .hasCause(cause);
    }

    @Test
    @DisplayName("existsByIdFallback - traduce fallo técnico a ServiceUnavailableException")
    void existsByIdFallback_lanzaServiceUnavailableException() {
        RuntimeException cause = new RuntimeException("db timeout");

        assertThatThrownBy(() -> adapter.existsByIdFallback(1L, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está disponible")
                .hasCause(cause);
    }

    @Test
    @DisplayName("deleteByIdFallback - traduce fallo técnico a ServiceUnavailableException")
    void deleteByIdFallback_lanzaServiceUnavailableException() {
        RuntimeException cause = new RuntimeException("db timeout");

        assertThatThrownBy(() -> adapter.deleteByIdFallback(1L, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está disponible")
                .hasCause(cause);
    }

    private void assertResilienceAnnotations(
            String methodName,
            Class<?>[] parameterTypes,
            String expectedFallbackMethod
    ) throws NoSuchMethodException {
        Method method = ProductoRepositoryAdapter.class.getMethod(methodName, parameterTypes);

        Retry retry = method.getAnnotation(Retry.class);
        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);

        assertThat(retry).isNotNull();
        assertThat(retry.name()).isEqualTo("productoRepository");

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.name()).isEqualTo("productoRepository");
        assertThat(circuitBreaker.fallbackMethod()).isEqualTo(expectedFallbackMethod);
    }
}