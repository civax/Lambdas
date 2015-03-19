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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
/**
 * Esta Clase contiene la lógica de control para las instancias de un cliente
 */
public class ClientController {
    private final UDPConnector connector;
    private final int LOCAL_PORT;
    private int target_port;
    private String target_ip;
    private Image image;
    private ArrayList<Image> imageQueue;
    
    public static void main(String args[]){
        
        int localPort = 3000;//Integer.parseInt(args[0]);
	int mainServerPort = 5000;//Integer.parseInt(args[1]);
        final ClientController client;
        try{
	 client=new ClientController(localPort);
         client.target_port=5000;
         
         new Thread(
                ()->{
                   // while(mainserver.listening){
                        Image image =new Image("first image", 0, "1");
                        image.setStatus("CLIENT");
                        SimpleDateFormat format = new SimpleDateFormat();
                        System.out.println("["+format.format(new Date())+"] "+image);
                        client.setImage(image);
                        System.out.println("[ACTION: ] sending image to server");
                        client.sendImage();
                        System.out.println("[INFO: ] image sent to server");
                        
                        client.receiveImages();
                        
                   // }
                }
         ).start();
        
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Contructor de Client Controller
     * @param LOCAL_PORT Puerto que utilizará el proceso para escuchar procesos externos
     */
    public ClientController(int LOCAL_PORT) {
        this.LOCAL_PORT = LOCAL_PORT;
        connector=new UDPConnector(LOCAL_PORT);
        imageQueue = new ArrayList<>();
        
    }
    
    /**
     * este metodo se encarga de realizar el envio de manera asincrona
     */
    public void sendImage(){
        new Thread( () -> {
            connector.send(image, target_port, target_ip);
        }).start();
    }
    private  boolean listening;
    /**
     * Este metodo termina el proceso de recepcion de mensajes
     */
    public void stopListening(){
        listening=false;
    }
    /**
     * Este metodo recibe imagenes mientras la bandera este activada y las almacena en una cola de imagenes
     */
    public synchronized void receiveImages(){
        listening=true;
        new Thread( () -> {
            //while(listening){
            System.out.println("[ACTION: ] waiting images...");
                
                Object remoteObject=connector.receive();
                Image workedImage=null;
                SimpleDateFormat format = new SimpleDateFormat();
                if(remoteObject instanceof Image){
                    workedImage=(Image)remoteObject;
                    connector.getClock().receiveAction(workedImage.getClock());
                    workedImage.setClock(connector.getClock().getTime());
                    System.out.println("["+format.format(new Date())+"] "+workedImage);
                    imageQueue.add(workedImage);
                }
            System.out.println("[INFO: ] image sent to client");
            
            //}
        }).start();
    }
    /**
     * 
     */
    public void printImages(){
        imageQueue.forEach(image->{
                System.out.println(image);
        });
    }
    /**
     * Obtener puerto de escucha del Server principal
     * @return puerto del server
     */
    public int getTarget_port() {
        return target_port;
    }
    /**
     * Asignar puerto de escucha del Server principal
     * @param target_port puerto de server
     */
    public void setTarget_port(int target_port) {
        this.target_port = target_port;
    }
    /**
     * Obtener ip del Server principal
     * @return ip del server
     */
    public String getTarget_ip() {
        return target_ip;
    }
    /**
     * Asignar ip del Server principal
     * @param target_ip ip del server
     */
    public void setTarget_ip(String target_ip) {
        this.target_ip = target_ip;
    }
    /**
     * Método setter para el objeto imagen que será eviado para su posterior procesamiento
     * @param image Objeto imagen que será enviada para su procesamiento
     */
    public void setImage(Image image) {
        this.image = image;
    }
    
}
