package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Categoría de dominio asociada a un producto.
 */
public class Category {

	private String categoryId;
	private String name;
	private String slug;

	public Category() {
	}

	public Category(String categoryId, String name, String slug) {
		this.categoryId = categoryId;
		this.name = name;
		this.slug = slug;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Category category = (Category) o;
		return Objects.equals(categoryId, category.categoryId)
				&& Objects.equals(name, category.name)
				&& Objects.equals(slug, category.slug);
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryId, name, slug);
	}

	@Override
	public String toString() {
		return "Category{" +
				"categoryId='" + categoryId + '\'' +
				", name='" + name + '\'' +
				", slug='" + slug + '\'' +
				'}';
	}

	public static class Builder {

		private String categoryId;
		private String name;
		private String slug;

		public Builder categoryId(String categoryId) {
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

		public Category build() {
			return new Category(categoryId, name, slug);
		}
	}
}
