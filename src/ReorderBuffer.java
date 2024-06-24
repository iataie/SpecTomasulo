/**
 * Ismail Ataie
 * CDA5155-Spring 2018
 * Assignment 4
 * Speculative Dynamic Scheduled Pipeline Simulator
 * copy all java files, with java extention, into some folder like f1.
 * copy trace.config to that folder, as well.
 * $cd <f1 path>
 * compile code: $javac *.java
 * execute code: $ java SpecTomasulo <inputfile >outputfile 
 */
//A class for definition of a circular reorder buffer and functionalities.
public class ReorderBuffer {

    public ReorderBufferElement buff[];  //main storage of buffer
    int head = 0;                        //head of buffer   
    int rear = 0;                        //rear of buffer 
    int buffsize;                        //maimum size of buffer
    int buffLen = 0;                     //current lenght of buffer   

    //constructor of class to initialize class
    public ReorderBuffer(int bSize) {
        buffsize = bSize;
        buff = new ReorderBufferElement[bSize];
        for (int i = 0; i < bSize; i++) {
            buff[i] = new ReorderBufferElement();
        }
    }

    //A method to check emptiness of buffer
    public boolean isempty() {
        return buffLen == 0;
    }

    //A method for checking fullness of buffer
    public boolean isFull() {
        return buffLen == buffsize;
    }

    //A method to insert an element into buffer
    //Accept an instruction and destination register and index of rob and 
    //inserts into buffer these values
    public int insert(InstType instP, Register destRegP, String InstStrP,
            int indxIQP) {
        int oldrear = rear;
        if (!isFull()) {

            buff[rear].inst = instP;
            buff[rear].destReg = destRegP;
            buff[rear].InstStr = InstStrP;
            buff[rear].ready = false;

            buff[rear].indxIQ = indxIQP;

            rear = (rear + 1) % buffsize;
            buffLen++;
            return oldrear;

        } else {
            System.out.println("Error: Overflow Buffer");
            return -1;
        }
    }

    //A method to remove an element from buffer
    //return index of removed item
    public int remove() {
        int res;
        if (!isempty()) {
            res = head;
            buffLen--;
            head = (head + 1) % buffsize;
            return res;
        } else {
            System.out.println("Error: Underflow Buffer");
            return -1;
        }
    }

    public boolean getReady(int indx) {
        return buff[indx].ready;
    }

    //A method to check whether there is any store before some load instruction
    //in buffer. If there is that item, return true.
    public boolean isStoreBefore(int idxP, int memAddP) {
        boolean res = false;
        int current = idxP;
        while (current != head) {
            current = (current - 1 + buffsize) % buffsize;
            if (buff[current].isStoreInst() && buff[current].destMemAdd
                    == memAddP) {
                return true;
            }
        }
        return false;
    }

    //A method to return head of buffer. 
    public ReorderBufferElement getHead() {
        return buff[head];
    }
}

//A class for each element of ROB buffer,
class ReorderBufferElement {

    InstType inst;              //instruction
    Register destReg;           //destination of instruction
    int destValue;              //computed value of instruction 
    int destMemAdd;             //memory address of destination
    SourceOperand storeSource;  //source operand
    boolean ready;              // ready flag
    int issueTime;              //issue time of instruction
    int startExeTime;           //start time of instruction
    int endExeTime;             //end time of execution
    int memReadTime;            //memory read time
    int writeResultTime;        //writing time of instruction
    int writeCDBTime;           //time of writing result into CDB
    String InstStr;             //string of instruction
    int indxIQ;                 //index of instruction in Instruction queue

    //method to clear one elment of buffer for the next usage
    public void clear() {
        inst = null;
        destReg = null;
        destValue = 0;
        destMemAdd = -1;
        storeSource = null;
        ready = false;
        issueTime = 1000000;
        startExeTime = 1000000;
        endExeTime = 1000000;
        memReadTime = 1000000;
        writeResultTime = 1000000;
        writeCDBTime = 1000000;
        InstStr = null;
        indxIQ = -1;
    }

    //A method to check whether instruction is store instruction.
    //Accept an instruction and return true if it is a store instruction.
    public boolean isStoreInst() {
        if (inst == null) {
            return false;
        }
        switch (inst) {
            case SW:
            case SdotS:
                return true;
        }
        return false;
    }

    //A method to check whether instruction is branch instruction.
    //Accept an instruction and return true if it is a branch instruction.
    public boolean isBranchInst() {
        if (inst == null) {
            return false;
        }
        switch (inst) {
            case BEQ:
            case BNE:
                return true;
        }
        return false;
    }
}
