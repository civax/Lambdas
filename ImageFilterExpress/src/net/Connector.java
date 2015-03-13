/*
 * Copyright (C) 2015 carcasti
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net;

/**
 * Esta Interfaz establece los métodos que se utilizarán en las conexiones realizadas en el sistema
 * @param <T> tipo de objeto que sera enviado en el sistema
 * @since 2015
 */
public interface Connector <T>{
    /**
     * Método para enviar un objecto
     * @param object Objecto que será enviado
     * @param port puerto de comunicación con el proceso destino
     * @param ip dirección ip del proceso destino
     */
    public void send(T object,int port,String ip);
    /**
     * Método de recepción de Objetos 
     * @return T regresa un objeto que fue recibido desde otro proceso
     */
    public T receive();
}
