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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.util.Clock;

/**
 *
 * esta clase implementa los metodos de coneccion utilizando el protocolo UDP
 */
public class UDPConnector implements Connector<Image>{
    private final int LOCAL_PORT;
    private int targetPort;

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getTargetPort() {
        return targetPort;
    }
    
    public UDPConnector(int port){
		this.LOCAL_PORT = port;
		clock = new Clock();		
	}
    private Clock clock;
    private final int BUFFER_SIZE=1024*5;
    @Override
    public void send(Image image, int port, String ip) {
        try {
            InetAddress ia = InetAddress.getByName(ip);
            //Message message = new Message("hola que hay", clock.getValue(), port+"");
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(byteArr);
            os.writeObject(image);
            byte arr [] = byteArr.toByteArray();
            socket.send(new DatagramPacket(arr, arr.length,ia,port));
	} catch (IOException ex) { 
            Logger.getLogger(UDPConnector.class.getName()).log(Level.SEVERE, "error de envio", ex);
        } 
    }

    @Override
    public Image receive() {
        final DatagramSocket socket;
        Image receivedImage = null;
        try {
            //conectando a socket local para realizar lectura/recepcion de informacion
            socket = new DatagramSocket(LOCAL_PORT);
            SimpleDateFormat format = new SimpleDateFormat();
            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, BUFFER_SIZE);

            socket.receive(packet);
            byte byteArr[]= packet.getData();
            //se extrae la informacion del socket
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(byteArr, 0, byteArr.length) );
            receivedImage= (Image) is.readObject();
            clock.receiveAction(receivedImage.getClock());
            
            receivedImage.setClock(clock.getTime());
          
            System.out.println(format.format(new Date())+receivedImage);
	} catch (SocketException ex) {
            Logger.getLogger(UDPConnector.class.getName()).log(Level.SEVERE, "Error de coneccion al socket", ex);
        }catch (IOException | ClassNotFoundException e1) {
            Logger.getLogger(UDPConnector.class.getName()).log(Level.SEVERE, "Error de escritura", e1);
	}
        return receivedImage; 
    }
    
}
