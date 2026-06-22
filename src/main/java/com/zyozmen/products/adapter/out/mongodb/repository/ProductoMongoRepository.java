package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.CategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;

import java.util.List;

/**
 * Repositorio Spring Data MongoDB.
 * El dominio no lo conoce en absoluto; es un detalle de infraestructura.
 */
public interface ProductoMongoRepository extends MongoRepository<ProductoMongoDocument, String> {

	@Aggregation(pipeline = {
			"{ \"$match\": { \"categories\": { \"$exists\": true, \"$ne\": null } } }",
			"{ \"$unwind\": \"$categories\" }",
			"{ \"$replaceRoot\": { \"newRoot\": \"$categories\" } }",
			"{ \"$group\": { \"_id\": \"$category_id\", \"category_id\": { \"$first\": \"$category_id\" }, \"name\": { \"$first\": \"$name\" }, \"slug\": { \"$first\": \"$slug\" } } }",
			"{ \"$project\": { \"_id\": 0, \"category_id\": 1, \"name\": 1, \"slug\": 1 } }"
	})
	List<CategoryDocument> findDistinctCategories();

}
