/*
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

import java.io.Serializable;
import net.util.Clock;

/**
 * Esta clase abstrae la imagen que sera procesada
 */
public class Image implements Serializable{
    	
	private static final long serialVersionUID = 6482853459949281988L;
        private String message;
	private final Clock clock;
	private String id;
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        
	@Override
	public String toString() {
		return "Image [message=" + message + ", clock=" + clock + ", id="+ id + "]";
	}
        
        public Image(String message, int clock, String id) {
		super();
		this.message = message;
		this.clock = new Clock(clock);
		this.id = id;
	}
        
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public int getClock() {
		return clock.getTime();
	}
	public void setClock(int clock) {
		this.clock.sendAction();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
}
