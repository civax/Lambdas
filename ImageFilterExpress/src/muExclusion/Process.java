/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package muExclusion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.UDPConnector;

/**
 *
 * @author mary
 */
public class Process {
    String Id;
    String file;
    /**
     * Cola de solicitudes
     */
    List<Request> list;
    static List<Process> listProcess;
    boolean inCS;
    UDPConnector connector;
    String ip;
    int port;
    
    public Process(String id,String file,String ip,int port){
        this.Id = id;
        this.file = file;
        list =  new ArrayList();
        inCS = false;
        this.ip= ip;
        this.port = port;
        this.receiveRequest();
    }
    
    public Request request(){
        //Agregar request a su misma cola
        Request req =  new Request(Id,"R");
        list.add(req);
        //Enviar request a los demas procesos.
        for (Process p : listProcess) {
            if(!this.Id.equals(p.Id))
                this.sendRequest(req, p.port, p.ip);
        }
        return req;
    }
    
    /**
     * Este metodo recibe requests mientras la bandera este activada y las almacena en una cola de requests
     */
    public synchronized void receiveRequest(){
       
        new Thread( () -> {
            //while(listening){
            System.out.println("[ACTION: ] waiting requests...");
            Object remoteObject=connector.receive();
            if(remoteObject instanceof Request){
            Request receivedRequest=(Request)remoteObject;
            list.add(receivedRequest);
            
            System.out.println("[INFO: ] request received in " + this.Id);
            //Si no esta en la CS enviar mensaje ACK
            if(!this.inCS){
                Request req = new Request(this.Id, "ACK");
                String ip="";
                int port=-1;
                
                for (Process p : listProcess) {
                    if(p.Id.equals(receivedRequest.process)){
                        ip = p.ip;
                        port = p.port;
                        break;
                    }
                }
            
                this.sendRequest(req, port, ip);
            }
            }
            //}
        }).start();
    }
    
    public void sendRequest(Request req,int port,String ip){
        new Thread( () -> {
            connector.send(req, port, ip);
        }).start();
    }
    
    public void sendACK(Request req,int port,String ip){
        new Thread( () -> {
            connector.send(req, port, ip);
        }).start();
    }
    //public String reply(String rep){
        
    //}
    
    public void open(){
        
    }
    
    public void close(){
        
    }
    
    public void update(){
        
    }
    
    public void read(){
        
    }
    
    public void write(){
        
    }
    
    private static Process registerProcess() throws IOException {
        listProcess =  new ArrayList<>();
        Process p;
        BufferedWriter out = null;
        BufferedReader in =  null;
        try  
        {
            FileReader fread = new FileReader("Processes.txt");
            in =  new BufferedReader(fread);
            String aux = "";
            
            while ((aux = in.readLine()) != null) {
                String st[] =  aux.split(" ");
                listProcess.add(new Process(st[0], st[1], st[2], Integer.parseInt(st[3])));
            }
            in.close();
            
            //String id,String file,String ip,int port
            FileWriter fstream = new FileWriter("Processes.txt", true); //true tells to append data.            
            out = new BufferedWriter(fstream);
            int number = listProcess.size() +1;
            p =  new Process("p" + number,"Conf" + number + ".properties", "localhost", 1000+number);
            
            out.write("p" + number + " Conf" + number + ".properties localhost " + (1000+number));
            out.close();           
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }
        finally
        {
            if(out != null) {
                out.close();
            }
            p =  new Process("p1" ,"Conf1.properties", "localhost", 1001);
        }
        return p;
    }
    
    public static void main(String args[]) throws IOException{
        
        Process p = registerProcess();
        p.request();
        
    }
}
