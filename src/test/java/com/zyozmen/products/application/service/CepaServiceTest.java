package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.port.out.CepaRepositoryPort;
import com.zyozmen.products.domain.port.out.CategoryRepositoryPort;
import com.zyozmen.products.domain.model.CatalogCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CepaServiceTest {
    @Mock private CepaRepositoryPort repository;
    @Mock private CategoryRepositoryPort categories;
    @InjectMocks private CepaService service;

    @Test
    void crearConservaNombreVisibleYGeneraIdentidadPersistente() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var cepa = service.crear("  Tangerine   Dream ");
        assertThat(cepa.nombre()).isEqualTo("Tangerine Dream");
        assertThat(cepa.id()).isNotBlank();
        assertThat(cepa.createdAt()).isEqualTo(cepa.updatedAt());
        assertThat(cepa.fotografia()).isNull();
    }

    @Test
    void renombrarNoCambiaIdentidadNiFotografia() {
        var foto = new Imagen("foto.png", "png", "AQID");
        var anterior = new Cepa("id", "Anterior", foto, Instant.EPOCH, Instant.EPOCH, 3L, List.of(7L));
        when(repository.findById("id")).thenReturn(Optional.of(anterior));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var nueva = service.actualizar("id", "Nuevo nombre", 3L);
        assertThat(nueva.id()).isEqualTo("id");
        assertThat(nueva.fotografia()).isEqualTo(foto);
        assertThat(nueva.createdAt()).isEqualTo(Instant.EPOCH);
        assertThat(nueva.nombre()).isEqualTo("Nuevo nombre");
        assertThat(nueva.categoryIds()).containsExactly(7L);
        assertThat(service.guardarFotografia("id", foto).categoryIds()).containsExactly(7L);
        assertThatThrownBy(() -> service.actualizar("id", "Otro", 2L))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void categoriasSeValidanYSePuedenAsignarOVaciar() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categories.findById(7L)).thenReturn(Optional.of(CatalogCategory.builder().categoryId(7L).build()));
        var creada = service.crear("Tangerine", List.of(7L, 7L));
        assertThat(creada.categoryIds()).containsExactly(7L);
        var anterior = new Cepa("id", creada.nombre(), null, Instant.EPOCH, Instant.EPOCH, 2L, creada.categoryIds());
        when(repository.findById("id")).thenReturn(Optional.of(anterior));
        assertThat(service.actualizar("id", "Tangerine", 2L, List.of()).categoryIds()).isEmpty();
        assertThatThrownBy(() -> service.crear("Invalida", List.of(8L)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> service.crear("Invalida", List.of(0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fotografiaSeGuardaEnCepaYExigeExistencia() {
        when(repository.findById("id")).thenReturn(Optional.of(new Cepa("id", "Tangerine")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var foto = new Imagen("foto.webp", "webp", "AQID");
        assertThat(service.guardarFotografia("id", foto).fotografia()).isEqualTo(foto);
        assertThatThrownBy(() -> service.obtener("ausente")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.guardarFotografia("id", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaNombreYPaginacion() {
        assertThatThrownBy(() -> service.crear(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.crear("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(0, 101)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
