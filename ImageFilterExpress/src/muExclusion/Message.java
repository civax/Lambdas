/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package muExclusion;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import net.Sendable;
import net.util.Clock;

/**
 *
 * @author mary
 */
public class Message implements Comparable<Message>, Sendable {

    /**
     * Id del proceso
     */
    String process;
    /**
     * *
     * Fecha en la que se realizo el request
     */
    Date date;
    /**
     * *
     * Clock del objeto message
     */
    private Clock clock;
    /**
     * *
     * Tipo de mensaje que se esta transmitiendo, puede ser de 3 tipos
     * R=Request, solicitud de acceso a la CS ACK=Reply,Mensaje de libre acceso
     * a la CS Release=Mensaje de release de la CS
     */
    String type;
    /**
     * *
     * Status del mensaje, usado para saber si se envio el ACK, cuando el
     * proceso estuvo en la CS y no fue posible enviar la ACK, el status se
     * mantendra en R
     */
    boolean ACKsent;

    public Message(String process, String type,Clock clock) {
        this.process = process;
//        this.date = new Date();
        this.type = type;
        this.ACKsent = false;
        this.clock = new Clock(clock.getTime());
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.process);
        //hash = 23 * hash + Objects.hashCode(this.date);
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
        final Message other = (Message) obj;
        if (!Objects.equals(this.process, other.process)) {
            return false;
        }
//        if (!Objects.equals(this.date, other.date)) {
//            return false;
//        }
        return true;
    }  

    public int getClock() {
        return clock.getTime();
    }

    public void setClock(int clock) {
        this.clock.receiveAction(clock);
    }

    @Override
    public String toString() {
        return  "(process=" + process + ", type=" + type + ", ACK status=" + 
                ACKsent+" date: "+clock.getTime() +")";//+ ", firstMsg=" + firstMsg + '}';
    }

    /**
     * *
     * Clonar el mensaje completamente, todas sus propiedades son transferidas
     * al nuevo objeto.
     *
     * @param msg
     */
    public Message(Message msg) {
        this.clock = msg.clock;
        this.date = msg.date;
        this.process = msg.process;
        this.ACKsent = msg.ACKsent;
        this.type = msg.type;
    //    this.firstMsg = msg.firstMsg;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isACKsent() {
        return ACKsent;
    }

    public void setACKsent(boolean ACKsent) {
        this.ACKsent = ACKsent;
    }

    @Override
    public int compareTo(Message t) {
        return this.clock.compareTo(t.clock);
//        return this.date.compareTo(t.date);
    }

    
}
