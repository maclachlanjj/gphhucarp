package gputils.FileOutput;

import gphhucarp.decisionprocess.routingpolicy.DualTree_MakespanLimiter;
import gphhucarp.decisionprocess.routingpolicy.GPRolloutRoutingPolicy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public abstract class FileOutput {
    protected static String fileName;
    protected static PrintStream ps;
    protected static PrintStream original_ps;
    protected static int seed;

    public static void initPrintStream(){
        if(GPRolloutRoutingPolicy.recordData || DualTree_MakespanLimiter.recordData) {
            try{
                ps = new PrintStream(new FileOutputStream(fileName + seed + ".csv"));
                original_ps = System.out;
            } catch(IOException e) {
                throw new Error(e);
            }
        }
    }

    public static void setParams(int s, String n){
        seed = s;
        fileName = n;
    }

    public static void typeToLog(String s){
        System.out.println(s);
    }

    public static void setup(){
        System.setOut(ps);
    }

    public static void finish(){
        System.setOut(original_ps);
    }

    public static void exit(){
        if(ps != null) {
            ps.flush();
            ps.close();
        }
    }
}
