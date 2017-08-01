import java.io.*;
import java.util.Arrays;

/* Luke Friedrich
 * ECE 463
 * Project 2: Branch Predictor
 */

public class sim_bp {
	public static String address_list[]; // holds all address
	public static String access_list[]; // holds t|n
	public static String trace_array[]; // holds all strings from the trace file
	public static BTB[] btb;	//BTB cache
	public static int index_bits;	//size of index i
	public static int ib;	//size of BTB
	public static int assoc;	//associativity of BTB
	public static int bimodal_table[];	//Holds bimodal values
	public static int g_bits;	//size of gshare register
	public static int g_reg = 0;	//value of gshare register
	public static int correct = 0;	//correct predictions
	public static int wrong = 0;	//wrong predictions
	public static int btb_miss = 0;	//btb miss prediction
	public static String rep;	//branch policy
	public static int bp = 0;	//# of branches seen by branch predictor

	public static void main(String[] args) {
		rep = args[0];					//Store arguments
		String file = "trace_gcc.txt";
		if (rep.contains("bimodal")) {
			index_bits = Integer.valueOf(args[1]);
			ib = Integer.valueOf(args[2]);
			assoc = Integer.valueOf(args[3]);
			file = args[4];
		} else if (rep.contains("gshare")) {
			index_bits = Integer.valueOf(args[1]);
			g_bits = Integer.valueOf(args[2]);
			ib = Integer.valueOf(args[3]);
			assoc = Integer.valueOf(args[4]);
			file = args[5];
		}

		btb = BTB.init_btb((int) Math.pow(2, ib));	//initialize btb

		File_Read(file);	//read in trace

		long mask = 0;		//create mask for finding index
		for (int i = 0; i < (64 - (index_bits)); i++) {
			mask = mask << 1;
		}
		// last bits of mask are 1 to extract index
		for (int i = 0; i < (index_bits + 1); i++) {
			mask++;
			mask = mask << 1;
		}

		long btb_mask = 0;	//mask for btb
		for (int i = 0; i < (64 - (ib)); i++) {
			btb_mask = btb_mask << 1;
		}
		// last bits of mask are 1 to extract index
		for (int i = 0; i < (ib + 1); i++) {
			btb_mask++;
			btb_mask = btb_mask << 1;
		}

		for (int i = 0; i < address_list.length; i++) {		//loop through all traces
			String address = address_list[i];
			String access = access_list[i];

			int index = (int) ((Long.valueOf(address, 16) & (mask)) >> (2));	//get indexes
			int btb_index = (int) ((Long.valueOf(address, 16) & (btb_mask)) >> (2));

			if (ib != 0) {	//check btb if used
				if (btb[btb_index].contains(address)) {
					bp++;
					if (rep.contains("bimodal")) {
						Bimodal(address, access, index);
					} else if (rep.contains("gshare")) {
						Gshare(address, access, index);
					}
				}

				else {
					if (btb[btb_index].size() >= assoc) { // max number of PC's
															// in set
						btb[btb_index].remove(); 			// add new PC
						btb[btb_index].add(address);
					} else {
						btb[btb_index].add(address);
					}
					if (access.contains("t")) {
						btb_miss++;
					}

				}
			} else {	// do branch prediction if no btb
				bp++;
				if (rep.contains("bimodal")) {
					Bimodal(address, access, index);
				} else if (rep.contains("gshare")) {
					Gshare(address, access, index);
				}

			}

		}

		System.out.print("Command Line:\n./sim_bp ");	//output
		for (int i = 0; i < args.length; i++) {
			System.out.print(args[i] + " ");
		}
		System.out.println();
		if (ib != 0) {
			System.out.println("\nFinal BTB Tag Array Contants {valid, pc}:");
			for (int i = 0; i < btb.length; i++) {
				System.out.print("Set    " + i + ":    ");
				btb[i].print();
				System.out.println();
			}
		}
		if (rep.contains("bimodal")) {
			System.out.println("\nFinal Bimodal Table Contents:");
			for (int i = 0; i < bimodal_table.length; i++) {
				System.out.println("table[" + i + "] : " + bimodal_table[i]);
			}
		}
		if (rep.contains("gshare")) {
			System.out.println("\nFinal GShare Table Contents:");
			for (int i = 0; i < bimodal_table.length; i++) {
				System.out.println("table[" + i + "] : " + bimodal_table[i]);
			}
			System.out.println("\nFinal GHR Contents: 0x       " + Integer.toHexString(g_reg));
		}

		System.out.println("\nFinal Branch Predictor Statistics:");
		System.out.println("a. Number of branches: " + access_list.length);
		System.out.println("b. Number of predictions from the branch predictor: " + bp);
		System.out.println("c. Number of mispredictions from the branch predictor: " + wrong);
		System.out.println("d. Number of mispredictions from the BTB: " + btb_miss);
		System.out.print("e. Misprediction Rate: ");
		System.out.printf( "%.2f",(((float) (wrong + btb_miss) / (float) access_list.length))*100);
		System.out.println(" percent");

	}

	public static void Bimodal(String address, String access, int index) {	//bimodal prediction
		boolean branch = true;

		if (bimodal_table[index] >= 2) {	//check what to predict
			branch = true;
		} else
			branch = false;

		if (branch) {
			if (access.contains("t")) {	//predict t 
				correct++;	//correct prediction update table
				if (bimodal_table[index] < 3) {
					bimodal_table[index]++;
				}
			} else {	//predict t but it is n
				wrong++;	//wrong update table
				if (bimodal_table[index] > 0) {
					bimodal_table[index]--;
				}
			}
		} else {
			if (access.contains("n")) {
				correct++;
				if (bimodal_table[index] > 0) {
					bimodal_table[index]--;
				}
			} else {
				wrong++;
				if (bimodal_table[index] < 3) {
					bimodal_table[index]++;
				}
			}
		}

	}

	public static void Gshare(String address, String access, int index) {
		boolean branch = true;
		index = index ^ (g_reg << (index_bits - g_bits));	//compute g share index

		if (bimodal_table[index] >= 2) {
			branch = true;
		} else
			branch = false;

		if (branch) {	//same as bimodal
			if (access.contains("t")) {
				correct++;
				if (bimodal_table[index] < 3) {
					bimodal_table[index]++;
				}
			} else {
				wrong++;
				if (bimodal_table[index] > 0) {
					bimodal_table[index]--;
				}
			}
		} else {
			if (access.contains("n")) {
				correct++;
				if (bimodal_table[index] > 0) {
					bimodal_table[index]--;
				}
			} else {
				wrong++;
				if (bimodal_table[index] < 3) {
					bimodal_table[index]++;
				}
			}
		}

		g_reg = g_reg >> 1;		//update gshare register
		if (access.contains("t")) {
			g_reg = g_reg | (1 << (g_bits - 1));
		} else {
			g_reg = g_reg & ~(1 << (g_bits - 1));
		}

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
			address_list = new String[trace_array.length];
			access_list = new String[trace_array.length];
			bimodal_table = new int[(int) Math.pow(2, index_bits)];
			Arrays.fill(bimodal_table, 2);

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
		String temp[] = new String[3];

		for (int i = 0; i < trace_array.length; i++) {
			// split string by whitespaces
			temp = trace_array[i].split("\\s+");
			// Second element will be t|n
			access_list[i] = temp[1];
			// first element will be tag address
			address_list[i] = temp[0];
		}
	}

}
