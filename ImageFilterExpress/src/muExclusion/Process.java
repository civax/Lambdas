/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package muExclusion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    List<Message> list;
    Set<Message> listACK;
    static List<Process> listProcess;
    boolean inCS;
    private static final String ACK="ACK";
    private static final String RELEASE="RELEASE";
    private static final String REQUEST="REQUEST";
    UDPConnector connector;
    final String IP;
    final int PORT;
    private boolean listening;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.Id);
        hash = 59 * hash + Objects.hashCode(this.IP);
        hash = 59 * hash + this.PORT;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Process other = (Process) obj;
        if (!Objects.equals(this.Id, other.Id)) {
            return false;
        }
        if (!Objects.equals(this.IP, other.IP)) {
            return false;
        }
        if (this.PORT != other.PORT) {
            return false;
        }
        return true;
    }

    private Process(RegistryCard card) {
        this.dateFormater = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        this.Id = card.Id;
        this.PORT = card.port;
        this.IP = card.ip;
        isSyncher = false;
    }

    private final boolean isSyncher;

    public Process(String id, String ip, int port, String syncIP, int syncPORT) {
        this.dateFormater = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        this.Id = id;
        this.file = "Processes.txt";
        createFile();
        list = new ArrayList<>();
        listACK = new HashSet<>();
        inCS = false;
        this.IP = ip;
        this.PORT = port;
        listProcess = new ArrayList<>();
        connector = new UDPConnector(port);
        
        System.out.println("Process " + this.Id + " running at: " + this.IP + ":" + this.PORT);
        addTarget(this);
        startListening(3);
        
            //sync with already started processes
        randomWait();
        if (syncIP != null && syncPORT != 0) {
            isSyncher = false;
            syncProcess(this, syncIP, syncPORT);
        } else {
            isSyncher = true;
        }
        write();
        //this.receiveRequest();
    }

    public Message request() {
        //Agregar request a su misma cola
        Message req = new Message(Id, REQUEST);
        if(!list.contains(req))
            list.add(req);
        listProcess.stream().filter(
                (p) -> (!this.Id.equals(p.Id))
        ).forEach(
                (p) -> {
                    this.sendRequest(req, p.PORT, p.IP);
                    randomWait();
                }
        );
        return req;
    }
    private final SimpleDateFormat dateFormater;

    /**
     * Este metodo recibe requests mientras la bandera este activada y las
     * almacena en una cola de requests
     * @param remoteObject
     */
    public synchronized void receiveRequest(Object remoteObject) {
       new Thread( () -> {
        Message receivedRequest = (Message) remoteObject;
        System.out.println("[INFO: ] request received : "+receivedRequest+" in " + this.Id);
        //Acción dependiendo del tipo de mensaje
        switch (receivedRequest.type) {
            //Solicitud de acceso a la CS
            case REQUEST:
                if(!list.contains(receivedRequest))
                    list.add(receivedRequest);
                System.out.println("["+this.Id+" ACTION] REQUEST received from "
                        + receivedRequest.process );
                sendResponse(receivedRequest);
                break;
            case ACK:
                System.out.println("["+this.Id+" ACTION] ACK received from "
                        + receivedRequest.process );
                saveACK(receivedRequest);
                break;
            //Mensaje de release de la CS
            case RELEASE:
                System.out.println("["+this.Id+" ACTION] RELEASE received from "
                        + receivedRequest.process );
                getRelease(receivedRequest);
                break;
            }
        }).start();
    }

    @Override
    public String toString() {
        return "Process{" + "Id=" + Id + ", IP=" + IP + ", PORT=" + PORT + '}';
    }

    public synchronized void sendRequest(Message req, int port, String ip) {
        new Thread(() -> {
            System.out.println("Sending Request: "+req+" to "+ip+":"+port);
            connector.send(req, port, ip);
        }).start();
    }
    private void createFile(){
        Path f = Paths.get(file);
        if(!Files.exists(f))
            try {
                Files.createFile(f);
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, "File creation failed", ex);
        }
    }
    /**
     * *
     * Actualizar un determinado archivo
     *
     */
    public void update() {
        write();

    }

    /**
     * *
     * Lee e imprime todas las lineas de un determinado archivo
     *
     */
    public void read() {
        Path f = Paths.get(file);
        try  {
            for (String line:Files.readAllLines(f)) {
                System.out.println(line + "\n");
            }
        }catch(IOException e){
            
        }

    }

    /**
     * *
     * Escribir un timestamp en determinado archivo
     *
     */
    public void write() {
        Path f = Paths.get(file);
        try (
                BufferedWriter writter = Files.newBufferedWriter(f, StandardOpenOption.APPEND);) {
            String text = "[" + Id + "]" + "[" + dateFormater.format(new Date()) + "]\n";
            writter.append(text);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void registerProcess(Object remoteObject) {
        if (listening) {
            new Thread(() -> {
                System.out.println("[ " + Id + " ACTION: ] Registry Card received, processing...");
                RegistryCard card = (RegistryCard) remoteObject;
                Process receivedProcess = cardToProcess(card);
                if (!listProcess.contains(receivedProcess) && (listProcess.size() <= waitfor)) {
                    addTarget(receivedProcess);
                    System.out.println("[ " + Id + " ][" + dateFormater.format(new Date()) + "] Process " + receivedProcess.Id + " added to work group");
                    System.out.println("# of processes in the group: " + listProcess.size() + " waiting for: " + waitfor);
                    if (isSyncher) {
                        syncGroup(cloneList(listProcess), receivedProcess.IP, receivedProcess.PORT);
                    }
                }
                if (!(listProcess.size() < waitfor)) {
                    System.out.println("# of processes quota reached: " + listProcess.size() + " stop waiting for processes");
                    stopListening();                    
                        System.out.print("Ready to start: ");
//                        
                        randomWait(4);
                        requestAccessToCS();
                        startMonitorDaemon();
                    
                    
                }
            }).start();
            Thread status=new Thread(() -> {
                try (
                            Scanner scanner = new Scanner(System.in);) {
            
                while (true) {
                    String output=scanner.nextLine().toUpperCase();
                    switch (output) {
                        case "STATUS":
                            System.out.println("ACK list: "+listACK);
                            System.out.println("Queue: "+list);
                            System.out.println("in CS: "+inCS);
                            System.out.println("processes: "+listProcess);
                            break;
                        case "RESUME":
                            resume();
                            break;
                    }
                       }
            }
            });
             status.setPriority(Thread.MAX_PRIORITY);
            status.start();
        } else {
            System.out.println("# of processes quota reached: " + listProcess.size() + " no more processes required");
        }
    }
    private void requestAccessToCS(){
        System.out.println("[ " + Id + " ][ACTION] Need access to Critic Section, sending request...");
        Message req1 = request();
        listProcess.stream().filter(
            (p) -> (!this.equals(p))
        ).forEach(
            (p) -> {
                sendRequest(req1, p.PORT, p.IP);
            }
        );
    }
    public void addTarget(Process p) {
        listProcess.add(p);
    }
    private int waitfor = 3;

    /**
     * Inicia el hilo de sincronización con otros procesos con un número
     * variable de procesos a registrar
     *
     * @param waitfor indica el número de procesos que debe contener la lista de
     * procesos, una vez que se alcanza este número se termina el hilo de
     * registro.
     */
    public void startListening(int waitfor) {
        this.waitfor = waitfor;
        startListening();
    }

    public void startListening() {
        listening = true;
        listenRequests();
    }

    public void stopListening() {
        listening = false;
    }

    private void listenRequests() {

        new Thread(() -> {

            System.out.println("[ " + Id + " ACTION: ] listening...");
            while (true) {
                Object remoteObject = connector.receive();
                //si se recibe un registro válido se continua el registro en un hilo por separado
                //para permitir al metodo seguir escuchando por más request
                if (remoteObject instanceof RegistryCard) {
                    registerProcess(remoteObject);
                } else if (remoteObject instanceof Message) {
                    receiveRequest(remoteObject);
                }

            }
        }).start();
    }

    private <T> List<T> cloneList(List<T> list) {
        List<T> cloneList = new ArrayList<>();
        list.forEach(p -> {
            cloneList.add(p);
        });
        return cloneList;
    }

    private synchronized void sendCard(Process process, String ip, int port) {
        RegistryCard card = processToCard(process);
        connector.send(card, port, ip);
    }

    private void syncProcess(Process process, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        sendCard(process, syncIP, syncPort);
    }

    private synchronized void syncGroup(List<Process> list, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        list.forEach(
                p -> {
                    listProcess.forEach(p2 -> {
                        randomWait();
                        if (!this.equals(p2)) {
                            sendCard(p, p2.IP, p2.PORT);
                        }
                    });

                }
        );
    }

    public static void main(String args[]) throws IOException {
        String processId;
        int port;
        InetAddress IP = InetAddress.getLocalHost();
        String syncIP;
        int syncPORT;
        if (args.length == 2) {
            try {
                processId = args[0].toUpperCase();
                port = Integer.parseInt(args[1]);
                syncIP = null;
                syncPORT = 0;
                Process process = new Process(processId, IP.getHostAddress(), port, syncIP, syncPORT);
            } catch (NumberFormatException e) {
                System.err.println("Indicar el identificador del proceso [P1,P2,P3]  el puerto [10000-10003] y el host de sincronizacion");
            }
            //Process p = registerProcess();
            //p.request();
        } else if (args.length == 4) {
            processId = args[0].toUpperCase();
            port = Integer.parseInt(args[1]);
            syncIP = args[2].toUpperCase();
            syncPORT = Integer.parseInt(args[3]);
            Process process = new Process(processId, IP.getHostAddress(), port, syncIP, syncPORT);
        } else {
            System.err.println("Indicar el identificador del proceso [P1,P2,P3]  el puerto [10000-10003] y el host de sincronizacion");

        }
    }

    /**
     * *
     * Enviar respuesta o agregar a la cola de requests
     *
     * @param receivedRequest
     */
    private synchronized void sendResponse(Message receivedRequest) {
        //Si no esta en la CS enviar mensaje ACK
        if (!this.inCS && notOtherACK()) {
            
            Message req = new Message(this.Id, ACK);
            
            String ip="";
            int port=-1;
            receivedRequest.ACKsent=true;
            //Buscar ip y puerto del proceso que envio el mensaje
            for (Process p : listProcess) {
                if (p.Id.equals(receivedRequest.process)) {
                    ip = p.IP;
                    port = p.PORT;
                    break;
                }
            }
            //Enviar ACK
            System.out.println("Send ACK from "
                        + this.Id + " to " + receivedRequest.process);
            this.sendRequest(req, port, ip);
        }
    }

    /**
     * *
     * Guardar ACK recibidos de los diferentes procesos y checar si todos los
     * ACK han sido recibidos para la solicitud que esta en top y que es del
     * proceso actual
     *
     * @param receivedRequest
     */
    private void saveACK(Message receivedRequest) {
        listACK.add(receivedRequest);
        //System.out.println("list of ack: "+listACK);
        if(!list.isEmpty()){
        Message topRequest =list.get(0);
       // System.out.println("[save ACK]request queue: "+list+" top: "+topRequest);
        //Si el top request no es el mismo proceso entonces no puede entrar
        //en la CS
        if (topRequest.process != this.Id) {
            return;
        }
        }
       // System.out.println("Este proceso si es el top de la cola. ACK# " +  listACK.size());
        //Todos los ACK han sido recibidos, el top en el query es el mismo 
        //proceso, por lo tanto se puede entrar en la CS
        if (listACK.size() >= listProcess.size()-1) {
            //quitar los ACK en el lista de ACK
            System.out.println("[INFO] all ACK received");
            listACK.clear();
            try {
                this.inCS = true;
                System.out.println("-----------------------------------------");
                System.out.println("-----------------------------------------");
                System.out.println("[INFO ]" + this.Id + " is entering Critic Section @ "+dateFormater.format(new Date()));
                goToCS();
                //quita su propia solicitud de su cola
                System.out.println("[INFO ]" + this.Id + " is leaving Critic Section @ "+dateFormater.format(new Date()));
                if(!list.isEmpty())
                    list.remove(0);
                sendRelease();
                this.inCS=false;
                sendPendingACK();
                System.out.println("-----------------------------------------");
                System.out.println("-----------------------------------------");
                randomWait();
                randomWait();
                requestAccessToCS();
                
            } catch (InterruptedException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * *
     * Ejecutar la CS.
     *
     * @throws InterruptedException
     */
    private void goToCS() throws InterruptedException {
        //write,update,read
//        int num = 1 + (int) (Math.random() * ((3 - 1) + 1));
        Random r=new Random();
        int num=r.nextInt(3)+1;
        switch (num) {
            case 1:
                System.out.println("[INFO] "+this.Id+" is reading the shared file");
                read();
                break;
            case 2:
                System.out.println("[INFO] "+this.Id+" is writing into the shared file");
                write();
                break;
            case 3:
                System.out.println("[INFO] "+this.Id+" is updating the shared file");
                update();
                break;
        }
        randomWait();
    }
    private static final Random random;
    static{random=new Random(800);}
    public static void randomWait(){
        int wait=random.nextInt(500)+300;
        try{
            Thread.sleep(wait);
        }catch(InterruptedException e){
            
        }
    }
    /**
     * *
     * Enviar mensaje de release a todos los procesos.
     *
     * @param topRequest
     */
    private void sendRelease() {
        String ip;
        int port;
        listProcess.stream().filter(
            p -> !(p.Id.equals(this.Id))
        ).forEach( 
            p->{
                System.out.println("Send Release from " + this.Id + " to " + p.Id);
                this.sendRequest(new Message(this.Id, RELEASE), p.PORT, p.IP);
            }
        );
//        for (Process p : listProcess) {
//            if(!this.Id.equals(p.Id)){
//                ip = p.IP;
//                port = p.PORT;
//                Message rel = new Message(this.Id, RELEASE);
//                //rel.setFirstMsg(topRequest);
//                System.out.println("Send Release from " + this.Id + " to " + p.Id);
//                this.sendRequest(rel, port, ip);
//            }
//        }
    }

    /**
     * Mensaje de release de parte del proceso que entro en la CS, este proceso
     * quitara el request en la cola de los procesos que reciban este mensaje.
     *
     * @param receivedRequest
     */
    private void getRelease(Message receivedRequest) {
        System.out.println("[ INFO ] Releasing: "+receivedRequest.process);
        if((!Objects.isNull(list))&& !list.isEmpty()){
           list.removeIf(
                   e->e.equals(receivedRequest)
                   );
        }
    }

    /**
     * *
     * Enviar ACK pendiendientes despues de salir de la CS.
     */
    private void sendPendingACK() {
        for (Message message : list) {
            if(!message.ACKsent){
                System.out.println("[ INFO ] Sending pending ACK to "+ message.process);
                sendResponse(message);
                message.ACKsent = true;
                break;
            }
        }
    }
    private void resume() {
        Message message=list.get(0);
        String ip="";
        int port=-1;
        if(!(this.Id.equals(message.process))){
            Message req = new Message(this.Id, ACK);
            for (Process p : listProcess) {
                        if (p.Id.equals(message.process)) {
                            ip = p.IP;
                            port = p.PORT;
                            break;
                        }
                    }
           
            this.sendRequest(req, port, ip);
        }
//        for (Message message : list) {
//            if(!message.ACKsent){
//                System.out.println("[ INFO ] Sending pending ACK to "+ message.process);
//                if (!this.inCS ) {
//            
//                    Message req = new Message(this.Id, ACK);
//
//                    String ip="";
//                    int port=-1;
//                    message.ACKsent=true;
//                    //Buscar ip y puerto del proceso que envio el mensaje
//                    for (Process p : listProcess) {
//                        if (p.Id.equals(message.process)) {
//                            ip = p.IP;
//                            port = p.PORT;
//                            break;
//                        }
//                    }
//            //Enviar ACK
//            System.out.println("Send ACK from "
//                        + this.Id + " to " + message.process);
//            this.sendRequest(req, port, ip);
//        }
//                message.ACKsent = true;
//                break;
//            }
//        }
    }
    private Process cardToProcess(RegistryCard card) {
        return new Process(card);
    }

    private RegistryCard processToCard(Process process) {
        return new RegistryCard(process);
    }

    private boolean notOtherACK() {
        for (Message message : list) 
            if(message.ACKsent)
                return false;
        return true;
    }

    private void startMonitorDaemon() {
        new Thread(
            ()->{
                while(true){
                    randomWait(5);
                        resume();
                }
            }
        ).start();
    }

    private void randomWait(int i) {
        for(;i>0;i--){
            randomWait();
        }
        
    }
}
