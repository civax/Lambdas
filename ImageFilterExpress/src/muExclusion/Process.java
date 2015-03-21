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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
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
    List<Message> listACK;
    static List<Process> listProcess;
    boolean inCS;
    private static final String ACK="ACK";
    private static final String REL="REL";
    private static final String REQ="REQ";
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
        this.Id = card.Id;
        this.PORT = card.port;
        this.IP = card.ip;
        isSyncher = false;
    }

    private final boolean isSyncher;

    public Process(String id, String ip, int port, String syncIP, int syncPORT) {
        this.Id = id;
        this.file = file;
        list = new ArrayList();
        listACK = new ArrayList();
        inCS = false;
        this.IP = ip;
        this.PORT = port;
        listProcess = new ArrayList<>();
        connector = new UDPConnector(port);
        System.out.println("Process " + this.Id + " running at: " + this.IP + ":" + this.PORT);
        addTarget(this);
        startListening(3);
        try {
            //sync with already started processes
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (syncIP != null && syncPORT != 0) {
            isSyncher = false;
            syncProcess(this, syncIP, syncPORT);
        } else {
            isSyncher = true;
        }

        //this.receiveRequest();
    }

    public Message request() {
        //Agregar request a su misma cola
        Message req = new Message(Id, "R");
        list.add(req);
        listProcess.stream().filter(
                (p) -> (!this.Id.equals(p.Id))
        ).forEach(
                (p) -> {
                    this.sendRequest(req, p.PORT, p.IP);
                }
        );
        return req;
    }
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");

    /**
     * Este metodo recibe requests mientras la bandera este activada y las
     * almacena en una cola de requests
     */
    public synchronized void receiveRequest(Object remoteObject) {
//       
       new Thread( () -> {
//            System.out.println("[ACTION: ] waiting requests...");
//
//            Object remoteObject=connector.receive();
//            if(remoteObject instanceof Message)
//            {
        Message receivedRequest = (Message) remoteObject;
        System.out.println("[INFO: ] request received in " + this.Id);
        //Acción dependiendo del tipo de mensaje
        switch (receivedRequest.type) {
            //Solicitud de acceso a la CS
            case "R":
                System.out.println("Request from "
                        + receivedRequest.process + " to " + this.Id);
                list.add(receivedRequest);
                sendResponse(receivedRequest);
                break;
            //Mensaje de ACK de parte de los otros procesos
            case "ACK":
                System.out.println("ACK from "
                        + receivedRequest.process + " to " + this.Id);
                saveACK(receivedRequest);
                break;
            //Mensaje de release de la CS
            case "Release":
                System.out.println("Release from "
                        + receivedRequest.process + " to " + this.Id);
                getRelease(receivedRequest);
                break;
        }
//            }
        }).start();
    }

    @Override
    public String toString() {
        return "Process{" + "Id=" + Id + ", IP=" + IP + ", PORT=" + PORT + '}';
    }

    public void sendRequest(Message req, int port, String ip) {
        new Thread(() -> {
            connector.send(req, port, ip);
        }).start();
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

        try (
                Scanner reader = new Scanner(file); //PrintWriter writter = new PrintWriter(f.toFile())
                ) {
            String line = reader.nextLine();
            while (line != null) {
                line = reader.nextLine();
                System.out.println(line + "\n");
            }
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
            String text = "[" + Id + "]" + "[" + format.format(new Date()) + "]";
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
                    System.out.println("[ " + Id + " ][" + format.format(new Date()) + "] Process " + receivedProcess.Id + " added to work group");
                    System.out.println("# of processes in the group: " + listProcess.size() + " waiting for: " + waitfor);
                    if (isSyncher) {
                        syncGroup(cloneList(listProcess), receivedProcess.IP, receivedProcess.PORT);
                    }
                }
                if (!(listProcess.size() < waitfor)) {
                    System.out.println("# of processes quota reached: " + listProcess.size() + " stop waiting for processes");
                    stopListening();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try (
                            Scanner scanner = new Scanner(System.in);) {
                        System.out.print("Ready to start: ");
                        while (!(scanner.nextLine().toUpperCase()).equals("START")) {
                        }

                    }
                    System.out.println("[ " + Id + " ][" + format.format(new Date()) + "] Sending request...");
                    Message req1 = request();
                    listProcess.stream().filter(
                            (p) -> (!this.equals(p))
                    ).forEach(
                            (p) -> {
                                sendRequest(req1, p.PORT, p.IP);
                            }
                    );
                }
            }).start();
        } else {
            System.out.println("# of processes quota reached: " + listProcess.size() + " no more processes required");
        }
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

    private List<Process> cloneList(List<Process> list) {
        List<Process> cloneList = new ArrayList<>();
        list.forEach(p -> {
            cloneList.add(p);
        });
        return cloneList;
    }

    private void sendCard(Process process, String ip, int port) {
        RegistryCard card = processToCard(process);
        connector.send(card, port, ip);
    }

    private void syncProcess(Process process, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        sendCard(process, syncIP, syncPort);
    }

    private void syncGroup(List<Process> list, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        list.forEach(
                p -> {
                    listProcess.forEach(p2 -> {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                        }
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
            } catch (Exception e) {
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
    private void sendResponse(Message receivedRequest) {

        //Si no esta en la CS enviar mensaje ACK
        if (!this.inCS) {
            Message req = new Message(this.Id, "ACK");
            
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

        Message topRequest = new Message(list.get(0));

        //Si el top request no es el mismo proceso entonces no puede entrar
        //en la CS
        if (topRequest.process != this.Id) {
            return;
        }

        int num = listProcess.size();
        HashMap<String, Integer> listProcessTemp = new HashMap<>();
        //Buscar en la lista de ACK recibidas si se tienen todos los ACK
        //del top request en la lista.
        for ( int i=0; i< listACK.size();i++) {
            Message msg =  listACK.get(i);
            
            if(!listProcessTemp.containsKey(msg.process)) //&& topRequest.date.equals(msg.firstMsg.date))
                listProcessTemp.put(msg.process, i);
            
            //if(listProcessTemp.size()==listProcess.size())
              //  break;
            
        }

        //Todos los ACK han sido recibidos, el top en el query es el mismo 
        //proceso, por lo tanto se puede entrar en la CS
        if (listProcessTemp.size() == listProcess.size()) {
            //quitar los ACK en el lista de ACK
            listACK.clear();
            try {
                this.inCS = true;
                goToCS();
                //quita su propia solicitud de su cola
                list.remove(0);
                sendRelease();
                this.inCS=false;
                sendPendingACK();
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
        int num = 1 + (int) (Math.random() * ((3 - 1) + 1));
        switch (num) {
            case 1:
                read();
                break;
            case 2:
                write();
                break;
            case 3:
                update();
                break;
        }
        Thread.sleep(1000);
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
        for (Process p : listProcess) {
            ip = p.IP;
            port = p.PORT;
            Message rel = new Message(this.Id, "Release");
            //rel.setFirstMsg(topRequest);
            this.sendRequest(rel, port, ip);
        }
    }

    /**
     * Mensaje de release de parte del proceso que entro en la CS, este proceso
     * quitara el request en la cola de los procesos que reciban este mensaje.
     *
     * @param receivedRequest
     */
    private void getRelease(Message receivedRequest) {
        int i;
        for (i = 0; i < list.size(); i++) {
            Message msg =  list.get(i);
            if(msg.process.equals(receivedRequest.process))
                //msg.date.equals(receivedRequest.firstMsg.date) &&;
            break;
        }

        list.remove(i);
    }

    /**
     * *
     * Enviar ACK pendiendientes despues de salir de la CS.
     */
    private void sendPendingACK() {
        for (Message message : list) {
            if(!message.ACKsent){
                sendResponse(message);
                message.ACKsent = true;
                break;
            }
        }
    }

    private Process cardToProcess(RegistryCard card) {
        return new Process(card);
    }

    private RegistryCard processToCard(Process process) {
        return new RegistryCard(process);
    }
}
