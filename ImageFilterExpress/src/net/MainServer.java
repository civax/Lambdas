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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Esta es la clase del server Principal, sirve como receptor de pticiones de los clientes y reparte la carga de trabajo entre los objetos Worker server que tenga disponibles
 * @see WorkerServer
 * 
 */
public class MainServer {
    /**
     * En el metodo main se inicializa el server
     * @param args  0 corresponde al puerto por el que estara escuchando el server
     *              1 corresponde al puerto con el que se estara comunicando
     */
    public static void main(String args[]){
        final MainServer mainserver;
        try{
        int localPort = 5000;//Integer.parseInt(args[0]);
	int workerPort = 5001;//Integer.parseInt(args[1]);
	 mainserver=new MainServer(localPort,workerPort);  
         new Thread(
                ()->{
                   // while(mainserver.listening){
                    
                        mainserver.receiveImages();       
                   // }
                }
         ).start();
        
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private final UDPConnector connector;
    private final int LOCAL_PORT;
    private final int WORKER_PORT;
    private final int CLIENT_PORT;
    private Image workedImage;
    private int target_port;
    private String target_ip;
    private ArrayList<Worker> workers;
    /**
     * Este método permite registrar un worker en la cola de workers
     * 
     * @param port puerto de conexion del worker server
     * @param ip Dirección IP de conexion del worker server
     */
    public void registerWorker(int port,String ip){
        workers.add(
                    new Worker(port,ip)
        );
    }
    
    private class Worker{

        public Worker(int port, String ip) {
            this.port = port;
            this.ip = ip;
        }
        
        int port;
        String ip;
    }
    /**
     * 
     * @param LOCAL_PORT puerto por el cual se establecerá comunicacion con procesos externos, puerto de escucha
     * @param WORKER_PORT puerto de comunicación deel workerserver al que se enviará trabajo
     */
    public MainServer(int LOCAL_PORT, int WORKER_PORT) {
        this.LOCAL_PORT = LOCAL_PORT;
        this.WORKER_PORT = WORKER_PORT;
        this.CLIENT_PORT = 3000;
        try {
            InetAddress IP=InetAddress.getLocalHost();
            System.out.println("Main Server Listening at PORT: "+this.LOCAL_PORT+" host: "+IP.getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(MainServer.class.getName()).log(Level.SEVERE, "Error getting IP adress", ex);
        }
        
        //target_ip="localhost";//148.201.188.204
        connector=new UDPConnector(LOCAL_PORT);
        listening=true;
        
    }
    
    /**
     * este metodo se encarga de realizar el envio de manera asincrona
     * @param PORT puerto al que se enviara la imagen con la que se esta trabajando
     */
    public void sendImage(int PORT){
        new Thread( () -> {
            connector.send(workedImage, PORT, target_ip);
            System.out.println("[INFO: ] image sent to "+target_ip+":"+PORT);
            System.out.println();
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
    public  void receiveImages(){
        listening=true;
        new Thread( () -> {
            SimpleDateFormat format = new SimpleDateFormat();
            while(true){
            System.out.println("[ACTION: ] waiting images...");
                Object remoteObject=connector.receive();
                if(remoteObject instanceof Image){
                    workedImage=(Image)remoteObject;
                    connector.getClock().receiveAction(workedImage.getClock());
                    workedImage.setClock(connector.getClock().getTime());
                    System.out.println("["+format.format(new Date())+"] "+workedImage);
                }
            System.out.println("[INFO: ] image received: "+workedImage.getStatus());
                        if(workedImage!=null){
                        switch(workedImage.getStatus()) 
                        {
                            case "CLIENT":
                                System.out.println("[ACTION: ] sending image to worker");
                                target_ip="localhost";//192.168.0.104
                                sendImage(WORKER_PORT);
                                break;
                            case "WORKER":
                                System.out.println("[ACTION: ] sending image to client");
                                target_ip="localhost";//
                                sendImage(CLIENT_PORT);
                                break;

                        }   
                        }
            
            }
        }).start();
    }
}
