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
        Process p1 =  new Process("P1","localhost",1000,null,0);
        Process p2 =  new Process("P2","localhost",1001,null,0);
        Process p3 =  new Process("P3","localhost",1002,null,0);
        p1.addTarget(p2);
        p1.addTarget(p3);
        p2.addTarget(p1);
        p2.addTarget(p3);
        p3.addTarget(p1);
        p3.addTarget(p2);
        list =  new ArrayList<>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        Request req1 = p1.request();
        
        list.stream().filter((p) -> (!p.Id.equals(p1.Id))).forEach((p) -> {
            p1.sendRequest(req1, p.PORT, p.IP);
        });
    }
}


