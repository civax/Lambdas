/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package muExclusion;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mary
 */
public class Coordinator {
    static List<Process> list;
    
    public static void main (String args[]){
        Process p1 =  new Process("1","Conf1.properties","localhost",1000);
        Process p2 =  new Process("2","Conf2.properties","localhost",1001);
        Process p3 =  new Process("3","Conf3.properties","localhost",1002);
        list =  new ArrayList<>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        Message req1 = p1.request();
        
        for (Process p : list) {
            if(!p.Id.equals(p1.Id))
                p1.sendRequest(req1, p.port, p.ip);
        }
    }
}


