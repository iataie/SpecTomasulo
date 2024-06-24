/**
 * Ismail Ataie
 * CDA5155-Spring 2018
 * Assignment 4
 * Speculative Dynamic Scheduled Pipeline Simulator
 * copy all java files, with java extention, into some folder like f1.
 * copy trace.config to that folder, as well.
 * $cd <f1 path>
 * compile code:
 *             $javac *.java
 * execute code:
 *             $ java SpecTomasulo <inputfile >outputfile 
 */

import java.util.HashMap;


//A Class for all registers from R0 to R31 and from F0 to F31 and their values
public class RegisterFile {
    HashMap<Register,Integer> Registers;

    //Constructor of class for initialization of properties
    public RegisterFile() {
        Registers= new HashMap<Register,Integer>();
        for(Register r: Register.values())
        {
            Registers.put(r,-1);
            
        }
    }
    public int getRegVal(Register rP)
    {
        return Registers.get(rP);
    }
    public void setRegVal(Register rP,int value)
    { Registers.put(rP,value);
    }
    //method for printing state of Registers
    public void print()
    {
        for(Register r: Register.values())
            if(Registers.get(r)>=0)
            System.out.print( r +":"+ Registers.get(r)+" ");
        System.out.println("");
    }
    
}
