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

import java.util.ArrayList;

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
    public MainServer(int LOCAL_PORT, int WORKER_PORT) {
        this.LOCAL_PORT = LOCAL_PORT;
        this.WORKER_PORT = WORKER_PORT;
        this.CLIENT_PORT = 3000;
        target_ip="localhost";
        connector=new UDPConnector(LOCAL_PORT);
        listening=true;
        
    }
    
    /**
     * este metodo se encarga de realizar el envio de manera asincrona
     */
    public void sendImage(int PORT){
        new Thread( () -> {
            connector.send(workedImage, PORT, target_ip);
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
            while(true){
            System.out.println("[ACTION: ] waiting images...");
            workedImage=connector.receive();
            System.out.println("[INFO: ] image received: "+workedImage.getStatus());
                        if(workedImage!=null){
                        switch(workedImage.getStatus()) 
                        {
                            case "CLIENT":
                                System.out.println("[ACTION: ] sending image to worker");
                                sendImage(WORKER_PORT);
                                System.out.println("[INFO: ] image sent to worker");
                                System.out.println();
                                break;
                            case "WORKER":
                                System.out.println("[ACTION: ] sending image to client");
                                sendImage(CLIENT_PORT);
                                System.out.println("[INFO: ] image sent to client");
                                System.out.println();
                                break;

                        }   
                        }
            
            }
        }).start();
    }
}
