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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
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
    final String IP;
    final int PORT;
    
    public Process(String id,String ip,int port, String syncIP, int syncPORT){
        this.Id = id;
        this.file = file;
        list =  new ArrayList();
        inCS = false;
        this.IP= ip;
        this.PORT = port;
        System.out.println("Process "+this.Id+" running at: "+this.IP+":"+this.PORT);
        //this.receiveRequest()
        ;
    }
    
    public Request request(){
        //Agregar request a su misma cola
        Request req =  new Request(Id,"R");
        list.add(req);
        listProcess.stream().filter(
                (p) -> (
                        !this.Id.equals(p.Id)
                        )
                ).forEach(
                        (p) -> {
                            this.sendRequest(req, p.PORT, p.IP);
                        }
                );
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
                        ip = p.IP;
                        port = p.PORT;
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
        Process p=null;
        BufferedWriter out = null;
        BufferedReader in =  null;
        try  
        {
            FileReader fread = new FileReader("Processes.txt");
            in =  new BufferedReader(fread);
            String aux = "";
            
            while ((aux = in.readLine()) != null) {
                String st[] =  aux.split(" ");
              //  listProcess.add(new Process(st[0], st[1], st[2], Integer.parseInt(st[3])));
            }
            in.close();
            
            //String id,String file,String IP,int port
            FileWriter fstream = new FileWriter("Processes.txt", true); //true tells to append data.            
            out = new BufferedWriter(fstream);
            int number = listProcess.size() +1;
           // p =  new Process("p" + number,"Conf" + number + ".properties", "localhost", 1000+number);
            
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
           // p =  new Process("p1" ,"Conf1.properties", "localhost", 1001);
        }
        return p;
    }
    
    public static void main(String args[]) throws IOException{
        String processId;
        int port;
        InetAddress IP=InetAddress.getLocalHost();
        String syncIP;
        int syncPORT;
        if(args.length==2){
            try{
                processId=args[0].toUpperCase();
                port=Integer.parseInt(args[1]);
                syncIP=null;
                syncPORT=0;
                Process process =  new Process(processId,IP.getHostAddress(),port,syncIP,syncPORT);
            }catch(Exception e){
              System.err.println("Indicar el identificador del proceso [P1,P2,P3]  el puerto [10000-10003] y el host de sincronizacion");  
            }
            //Process p = registerProcess();
            //p.request();
        }else if (args.length==4){
            processId=args[0].toUpperCase();
            port=Integer.parseInt(args[1]);
            syncIP=args[2].toUpperCase();
            syncPORT=Integer.parseInt(args[3]);
            Process process =  new Process(processId,IP.getHostAddress(),port,syncIP,syncPORT);
        }
        else{
            System.err.println("Indicar el identificador del proceso [P1,P2,P3]  el puerto [10000-10003] y el host de sincronizacion");
                    
        }
            
        
    }
}
