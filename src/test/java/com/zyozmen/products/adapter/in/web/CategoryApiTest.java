package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.mapper.CategoryWebMapper;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CategoryController.class, ImageController.class, ProductoController.class, CepaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CategoryWebMapper.class,
        ProductoWebMapper.class, ImageUploadService.class})
class CategoryApiTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private CategoryUseCase categories;
    @MockBean private com.zyozmen.products.domain.port.in.CepaUseCase cepas;
    @MockBean
    private ProductoUseCase products;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private RevokedTokenRepositoryPort revokedTokens;

    @Test
    void cepaUploadUsesSharedValidatorAndPersistsBase64WithStringIdentity() throws Exception {
        var main = new MockMultipartFile("fotoPrincipal", "cepa.webp", "image/webp", new byte[]{1, 2, 3});
        var foto = new Imagen("cepa.webp", "webp", "AQID");
        when(cepas.guardarFotografia("cepa-id", foto)).thenReturn(
                new com.zyozmen.products.domain.model.Cepa("cepa-id", "Tangerine", foto, null, null, 1L));
        mvc.perform(multipart("/api/v1/images/cepa-id").file(main).param("target", "CEPA")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotografia.filepart").value("AQID"));
        mvc.perform(multipart("/api/v1/images/cepa-id").file(main).param("target", "CEPA"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/images/cepa-id").file(main).param("target", "CEPA")
                        .with(user("client").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/images/cepa-id").param("target", "CEPA")
                        .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(categories, products);
    }

    @Test
    void cepasAreAdminOnlyAndSupportCreateUpdateAndList() throws Exception {
        var cepa = new com.zyozmen.products.domain.model.Cepa("cepa-id", "Tangerine");
        when(cepas.crear("Tangerine")).thenReturn(cepa);
        when(cepas.actualizar("cepa-id", "Tangerine", 1L)).thenReturn(cepa);
        when(cepas.listar(0, 20)).thenReturn(new PageImpl<>(List.of(cepa)));
        mvc.perform(post("/api/cepas").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Tangerine\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("cepa-id"));
        mvc.perform(put("/api/cepas/cepa-id").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Tangerine\",\"version\":1}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/cepas").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "1"));
        mvc.perform(get("/api/cepas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cepas").with(user("client").roles("CLIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void listAndDetailArePublicAndDoNotRequireProducts() throws Exception {
        var category = category();
        when(categories.getAllCategories()).thenReturn(List.of(category));
        when(categories.getCategoryById(7L)).thenReturn(category);

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Audio"))
                .andExpect(jsonPath("$[0].description").value("Speakers"));
        mvc.perform(get("/api/v1/categories/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/audio.png"));
        verifyNoInteractions(products);
    }

    @Test
    void adminCanCreateAndUpdateCategory() throws Exception {
        when(categories.createCategory(any())).thenReturn(category());
        when(categories.updateCategory(eq(7L), any())).thenReturn(category());
        String request = "{\"name\":\"Audio\",\"description\":\"Speakers\",\"imageUrl\":\"https://example.com/audio.png\"}";

        mvc.perform(post("/api/v1/categories").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7));
        mvc.perform(put("/api/v1/categories/7").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Audio"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void categoryWritesRequireAdmin(String method) throws Exception {
        String path = method.equals("POST") ? "/api/v1/categories" : "/api/v1/categories/7";
        var request = request(org.springframework.http.HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Audio\"}");
        mvc.perform(request).andExpect(status().isUnauthorized());
        mvc.perform(request.with(user("client").roles("CLIENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(categories);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\" \"}",
            "{\"name\":\"Audio\",\"imageUrl\":\"javascript:alert(1)\"}"
    })
    void invalidCategoryDtoReturnsBadRequest(String body) throws Exception {
        mvc.perform(post("/api/v1/categories").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(categories);
    }

    @Test
    void categoryFieldLengthLimitsAreEnforced() throws Exception {
        String body = "{\"name\":\"" + "a".repeat(101) + "\",\"description\":\"" + "b".repeat(1001) + "\"}";
        mvc.perform(post("/api/v1/categories").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(categories);
    }

    @Test
    void missingCategoryReturns404AndDuplicateNameReturns409() throws Exception {
        when(categories.getCategoryById(99L)).thenThrow(new ResourceNotFoundException("Category not found"));
        when(categories.createCategory(any())).thenThrow(new DuplicateKeyException("name"));

        mvc.perform(get("/api/v1/categories/99")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/categories").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Audio\"}"))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource({"png,image/png", "jpg,image/jpeg", "webp,image/webp"})
    void categoryImageIsPublicAndReturnsDecodedBytes(String extension, String contentType) throws Exception {
        var category = category();
        category.setImage(new Imagen("audio." + extension, extension, "AQID"));
        when(categories.getCategoryById(7L)).thenReturn(category);

        mvc.perform(get("/api/v1/categories/7/image")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType)).andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void absentStoredImageReturns404() throws Exception {
        when(categories.getCategoryById(7L)).thenReturn(category());

        mvc.perform(get("/api/v1/categories/7/image")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/images/7", "/api/productos/7/images", "/api/v1/products/7/images"})
    void categoryUploadOnlyProcessesMainImageOnBothNewAndExistingRoutes(String path) throws Exception {
        var main = new MockMultipartFile("fotoPrincipal", "audio.png", "image/png", new byte[]{1, 2, 3});
        var invalidSecondary = new MockMultipartFile("fotosSecundarias", "ignored.pdf", "application/pdf", new byte[]{4});
        when(categories.saveImage(eq(7L), any())).thenReturn(category());

        mvc.perform(multipart(path).file(main).file(invalidSecondary).param("target", "CATEGORY")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(categories).saveImage(7L, new Imagen("audio.png", "png", "AQID"));
        verifyNoInteractions(products);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/images/7", "/api/productos/7/images", "/api/v1/products/7/images"})
    void imageUploadsRequireAdmin(String path) throws Exception {
        var main = new MockMultipartFile("fotoPrincipal", "audio.png", "image/png", new byte[]{1});
        mvc.perform(multipart(path).file(main).param("target", "CATEGORY"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart(path).file(main).param("target", "CATEGORY")
                        .with(user("client").roles("CLIENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(categories, products);
    }

    @Test
    void productUploadWorksWithExplicitTargetAndLegacyDefault() throws Exception {
        var main = new MockMultipartFile("fotoPrincipal", "audio.png", "image/png", new byte[]{1, 2, 3});
        when(products.guardarImagenes(eq(7L), any(), anyList())).thenReturn(Producto.builder().id(7L).build());

        mvc.perform(multipart("/api/v1/images/7").file(main).param("target", "PRODUCT")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("7"));
        mvc.perform(multipart("/api/productos/7/images").file(main).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("7"));
        verifyNoInteractions(categories);
    }

    @Test
    void missingCategoryMainImageOrUnknownTargetReturns400() throws Exception {
        mvc.perform(multipart("/api/v1/images/7").param("target", "CATEGORY")
                        .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/images/7").param("target", "UNKNOWN")
                        .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(categories, products);
    }

    @Test
    void legacyProductImageRouteRejectsCepaInsteadOfOverwritingProduct() throws Exception {
        var main = new MockMultipartFile("fotoPrincipal", "cepa.png", "image/png", new byte[]{1});
        mvc.perform(multipart("/api/productos/7/images").file(main).param("target", "CEPA")
                        .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(categories, products, cepas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/productos", "/api/v1/products"})
    void categoryFilterWorksOnBothProductRoutes(String path) throws Exception {
        var product = Producto.builder().id(1L).name("Speaker").categoryId(7L).build();
        when(products.listarTodosPorCategorias(eq(List.of(7L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        mvc.perform(get(path).param("categoryId", "7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(7))
                .andExpect(jsonPath("$.content[0].category_ids[0]").value(7));
        verify(products).listarTodosPorCategorias(eq(List.of(7L)), any(Pageable.class));
    }

    @Test
    void invalidCategoryFilterReturns400() throws Exception {
        mvc.perform(get("/api/v1/products").param("categoryId", "0")).andExpect(status().isBadRequest());
        verifyNoInteractions(products);
    }

    @Test
    void productAliasDoesNotBypassAdminProtection() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/products/1").with(user("client").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/products/1").with(user("client").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private CatalogCategory category() {
        return CatalogCategory.builder().categoryId(7L).name("Audio").description("Speakers")
                .imageUrl("https://example.com/audio.png").build();
    }
}
