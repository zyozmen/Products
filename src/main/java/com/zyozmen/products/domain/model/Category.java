package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Categoría de dominio asociada a un producto.
 */
public class Category {

	private Long categoryId;
	private String name;
	private String slug;
	private Long productsCount;

	public Category() {
	}

	public Category(Long categoryId, String name, String slug) {
		this.categoryId = categoryId;
		this.name = name;
		this.slug = slug;
	}

	public Category(Long categoryId, String name, String slug, Long productsCount) {
		this.categoryId = categoryId;
		this.name = name;
		this.slug = slug;
		this.productsCount = productsCount;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public Long getProductsCount() {
		return productsCount;
	}

	public void setProductsCount(Long productsCount) {
		this.productsCount = productsCount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Category category = (Category) o;
		return Objects.equals(categoryId, category.categoryId)
				&& Objects.equals(name, category.name)
				&& Objects.equals(slug, category.slug)
				&& Objects.equals(productsCount, category.productsCount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryId, name, slug, productsCount);
	}

	@Override
	public String toString() {
		return "Category{" +
				"categoryId='" + categoryId + '\'' +
				", name='" + name + '\'' +
				", slug='" + slug + '\'' +
				", productsCount='" + productsCount + '\'' +
				'}';
	}

	public static class Builder {

		private Long categoryId;
		private String name;
		private String slug;
		private Long productsCount;

		public Builder categoryId(Long categoryId) {
			this.categoryId = categoryId;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder slug(String slug) {
			this.slug = slug;
			return this;
		}

		public Builder productsCount(Long productsCount) {
			this.productsCount = productsCount;
			return this;
		}

		public Category build() {
			return new Category(categoryId, name, slug, productsCount);
		}
	}
}
