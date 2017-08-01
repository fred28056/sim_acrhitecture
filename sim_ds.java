package proj3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class sim_ds {

	static int index;	//index of instruction to fetch
	static int width;	//register width
	static int rob_size;
	static int cycle;	//cycle count
	static int iq_size;	
	static ArrayList<Integer> de = new ArrayList<Integer>();	//registers
	static ArrayList<Integer> rn = new ArrayList<Integer>();
	static ArrayList<Integer> rr = new ArrayList<Integer>();
	static ArrayList<Integer> di = new ArrayList<Integer>();
	static ArrayList<Integer> iq = new ArrayList<Integer>();
	static ArrayList<Integer> ex = new ArrayList<Integer>();
	static ArrayList<Integer> wb = new ArrayList<Integer>();
	static ArrayList<Integer> rt = new ArrayList<Integer>();
	static ArrayList<Integer> rob_list = new ArrayList<Integer>();	//rob register
	public static boolean rob[]; //s
	public static int src1[]; //
	public static boolean src1_rob[]; //
	public static int src2[]; //
	public static boolean src2_rob[]; //
	public static int dest[]; //
	public static int address_list[]; // holds all address
	public static int op[]; // holds op type
	public static int latency[]; // holds latency timer
	public static String trace_array[]; // holds all strings from the trace file

	public static void main(String[] args) {
		
		String file = args[5];	//Read in arguments
		width = Integer.valueOf(args[2]);
		rob_size = Integer.valueOf(args[0]);
		iq_size = Integer.valueOf(args[1]);
		
		index = 0;	//initialize globals
		cycle = 0;
		rob = new boolean[99];

		File_Read(file);	//read trace file

		while (advance_cycle())	//cycle through pipeline till no more instructions
			;

		System.out.println("# === Simulator Command =========");
		System.out.println("# ./sim_ds" + args[0] + " "+ args[1] + " "+ args[2] + " "+ args[3] + " "+ args[4] + " "+ args[5]);
		System.out.println("# === Processor Configuration ===");
		System.out.println("# ROB_SIZE 	= "+rob_size);
		System.out.println("# IQ_SIZE  	= "+iq_size);
		System.out.println("# WIDTH    	= "+ width);
		System.out.println("# CACHE_SIZE 	= "+args[3]);
		System.out.println("# PREFETCHING	= "+args[4]);
		System.out.println("# === Simulation Results ========");
		System.out.println("# Dynamic Instruction Count      = "+ trace_array.length);
		System.out.println("# Cycles                         = "+ cycle);
		System.out.print("# Instructions Per Cycle (IPC)   = ");
		System.out.printf( "%.2f",((float) trace_array.length)/(float) cycle );
	}

	// Retire up to WIDTH consecutive “ready” instructions from the head of
	// the ROB.
	public static void retire() {
		for (int i = rt.size() - 1; i > -1; i--) {	//parse through all instructions waiting to retire
			if (dest[rt.get(i)] > -1) {
				rob[dest[rt.get(i)]] = false;	//change flag to remove from rob and allow depedent instructions to execute
				
				for(int k = 0; k < rob_list.size(); k++) {	//remove from rob list
					if(rob_list.get(k) == dest[rt.get(i)]) {
						rob_list.remove(k);
					}
				}
			}
			rt.remove(i); //remove from retire list
		}

	}

	// Process the writeback bundle in WB: For each instruction in WB, mark
	// the instruction as “ready” in its entry in the ROB.
	public static void writeback() {
		for (int i = wb.size() - 1; i > -1; i--) {	//move from writeback to retire routine
			rt.add(wb.get(i));
			wb.remove(i);
		}

	}

	// From the execute_list, check for instructions that are finishing
	// execution this cycle, and:
	// 1) Remove the instruction from the execute_list.
	// 2) Add the instruction to WB.
	// 3) Wakeup dependent instructions (set their source operand ready
	// flags) in the IQ, DI (dispatch bundle), and RR (the register-read
	// bundle).
	public static void execute() {
		int ex_index = 0;
		for (int i = 0; i < ex.size(); i++) {

			ex_index = ex.get(i);	//get instructions counter and decrement
			latency[ex_index]--;

			if (latency[ex_index] < 1) {	//if counter is 0 move to writeback and free up dependencies

				for (int x = 0; x < iq.size(); x++) {

					if (dest[ex_index] == src1[iq.get(x)]) {
						src1_rob[iq.get(x)] = false;
					}
					if (dest[ex_index] == src2[iq.get(x)]) {
						src2_rob[iq.get(x)] = false;
					}
					if ((src1_rob[iq.get(x)] == false) & (src2_rob[iq.get(x)] == false)) {
						ex.add(iq.get(x));
						iq.remove(x);
					}
				}

				wb.add(ex.get(i));
				ex.remove(i);

			}
		}

	}

	// Issue up to WIDTH oldest instructions from the IQ. (One approach to
	// implement oldest-first issuing is to make multiple passes through
	// the IQ, each time finding the next oldest ready instruction and then
	// issuing it. One way to annotate the age of an instruction is to
	// assign an incrementing sequence number to each instruction as it is
	// fetched from the trace file.)
	// To issue an instruction:
	// 1) Remove the instruction from the IQ.
	// 2) Add the instruction to the execute_list. Set a timer for
	// the instruction in the execute_list that will allow you to
	// model its execution latency.
	public static void issue() {

		int iq_index = 0;
		for (int i = 0; i < iq.size(); i++) {
			iq_index = iq.get(i);
			//check if instruction can be executed
			
			if (src1[iq_index] > -1) {	//if not dependent on previous instruction prevent deadlock if source register same as destination register
				if (rob[src1[iq_index]] == false) {
					src1_rob[iq_index] = false;
				}
				
				if(dest[iq_index] == src1[iq_index]) {
					src1_rob[iq_index] = false;
				}
				
			}
			if (src2[iq_index] > -1) {
				if (rob[src2[iq_index]] == false) {
					src2_rob[iq_index] = false;
				}
				
				if(dest[iq_index] == src2[iq_index]) {
					src2_rob[iq_index] = false;
				}
				
			}

			if (!(src1_rob[iq_index] | src2_rob[iq_index])) {	//check if dependent instructions completed
				ex.add(iq.get(i));	//move to execution
				iq.remove(i);
			}
		}

	}

	// If DI contains a dispatch bundle:
	// If the number of free IQ entries is less than the size of the
	// dispatch bundle in DI, then do nothing. If the number of free IQ
	// entries is greater than or equal to the size of the dispatch bundle
	// in DI, then dispatch all instructions from DI to the IQ.
	public static void dispatch() {
		if (iq.size() < iq_size) {	//if iq can hold more instruction move into iq
			for (int i = di.size() - 1; i > -1; i--) {
				iq.add(di.get(i));
				di.remove(i);
			}
		}
	}

	// If RR contains a register-read bundle:
	// If DI is not empty (cannot accept a new dispatch bundle), then do
	// nothing. If DI is empty (can accept a new dispatch bundle), then
	// process (see below) the register-read bundle and advance it from RR
	// to DI.
	//
	// How to process the register-read bundle:
	// Since values are not explicitly modeled, the sole purpose of the
	// Register Read stage is to ascertain the readiness of the renamed
	// source operands. Apply your learning from the class lectures/notes
	// on this topic.
	//
	// Also take care that producers in their last cycle of execution
	// wakeup dependent operands not just in the IQ, but also in two other
	// stages including RegRead()(this is required to avoid deadlock). See
	// Execute() description above.
	public static void regRead() {
		if (di.size() == 0) {	//if di is empty move bundle
			for (int i = rr.size() - 1; i > -1; i--) {
				di.add(rr.get(i));
				rr.remove(i);
			}
		}
	}

	// If RN contains a rename bundle:
	// If either RR is not empty (cannot accept a new register-read bundle)
	// or the ROB does not have enough free entries to accept the entire
	// rename bundle, then do nothing.
	// If RR is empty (can accept a new register-read bundle) and the ROB
	// has enough free entries to accept the entire rename bundle, then
	// process (see below) the rename bundle and advance it from RN to RR.
	//
	// How to process the rename bundle:
	// Apply your learning from the class lectures/notes on the steps for
	// renaming:
	// (1) Allocate an entry in the ROB for the instruction,
	// (2) Rename its source registers, and
	// (3) Rename its destination register (if it has one).
	// Note that the rename bundle must be renamed in program order.
	// Fortunately, the instructions in the rename bundle are in program
	// order).
	public static void rename() {
		int rob_index = 0;
		if ((rr.size() == 0) && (iq.size() < (rob_size - width))) {	//if rob can hold bundle rename and add to rob
			for (int i = rn.size() - 1; i > -1; i--) {
				rr.add(rn.get(i));	//move to regread
				rn.remove(i);
			}

			for (int i = 0; i < rr.size(); i++) {
				rob_index = rr.get(i);

				if (src1[rob_index] > -1) {	//set flag for rename
					if (rob[src1[rob_index]]) {
						src1_rob[rob_index] = true;

					}
				}
				if (src2[rob_index] > -1) {	//set flag for rename
					if (rob[src2[rob_index]]) {
						src2_rob[rob_index] = true;
					}
				}

				if (dest[rob_index] > -1) {	//set flag for rename
					int r = dest[rob_index];
					rob[r] = true;
					rob_list.add(dest[rob_index]);
				}
			}
		}
	}

	// If DE contains a decode bundle:
	// If RN is not empty (cannot accept a new rename bundle), then do
	// nothing. If RN is empty (can accept a new rename bundle), then
	// advance the decode bundle from DE to RN.
	public static void decode() {
		if (rn.size() == 0) {	//if rename empty move bundle into rename
			for (int i = de.size() - 1; i > -1; i--) {
				rn.add(de.get(i));
				de.remove(i);

			}

		}
	}

	// Do nothing if instruction cache is perfect (CACHE_SIZE=0) and either
	// (1) there are no more instructions in the trace file or
	// (2) DE is not empty (cannot accept a new decode bundle).
	//
	// If there are more instructions in the trace file and if DE is empty
	// (can accept a new decode bundle), then fetch up to WIDTH
	// instructions from the trace file into DE. Fewer than WIDTH
	// instructions will be fetched only if the trace file has fewer than
	// WIDTH instructions left.
	//
	// If instruction cache is imperfect or next-line prefetcher is
	// enabled, in addition to the above operations, adjust the timer to
	// model fetch latency when necessary.
	public static void fetch() {
		//fetch next instruction
		if ((de.size() == 0) & (index < trace_array.length)) {
			if ((index + width) >= trace_array.length) {
				for (int k = 0; k < (trace_array.length-index); k++) {	//special case if instructions left is less than bundle size
					de.add(address_list[index]);
					System.out.println(index + " fu{"+op[index]+"} src{" + src1[index] + "," + src2[index]+"} des{" +dest[index]+ "} FE{"+cycle+",1}"+ " DE{"+ (cycle+1) +",1}"+ " RN{"+ (cycle+2) +",1}"+ " RR{"+ (cycle+3) +",1}"+ " DI{"+ (cycle+4) +",1}"+ " IS{"+ (cycle+5) +",1}"+ " EX{"+ (cycle+6) +",1}" + " WB{"+ (cycle+7) +",1}"+ " RT{"+ (cycle+8) +",1}"); 
					index++;
				}

			} else {
				for (int i = 0; i < width; i++) {	//create bundle
					de.add(address_list[index]);
					System.out.println(index + " fu{"+op[index]+"} src{" + src1[index] + "," + src2[index]+"} des{" +dest[index]+ "} FE{"+cycle+",1}"+ " DE{"+ (cycle+1) +",1}"+ " RN{"+ (cycle+2) +",1}"+ " RR{"+ (cycle+3) +",1}"+ " DI{"+ (cycle+4) +",1}"+ " IS{"+ (cycle+5) +",1}"+ " EX{"+ (cycle+6) +",1}" + " WB{"+ (cycle+7) +",1}"+ " RT{"+ (cycle+8) +",1}");
					index++;
				}
			}
		}

	}

	// while(advance_cycle());
	// advance_cycle() performs several functions.
	// (1) It advances the simulator cycle.
	// (2) When it becomes known that the pipeline is empty AND the trace
	// is depleted, the function returns “false” to terminate the loop.
	public static boolean advance_cycle() {
		fetch();
		decode();
		rename();
		regRead();
		dispatch();
		issue();
		execute();
		writeback();
		retire();
		cycle++; //advance cycle counts
		if (index < trace_array.length) {	//check if still instructions in pipeline
			return true;
		} else if (de.size() != 0) {
			return true;
		} else if (rn.size() != 0) {
			return true;
		} else if (rr.size() != 0) {
			return true;
		} else if (di.size() != 0) {
			return true;
		} else if (iq.size() != 0) {
			return true;
		} else if (ex.size() != 0) {
			return true;
		} else if (wb.size() != 0) {
			return true;
		} else if (rt.size() != 0) {
			return true;
		} else	//if not end while loop
			return false;

	}

	public static void File_Read(String file) {

		String line = null; // current line being read in
		String temp_array[] = new String[3000001]; // hold all strings in trace
													// file

		try { // try to read in file
			int i = 0;

			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// check if eof, if not store in array
			while ((line = bufferedReader.readLine()) != null) {
				temp_array[i] = line;
				i++;
			}
			bufferedReader.close(); // close at eof

			trace_array = new String[i]; // intiate arrays
			address_list = new int[trace_array.length];
			op = new int[trace_array.length];
			dest = new int[trace_array.length];
			src1 = new int[trace_array.length];
			src1_rob = new boolean[trace_array.length];
			src2 = new int[trace_array.length];
			src2_rob = new boolean[trace_array.length];
			latency = new int[trace_array.length];

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file");
		} catch (IOException ex) {
			System.out.println("Error reading file");
			ex.printStackTrace();
		}

		// fill trace array from file array
		for (int i = 0; i < trace_array.length; i++) {
			trace_array[i] = temp_array[i];
		}

		parse();

	}

	public static void parse() {

		// temp array to hold strings
		String temp[] = new String[5];

		// trace_array.length
		for (int i = 0; i < trace_array.length; i++) {
			// split string by whitespaces
			temp = trace_array[i].split("\\s+");
			// Fifth element is source 2
			src2[i] = Integer.valueOf(temp[4]);
			// Fourth element is source 1
			src1[i] = Integer.valueOf(temp[3]);
			// Third element is destination register
			dest[i] = Integer.valueOf(temp[2]);
			// Second element will be operation
			op[i] = Integer.valueOf(temp[1]);
			// first element will be tag address
			address_list[i] = i;

			if (op[i] == 0) {
				latency[i] = 1;
			}
			if (op[i] == 1) {
				latency[i] = 2;
			}
			if (op[i] == 2) {
				latency[i] = 5;
			}
		}
	}

}
