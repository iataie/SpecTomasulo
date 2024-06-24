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

//A class for storing instructions and fetching them to process them.
public class InstructionQueue {

    public QueueItem buff[];            //stoarge of instructions
    int pc = 0;                         //program counter
    int rear = 0;                       //last instruction in buffer
    int count = 0;                      //number of instructions in buffer    

    public InstructionQueue(int qSizeP) {
        buff = new QueueItem[qSizeP];
    }

    //A method to insert an instruction into buffer
    //Accept instruction and operands and destination
    public void insert(InstType opP, Register src1P, Register src2P,
            Register dstP,
            int memAddP, String memLblP, String instStrP) {
        if (buff.length > count) {
            count++;
            buff[rear]
                    = new QueueItem(opP, src1P, src2P, dstP, memAddP, memLblP,
                            instStrP);
            rear++;
        } else {
            System.out.println("Error: Instruction Queue is full.");
        }
    }

    //A  methhod to fetch next instruction from buffer.
    //return next instruction and increse Program Counter(PC).
    public QueueItem fetch() {
        if (pc < rear) {
            QueueItem result;
            result = buff[pc];
            pc++;
            return result;
        } else {
            System.out.println(
                    "Error: Instruction Queue is empty for fetch instruction");
            return null;
        }
    }

    public QueueItem currentInst() {
        return buff[pc];
    }

    public boolean isEmpty() {
        if (pc >= rear) {
            return true;
        }
        return false;
    }

    //A method to print instruction buffer and statistics of each instruction
    public void print() {
        System.out.println("                    Pipeline Simulation");
        System.out.println(
                "-----------------------------------------------------------");
        System.out
                .println("                                      Memory Writes");
        System.out.println(
                "     Instruction      Issues Executes  Read  Result Commits");
        System.out.println(
                "--------------------- ------ -------- ------ ------ -------");
        for (int i = 0; i < count; i++) {
            buff[i].print();
        }
    }

    //A method to calculate true delay of simulation
    //calculate and return true dealy
    public int trueDepDelay() {
        int res = 0;
        for (int i = 0; i < count; i++) {
            res += (buff[i].startExeTime - buff[i].issueTime) - 1;
        }
        return res;
    }
}
///////////////////////////////////////////////////////////////////////////////

//class for each item of queue
class QueueItem {

    InstType operation;     //instruction
    Register srcReg1;       //first operand    
    Register srcReg2;       //second operand
    Register destReg;       //destination
    int memAddress;         //memory address
    String memLabel;        //memory label
    String instStr;         //command string

    int issueTime;          //isuue time of instruction 
    int startExeTime;       //start time of instruction    
    int endExeTime;         // end of execution time
    int memTime;            //memory access time
    int writeCDBTime;       //write to CDB time
    int commitTime;         //commit time of instruction

    //Constructor of class to initialize class with its parameters
    public QueueItem(InstType operation, Register srcReg1, Register srcReg2,
            Register destReg, int memAddress, String memLabel, String instStr) {
        this.operation = operation;
        this.srcReg1 = srcReg1;
        this.srcReg2 = srcReg2;
        this.destReg = destReg;
        this.memAddress = memAddress;
        this.memLabel = memLabel;
        this.instStr = instStr;
    }

    public void updateStat(int issueTimeP, int startExeTimeP, int endExeTimeP,
            int memTimeP, int writeCDBTimeP, int commitTimeP) {
        issueTime = issueTimeP;
        startExeTime = startExeTimeP;
        endExeTime = endExeTimeP;
        memTime = memTimeP;
        writeCDBTime = writeCDBTimeP;
        commitTime = commitTimeP;
    }

    //A method to print current instruct and its statistics
    public void print() {
        String a;
        a = String.format("%-21s %6d %3d -%3d ", instStr, issueTime,
                startExeTime, endExeTime);
        System.out.print(a);
        if (memTime == 1000000 || memTime == 0) {
            a = String.format("%6s ", "");
        } else {
            a = String.format("%6d ", memTime);
        }

        System.out.print(a);

        if (writeCDBTime == 1000000 || writeCDBTime == 0) {
            a = String.format("%6s ", "");
        } else {
            a = String.format("%6d ", writeCDBTime);
        }

        System.out.print(a);

        a = String.format("%7d", commitTime);
        System.out.println(a);

    }

}
