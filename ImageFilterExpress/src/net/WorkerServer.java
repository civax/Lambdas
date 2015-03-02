/**
 * Copyright (C) 2015 
 *
 *  @author Mary Carmen Ríos Ramírez
 *  @author Laura Lizeth Heredia Manzano 
 *  @author Carlos Iván Castillo Sepúlveda
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
 * Esta clase realiza el procesamiento de las imagenes, y la aplicacion de filtros, recibe partes de la imagen original del main server, las procesa y las regresa al server de origen
 * @see MainServer
 */
public class WorkerServer {
    private final UDPConnector connector;
    private final int LOCAL_PORT;
    private Image workedImage;
    private int target_port;
    private String target_ip;
    
    public WorkerServer(int LOCAL_PORT) {
        this.LOCAL_PORT = LOCAL_PORT;
        connector=new UDPConnector(LOCAL_PORT);
    }
    
    /**
     * este metodo se encarga de realizar el envio de manera asincrona
     */
    public void sendImage(){
        new Thread( () -> {
            connector.send(workedImage, target_port, target_ip);
            setBusy(false);
            
        }).start();
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    private  boolean listening;
    /**
     * Este metodo termina el proceso de recepcion de mensajes
     */
    public void stopListening(){
        listening=false;
    }
    private boolean busy;
    /**
     * Este metodo recibe imagenes mientras la bandera este activada y las almacena en una cola de imagenes
     */
    public synchronized void receiveImages(){
        listening=true;
        new Thread( () -> {
            while(listening){
                workedImage=connector.receive();
                setBusy(true);
            }
        }).start();
    }
}
