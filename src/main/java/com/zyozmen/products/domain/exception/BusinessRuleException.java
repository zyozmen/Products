package com.zyozmen.products.domain.exception;

/** Regla de negocio financiera violada (p. ej. abono mayor al costo del lote). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
