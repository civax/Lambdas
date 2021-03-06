/**
 *
 *
 *
 * @author mary, carlos, lheredia
 *
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static muExclusion.Process.randomWait;
import net.UDPConnector;
import net.util.Clock;
import net.Sendable;

//Process class 
public class Process {
    //Declaracion de variables
    public String Id;
    public final String IP;
    public final int PORT;
    private UDPConnector connector;
    private String file;
    private boolean listening;
    private boolean inCS;
    private int waitfor;  //inicializar con startlistening para que sea diferente el valor
    private final SimpleDateFormat dateFormater;
    private final boolean isSyncher;
    private static final String ACK = "ACK";
    private static final String RELEASE = "RELEASE";
    private static final String REQUEST = "REQUEST";
    private static final PriorityQueue<Message> requestQueue;
    private static final HashMap<String,Integer> ACKmap;
    private static final List<Process> listProcess;
    private static final LinkedBlockingQueue<Sendable> inputBuffer;
    private static final List<Message> sentRequests;
    private static final Clock clock;
    private static final Random random;

    //Inicialización de variables static
    static {
        ACKmap = new HashMap<>();
        requestQueue = new PriorityQueue<>();
        listProcess = new ArrayList<>();
        inputBuffer = new LinkedBlockingQueue<>();
        clock = new Clock(0);
        random = new Random(2000);
        sentRequests = new ArrayList<>();
    }

    //Constructores de la clase Process 
    private Process(RegistryCard card) {
        this.dateFormater = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        this.Id = card.Id;
        this.PORT = card.port;
        this.IP = card.ip;
        isSyncher = false;
    }

    public Process(String id, String ip, int port, String syncIP, int syncPORT) {
        this.dateFormater = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        this.Id = id;
        this.file = "Processes.txt";
        createFile(this.file);
        createFile(this.Id+".log");
        inCS = false;
        this.IP = ip;
        this.PORT = port;
        connector = new UDPConnector(port);
        System.out.println("Process " + this.Id + " running at: " + this.IP + ":" + this.PORT);
        write("Process " + this.Id + " running at: " + this.IP + ":" + this.PORT);
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
    }

    //Metodos de control de hilos
    /**
     *
     * Inicia el hilo de sincronización con otros procesos con un número
     *
     * variable de procesos a registrar
     *
     *
     *
     * @param waitfor indica el número de procesos que debe contener la lista de
     *
     * procesos, una vez que se alcanza este número se termina el hilo de
     *
     * registro.
     *
     */
    public void startListening(int waitfor) {
        this.waitfor = waitfor;
        startListening();
    }

    /**
     *
     * Inicia el hilo de sincronización con otros procesos
     *
     */
    public void startListening() {
        listening = true;
        System.out.println("[ " + Id + " ACTION: ] listening...");
        write("[ " + Id + " ACTION: ] listening...");
        Producer producer = new Producer();
        Consumer consumer = new Consumer();
        Thread producerThread = new Thread(producer);
        Thread consumerThread = new Thread(consumer);
        producerThread.start();
        consumerThread.start();
    }

    /**
     *
     * Finaliza el hilo de sincronización con otros procesos al      *
     * actualizar el valor de listening a falso
     *
     */
    public void stopListening() {
        listening = false;
    }

    //Metodos Producer/Consumer
    /**
     *
     * Este metodo es utilizado por la clase Producer/Consumer      *
     * recibe el proceso y lo agrega al buffer
     *
     */
    public void produce() {
        //Recibe objeto y lo agrega al buffer(linkedlist)
        Sendable remoteObject = connector.receive();
        write("[ Offering to BUFFER ]"+inputBuffer);
        inputBuffer.offer(remoteObject);
    }
    /**
     *
     * Este metodo es utilizado por la clase Producer/Consumer      *
     * si el buffer no esta vacio obtiene el siguiente
     *
     * elemento a procesar
     *
     */
    public  void consume() {
            write("[ Polling BUFFER ]"+inputBuffer);
            Sendable tempRemoteObject = inputBuffer.poll();
            if (tempRemoteObject instanceof RegistryCard) {
                registerProcess(tempRemoteObject);
            } else if (tempRemoteObject instanceof Message) {
                receiveRequest(tempRemoteObject);
            }
    }
    /**
     *
     * Este metodo registra el proceso      *
     * @param remoteObject
     *
     */
    private void registerProcess(Object remoteObject) {
        if (true) {
            new Thread(() -> {
                System.out.println("[ " + Id + " ACTION: ] Registry Card received, processing...");
                write("[ " + Id + " ACTION: ] Registry Card received, processing...");
                RegistryCard card = (RegistryCard) remoteObject;
                Process receivedProcess = cardToProcess(card);
                if (!listProcess.contains(receivedProcess) && (listProcess.size() <= waitfor)) {
                    addTarget(receivedProcess);
                    System.out.println("[ " + Id + " ][" + dateFormater.format(new Date()) + "] Process " + receivedProcess.Id + " added to work group");
                    write("[ " + Id + " ][" + dateFormater.format(new Date()) + "] Process " + receivedProcess.Id + " added to work group");
                    System.out.println("# of processes in the group: " + listProcess.size() + " waiting for: " + waitfor);
                    write("# of processes in the group: " + listProcess.size() + " waiting for: " + waitfor);
                    if (isSyncher) {
                        syncGroup(cloneList(listProcess), receivedProcess.IP, receivedProcess.PORT);
                    }
                }
                if (!(listProcess.size() < waitfor)) {
                    System.out.println("# of processes quota reached: " + listProcess.size() + " stop waiting for processes");
                    write("# of processes quota reached: " + listProcess.size() + " stop waiting for processes");
                   
                    System.out.print("Ready to start: ");     
                    write("Ready to start: :D");
                   randomWait(4);
                    requestAccessToCS();
                 //   startMonitorDaemon();
                }
            }).start();
            Thread status = new Thread(() -> {
                try (
                        Scanner scanner = new Scanner(System.in);) {
                    while (true) {
                        String output = scanner.nextLine().toUpperCase();
                        switch (output) {
                            case "STATUS":
                                System.out.println("ACK list: " + ACKmap);
                                System.out.println("Queue: " + requestQueue);
                                System.out.println("in CS: " + inCS);
                                System.out.println("processes: " + listProcess);
                                break;
                            case "RESUME":
                                resume();
                                break;
                            case "RELEASE":
                                requestQueue.poll();
                                break;
                        }
                    }
                }
            });
            status.setPriority(Thread.MAX_PRIORITY);
            status.start();
        } else {
            System.out.println("# of processes quota reached: " + listProcess.size() + " no more processes required");
            write("# of processes quota reached: " + listProcess.size() + " no more processes required");
        }

    }

    /**
     *
     * Este metodo recibe requests mientras la bandera este activada y las
     *
     * almacena en una cola de requests
     *
     * @param remoteObject
     *
     */
    public void receiveRequest(Sendable remoteObject) {
        //dejar thread 
        new Thread(() -> {
            Message receivedRequest = (Message) remoteObject;
            System.out.println(this.requestQueue);
            System.out.println("[INFO: ] request received : " + receivedRequest + " in " + this.Id);
            write("[INFO: ] request received : " + receivedRequest + " in " + this.Id);
            
        //Acción dependiendo del tipo de mensaje
            getClock().receiveAction(receivedRequest.getClock().getTime());
            switch (receivedRequest.type) {
            //Solicitud de acceso a la CS
                case REQUEST:
                    this.requestQueue.offer(receivedRequest);
                    System.out.println("[" + this.Id + " ACTION] REQUEST received from "
                            + receivedRequest.process+" ("+receivedRequest+")");
                    write("[" + this.Id + " ACTION] REQUEST received from "
                            + receivedRequest.process);
                    sendResponse(receivedRequest);
                    break;
                case ACK:
                    System.out.println("[" + this.Id + " ACTION] ACK received from "
                            + receivedRequest.process);
                    write("[" + this.Id + " ACTION] ACK received from "
                            + receivedRequest.process);
                    saveACK(receivedRequest);
                    break;
            //Mensaje de release de la CS
                case RELEASE:
                    System.out.println("[" + this.Id + " ACTION] RELEASE received from "
                            + receivedRequest.process);
                    
                    write("[" + this.Id + " ACTION] RELEASE received from "
                            + receivedRequest.process);
                    if (!requestQueue.isEmpty()) {
                        requestQueue.poll();
                    }
                    break;
            }
        }).start();
    }

    //Metodos del algoritmo
    public Message request() {
        //Agregar request a su misma cola
        requestQueue.forEach(e -> getClock().receiveAction(e.getClock().getTime()));
        Message req = new Message(Id, REQUEST, getClock(),Id+":"+getClock().getTime());
        ACKmap.put(req.getID(), 0);
        requestQueue.offer(req);
        return req;
    }

    public  void sendRequest(Message req, int port, String ip) {
        new Thread(() -> {
            System.out.println("Sending message: " + req + " to " + ip + ":" + port);
            write("Sending message: " + req + " to " + ip + ":" + port);
            connector.send(req, port, ip);
        }).start();
    }

    private void requestAccessToCS() {
        System.out.println("[ " + Id + " ][ACTION] Need access to Critic Section, sending request...");
        write("[ " + Id + " ][ACTION] Need access to Critic Section, sending request...");
        Message req = request();
        listProcess.stream().filter(
                (p) -> (!this.equals(p))
        ).forEach(
                (p) -> {
                    sendRequest(req, p.PORT, p.IP);
                    randomWait();
                }
        );
    }

    public void addTarget(Process p) {
        listProcess.add(p);
    }

    private <T> List<T> cloneList(List<T> list) {
        List<T> cloneList = new ArrayList<>();
        list.forEach(p -> {
            cloneList.add(p);
        });
        return cloneList;
    }

    private  void sendCard(Process process, String ip, int port) {
        RegistryCard card = processToCard(process);
        getClock().sendAction();
        connector.send(card, port, ip);
    }

    private void syncProcess(Process process, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        write("Synching with: " + syncIP + ":" + syncPort);
        sendCard(process, syncIP, syncPort);
    }

    private  void syncGroup(List<Process> list, String syncIP, int syncPort) {
        System.out.println("Synching with: " + syncIP + ":" + syncPort);
        write("Synching with: " + syncIP + ":" + syncPort);
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

    /**
     *
     * *
     *
     * Enviar respuesta o agregar a la cola de requests
     *
     *
     *
     * @param receivedRequest
     *
     */
    private  void sendResponse(Message receivedRequest) {
        //Si no esta en la CS enviar mensaje ACK
        if (!this.inCS) {// && notOtherACK()) {
            Message req = new Message(this.Id, ACK, receivedRequest.getClock(),receivedRequest.getID());
            String ip = "";
            int port = -1;
            receivedRequest.ACKsent = true;
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
            write("Send ACK from "
                    + this.Id + " to " + receivedRequest.process+"("+receivedRequest+")");
            if(this.PORT!=port)
                this.sendRequest(req, port, ip);
        }
    }
    /**
     *
     * *
     *
     * Guardar ACK recibidos de los diferentes procesos y checar si todos los
     *
     * ACK han sido recibidos para la solicitud que esta en top y que es del
     *
     * proceso actual
     *
     *
     *
     * @param receivedRequest
     *
     */
    private void saveACK(Message receivedRequest) {
        Integer count=ACKmap.get(receivedRequest.getID());
        if (count!=null) {
            ACKmap.put(receivedRequest.getID(),count++);
        }
        //System.out.println("list of ack: "+listACK);
        if (!requestQueue.isEmpty()) {
            Message topRequest = requestQueue.peek();
           // System.out.println(topRequest);
       // System.out.println("[save ACK]request queue: "+list+" top: "+topRequest);
        //Si el top request no es el mismo proceso entonces no puede entrar
        //en la CS
            if (!topRequest.process.equals( this.Id)) {
                return;
            }
        } else {
            System.out.println("ERROR lista vacia de requests");
            write("ERROR lista vacia de requests D:");
            return;
        }
        if (ACKmap.get(receivedRequest.getID())>2) {
            //quitar los ACK en el lista de ACK
            System.out.println("[INFO] all ACK received for "+receivedRequest.getID());
            write("[INFO] all ACK received for "+receivedRequest.getID());
            criticSection();
            randomWait(3);
            requestAccessToCS();
        }
    }

    private void criticSection() {
        this.inCS = true;
        System.out.println("-----------------------------------------");
        System.out.println("-----------------------------------------");
        write("-----------------------------------------");
        System.out.println("[INFO ]" + this.Id + " is entering Critic Section @ " + dateFormater.format(new Date()));
        write("[INFO ]" + this.Id + " is entering Critic Section @ " + dateFormater.format(new Date()));
        goToCS();
        //quita su propia solicitud de su cola
        System.out.println("[INFO ]" + this.Id + " is leaving Critic Section @ " + dateFormater.format(new Date()));
        write("[INFO ]" + this.Id + " is leaving Critic Section @ " + dateFormater.format(new Date()));
        Message releasetmp;
        //if(!list.isEmpty())
        releasetmp = requestQueue.poll();
        sendRelease(releasetmp.getClock());
        this.inCS = false;
        sendPendingACK();
        System.out.println("-----------------------------------------");
        System.out.println("-----------------------------------------");
        write("-----------------------------------------");
    }

    /**
     *
     * *
     *
     * Ejecutar la CS.
     *
     *
     *
     * @throws InterruptedException
     *
     */
    private void goToCS() {
        //write,update,read
//        int num = 1 + (int) (Math.random() * ((3 - 1) + 1));
        Random r = new Random();
        int num = r.nextInt(3) + 1;
        switch (num) {
            case 1:
                System.out.println("[INFO] " + this.Id + " is reading the shared file");
                write("[INFO] " + this.Id + " is reading the shared file");
                read();
                break;
            case 2:
                System.out.println("[INFO] " + this.Id + " is writing into the shared file");
                write("[INFO] " + this.Id + " is writing into the shared file");
                write();
                break;
            case 3:
                System.out.println("[INFO] " + this.Id + " is updating the shared file");
                write("[INFO] " + this.Id + " is updating the shared file");
                update();
                break;
        }
        randomWait();
    }

    public static void randomWait() {
        int wait = random.nextInt(2000) + 500;
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            System.out.println("ERROR:" + e.getMessage());
            //write("ERROR:" + e.getMessage());
        }
    }

    private synchronized static  Clock getClock() {
        return clock;
    }

    /**
     *
     * *
     *
     * Enviar mensaje de release a todos los procesos.
     *
     *
     *
     * @param topRequest
     *
     */
    private void sendRelease(Clock clock) {
        String ip;
        int port;
        listProcess.stream().filter(
                p -> !(p.Id.equals(this.Id))
        ).forEach(
                p -> {
                    System.out.println("Send Release from " + this.Id + " to " + p.Id);
                    write("Send Release from " + this.Id + " to " + p.Id);
                    getClock().sendAction();
                    this.sendRequest(new Message(this.Id, RELEASE, clock,this.Id+":"+clock.getTime()), p.PORT, p.IP);
                }
        );
    }

    /**
     *
     * *
     *
     * Enviar ACK pendiendientes despues de salir de la CS.
     *
     */
    private void sendPendingACK() {
        for (Message message : requestQueue) {
            if (!message.ACKsent) {
                System.out.println("[ INFO ] Sending pending ACK to " + message.process);
                write("[ INFO ] Sending pending ACK to " + message.process);
                sendResponse(message);
                message.ACKsent = true;
                //break;
            }
        }
    }

    private void resume() {
        if (!requestQueue.isEmpty()) {
            Message message = requestQueue.peek();
            if ((message.process.equals(this.Id)) && (ACKmap.size() >= listProcess.size() - 1)) {
                System.out.println("[INFO] all ACK received");
                write("[INFO] all ACK received");
                criticSection();
                randomWait(3);
                requestAccessToCS();
            }
        }
    }

    private Process cardToProcess(RegistryCard card) {
        return new Process(card);
    }

    private RegistryCard processToCard(Process process) {
        return new RegistryCard(process);
    }

    /**
     *
     * Checar si ya se envio un ACK en el queue de mensajes, ya que no se puede
     *
     * enviar otro ACK hasta no recibir el release del proceso al que se le      *
     * envio el ACK
     *
     * @return false - Si se envio un ACK. true - No hay mensajes con ACK
     *
     */
    private boolean notOtherACK() {
        for (Message message : requestQueue) {
            if (message.ACKsent) {
                return false;
            }
        }
        return true;
    }

    private void startMonitorDaemon() {
        new Thread(
                () -> {
                    while (true) {
                        randomWait(10);
//                        resume();
                        Message curMessage = requestQueue.peek();
                        if (!Objects.isNull(curMessage) && curMessage.process.equals(this.Id)) {
                            sendResponse(curMessage);
                        }
                   // sendPendingACK();
                    }
                }
        ).start();
    }

    private void randomWait(int i) {
        for (; i > 0; i--) {
            randomWait();
        }
    }

    //Metodos para el manejo de archivos
    /**
     *
     * *
     *
     * Crea un determinado archivo
     *
     *
     *
     */
    private void createFile(String file) {
        Path f = Paths.get(file);
        if (!Files.exists(f)) {
            try {
                Files.createFile(f);
            } catch (IOException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, "File creation failed", ex);
            }
        }
        else{
            try {
                Files.delete(f);
                Files.createFile(f);
            } catch (IOException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, "File creation failed", ex);
            }
        }
            
    }

    /**
     *
     * *
     *
     * Actualizar un determinado archivo
     *
     *
     *
     */
    public void update() {
        write();
    }

    /**
     *
     * *
     *
     * Lee e imprime todas las lineas de un determinado archivo
     *
     *
     *
     */
    public void read() {
        Path f = Paths.get(file);
        try {
            for (String line : Files.readAllLines(f)) {
                System.out.println(line + "\n");
            }
        } catch (IOException e) {
        }
    }

    /**
     *
     * *
     *
     * Escribir un timestamp en determinado archivo
     *
     *
     *
     */
    public void write() {
        Path f = Paths.get(file);
        try (
                BufferedWriter writter = Files.newBufferedWriter(f, StandardOpenOption.APPEND);
                ) {
            String text = "[" + Id + "]" + "[" + dateFormater.format(new Date()) + "]\n";
            writter.append(text);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    /**
     * Escribir el log de cada proceso en el archivo respectivo
     * @param event 
     */
    public void write(String event) {
        Path f = Paths.get(this.Id + ".log");
        try (
                BufferedWriter writter = Files.newBufferedWriter(f, StandardOpenOption.APPEND);
                ) {
            String text = "[" + dateFormater.format(new Date()) + "]" + event + "\n";
            writter.append(text);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

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

    @Override
    public String toString() {
        return "Process{" + "Id=" + Id + ", IP=" + IP + ", PORT=" + PORT + '}';
    }

    //main() clase process
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

    //Clase Interna Producer
    class Producer implements Runnable {
        @Override
        public void run() {
            while (true) {
                produce();
            }
        }
    }

    //Clase Interna Consumer  
    class Consumer implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (!inputBuffer.isEmpty()) {
                    consume();
                }
                randomWait(2);
            }
        }
    }
}
