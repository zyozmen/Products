package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.adapter.in.web.dto.CategoryDTO;
import com.zyozmen.products.adapter.in.web.dto.PriceDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoListItemDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.RankingDTO;
import com.zyozmen.products.adapter.in.web.dto.RecentCommentDTO;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Comment;
import com.zyozmen.products.domain.model.Price;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.model.Ranking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoWebMapperTest {

    private final ProductoWebMapper mapper = new ProductoWebMapper();

    @Test
    void toDomainShouldMapRequestDtoToDomainModel() {
        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .id("1")
                .name("Headphones")
                .slug("headphones")
                .description("Great audio")
                .sku("SKU-1")
                .status("active")
                .categories(List.of(CategoryDTO.builder().categoryId(1L).name("Audio").slug("audio").build()))
                .price(PriceDTO.builder().current(BigDecimal.valueOf(199.99)).original(BigDecimal.valueOf(249.99)).currency("USD").build())
                .ranking(RankingDTO.builder().averageRating(BigDecimal.valueOf(4.5)).totalReviews(120).build())
                .recentComments(List.of(RecentCommentDTO.builder().username("alice").body("Nice").build()))
                .hasMoreComments(true)
                .build();

        Producto producto = mapper.toDomain(dto);

        assertThat(producto.getId()).isEqualTo("1");
        assertThat(producto.getName()).isEqualTo("Headphones");
        assertThat(producto.getPrice().getCurrent()).isEqualByComparingTo("199.99");
        assertThat(producto.getCategories()).hasSize(1);
        assertThat(producto.getRanking().getAverageRating()).isEqualByComparingTo("4.5");
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getHasMoreComments()).isTrue();
    }

    @Test
    void toResponseDTOShouldMapDomainModelToResponseDto() {
        Producto producto = Producto.builder()
                .id("2")
                .name("Keyboard")
                .slug("keyboard")
                .description("Mechanical")
                .sku("SKU-2")
                .status("active")
                .categories(List.of(Category.builder().categoryId(2L).name("Peripherals").slug("peripherals").build()))
                .price(Price.builder().current(BigDecimal.valueOf(89.99)).original(BigDecimal.valueOf(99.99)).currency("USD").build())
                .ranking(Ranking.builder().averageRating(BigDecimal.valueOf(4.2)).totalReviews(50).build())
                .recentComments(List.of(Comment.builder().username("bob").body("Good").build()))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        ProductoResponseDTO dto = mapper.toResponseDTO(producto);

        assertThat(dto.getId()).isEqualTo("2");
        assertThat(dto.getName()).isEqualTo("Keyboard");
        assertThat(dto.getPrice().getCurrent()).isEqualByComparingTo("89.99");
        assertThat(dto.getCategories()).hasSize(1);
        assertThat(dto.getRanking().getAverageRating()).isEqualByComparingTo("4.2");
        assertThat(dto.getRecentComments()).hasSize(1);
        assertThat(dto.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void toListItemDTOShouldMapDomainModelToListItemDto() {
        Producto producto = Producto.builder()
                .id("3")
                .name("Mouse")
                .categories(List.of(Category.builder().categoryId(3L).name("Peripherals").slug("peripherals").build()))
                .price(Price.builder().current(BigDecimal.valueOf(49.99)).original(BigDecimal.valueOf(59.99)).currency("USD").build())
                .ranking(Ranking.builder().averageRating(BigDecimal.valueOf(4.8)).totalReviews(300).build())
                .build();

        ProductoListItemDTO dto = mapper.toListItemDTO(producto);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("3");
        assertThat(dto.getName()).isEqualTo("Mouse");
        assertThat(dto.getCategoryIds()).containsExactly(3L);
        assertThat(dto.getCurrentPrice()).isEqualByComparingTo("49.99");
        assertThat(dto.getAverageRating()).isEqualByComparingTo("4.8");
        assertThat(dto.getTotalReviews()).isEqualTo(300);
    }
}
