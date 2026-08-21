package com.zyozmen.products.E2E;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class ProductoApiIT extends ApiIntegrationTest {

    private static final String VALID_PRODUCT = """
            {
              "name": "Producto E2E",
              "slug": "producto-e2e",
              "description": "Producto creado por una prueba end to end",
              "sku": "E2E-001",
              "status": "active",
                            "categories": [
                                {
                                    "category_id": 1,
                                    "name": "Electronics",
                                    "slug": "electronics"
                                }
                            ],
              "price": {
                "current": 99.99,
                "original": 129.99,
                "currency": "USD",
                "discount_percentage": 23,
                "tax_inclusive": true
              }
            }
            """;

    @Test
    void shouldCreateAndRetrieveProductThroughHttp() {
        String productId = given()
                .contentType(ContentType.JSON)
                .body(VALID_PRODUCT)
            .when()
                .post("/api/productos")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo("Producto E2E"))
                .body("price.current", equalTo(99.99f))
                .extract()
                .path("id");

        given()
            .when()
                .get("/api/productos/{id}", productId)
            .then()
                .statusCode(200)
                .body("id", equalTo(productId))
                .body("sku", equalTo("E2E-001"));
    }

    @Test
    void shouldReturnBadRequestForInvalidProduct() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"\"}")
            .when()
                .post("/api/productos")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue());
    }

    @Test
    void shouldReturnBadRequestWhenProductHasNoCategories() {
                String productWithoutCategories = """
                                {
                                    "name": "Producto sin categoría",
                                    "slug": "producto-sin-categoria",
                                    "sku": "E2E-NO-CATEGORY",
                                    "status": "active",
                                    "categories": [],
                                    "price": {
                                        "current": 99.99,
                                        "currency": "USD"
                                    }
                                }
                                """;

        given()
                .contentType(ContentType.JSON)
                .body(productWithoutCategories)
            .when()
                .post("/api/productos")
            .then()
                .statusCode(400)
                .body("validationErrors", containsString("El producto debe tener al menos una categoría"));
    }

    @Test
    void shouldReturnNotFoundForUnknownProduct() {
        given()
            .when()
                .get("/api/productos/{id}", "000000000000000000000099")
            .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Producto"));
    }

    @Test
    void shouldDeleteProductThroughHttp() {
        ProductoResponseDTO product = given()
                .contentType(ContentType.JSON)
                .body(VALID_PRODUCT.replace("E2E-001", "E2E-DELETE"))
            .when()
                .post("/api/productos")
            .then()
                .statusCode(201)
                .extract()
                .as(ProductoResponseDTO.class);

        given()
            .when()
                .delete("/api/productos/{id}", product.getId())
            .then()
                .statusCode(204)
                .body(equalTo(""));

        given()
            .when()
                .get("/api/productos/{id}", product.getId())
            .then()
                .statusCode(404);
    }
}