/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package muExclusion;

import java.util.Date;
import net.Sendable;
import net.util.Clock;

/**
 *
 * @author mary
 */
public class Message implements Comparable<Message>, Sendable{

    
        String process;
        Date date;
        Clock clock;
        String type;
        
        public Message(String process, String type){
            this.process = process;
            this.date = new Date();
            this.type = type;
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
            if(this.process.equals(req.process) && 
                    this.date.compareTo(req.date)==0)
                return 1;
            return 0;
        }
        
        public int getClock() {
		return clock.getTime();
	}
        
	public void setClock(int clock) {
		this.clock.receiveAction(clock);
	}
        
    }
