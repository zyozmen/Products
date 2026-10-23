package com.zyozmen.products.exception;

import com.mongodb.MongoException;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;

import static org.assertj.core.api.Assertions.*;

class OrderInventoryConflictTest {
    private final OrderExceptionHandler handler = new OrderExceptionHandler();

    @Test
    void writeConflictSeExponeComo409SinDetallesInternos() {
        var response = handler.handleMongoConflict(
                new UncategorizedMongoDbException("error interno", new MongoException(112, "WriteConflict")));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody().get("message")).doesNotContain("error interno", "WriteConflict");
    }

    @Test
    void conflictoTransaccionalAlConfirmarSeExponeComo409() {
        var mongo = new MongoException("transaccion abortada");
        mongo.addLabel("TransientTransactionError");
        var response = handler.handleMongoConflict(new MongoTransactionException("fallo al confirmar", mongo));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void otrosErroresMongoNoSeOcultanComoConflictosDeStock() {
        var error = new UncategorizedMongoDbException("otro error", new MongoException(13, "Unauthorized"));
        assertThatThrownBy(() -> handler.handleMongoConflict(error)).isSameAs(error);
        var commitDesconocido = new MongoException("confirmacion desconocida");
        commitDesconocido.addLabel("UnknownTransactionCommitResult");
        var commit = new MongoTransactionException("fallo", commitDesconocido);
        assertThatThrownBy(() -> handler.handleMongoConflict(commit)).isSameAs(commit);
    }
}
