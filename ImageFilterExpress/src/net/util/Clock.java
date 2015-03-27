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
package net.util;

import java.io.Serializable;

public class Clock implements Serializable,Comparable<Clock>{

    @Override
    public String toString() {
        return "Clock{" + "time=" + time + '}';
    }
        /**
         * contador interno
         */
 	private int time; 
        /**
         * Constructor
         */
	public Clock(){	}
	public Clock(int initTime){
            this.time=initTime;
        }
        /**
         *  Metodo getter del campo time, obtiene su valor actual
         * @return el tiempo actual del reloj
         */
	public int getTime(){
		return time;
	}
        /**
         * metodo tick tack, incrementa el contador de tiempo en uno
         */
	public void tick(){
		time++;
	}
        /**
         * Se ejecuta durante la accion de envio de mensaje
         */
	public void sendAction(){
		tick();	
	}
        /**
         * se ejecuta durante la accion de recepcion de mensaje
         * @param incommingTime Es el tiempo del reloj externo
         */
	public void receiveAction( int incommingTime){
		time = max(time, incommingTime);
		tick();
	}
	/**
         * Obtiene el valor maximo de dos relojes dados
         */
	private int max(int a, int b){
		return Math.max(a, b);
	}   

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.time;
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
        final Clock other = (Clock) obj;
        if (this.time != other.time) {
            return false;
        }
        return true;
    }
        
    @Override
    public int compareTo(Clock o) {
        return this.getTime()-o.getTime();
    }
}
