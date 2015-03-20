/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package muExclusion;

import java.io.Serializable;
import java.util.Date;
import net.Sendable;
import net.util.Clock;

/**
 *
 * @author mary
 */
public class Message implements Comparable<Message>, Sendable, Serializable {

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
    Clock clock;
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
    String status;
    /**
     * *
     * Mensaje original de solicitud de acceso a la CS
     */
    Message firstMsg;

    public Message(String process, String type) {
        this.process = process;
        this.date = new Date();
        this.type = type;
        this.status = "R";
        this.clock = new Clock(1);
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
    public int compareTo(Message req) {
        //Son iguales
        if (this.process.equals(req.process)
                && this.date.compareTo(req.date) == 0) {
            return 1;
        }
        return 0;
    }

    public int getClock() {
        return clock.getTime();
    }

    public void setClock(int clock) {
        this.clock.receiveAction(clock);
    }

    @Override
    public String toString() {
        return "Message{" + "process=" + process + ", type=" + type + ", status=" + status + ", firstMsg=" + firstMsg + '}';
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
        this.status = msg.status;
        this.type = msg.type;
        this.firstMsg = msg.firstMsg;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * *
     * Obtener el mensaje original con el que se solicito el acceso a la CS
     *
     * @return
     */
    public Message getFirstMsg() {
        return firstMsg;
    }

    /**
     * *
     * Guardar el mensaje original con el que se solicito el acceso a la CS
     *
     * @param firstMsg
     */
    public void setFirstMsg(Message firstMsg) {
        this.firstMsg = firstMsg;
    }
}
