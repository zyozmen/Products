package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.User;

/**
 * Puerto de salida para notificaciones por correo electrónico a usuarios.
 */
public interface UserNotificationPort {

    /**
     * Envía el correo de bienvenida a un usuario recién registrado.
     * Las implementaciones no deben propagar fallos de envío: el registro
     * no depende de la entrega del correo.
     */
    void sendWelcomeEmail(User user);
}
