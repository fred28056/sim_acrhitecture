import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

/* 
 * File_read
 * 
 * read_file(): Reads in trace file
 * 
 * parse(): parse trace addresses
 * 
 */

public class File_Read {
	// Globals
	public static String trace_array[]; // holds all strings from the trace file
	public static String address_list[]; // holds all address
	public static String access_list[]; // holds r|w
	public static int optimal_list[]; // holds optimal next access values
	public static int write_count = 0; // write counter
	public static int read_count = 0; // read counter

	/*
	 * read_file(String filename)
	 * 
	 * read in specified trace file
	 */
	public static void read_file(String fileName) {
		String file = fileName; // file to read in
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

			// Set all access values to infinite
			// -1 used to denote infinite next access
			if (Sim_cache.rep.equals("Optimal")) {
				optimal_list = new int[trace_array.length];
				Arrays.fill(optimal_list, -1);
			}

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

		if (Sim_cache.debug) {
			System.out.println(trace_array.length);
		}
	}

	/*
	 * parse()
	 * 
	 * parse through trace array and remove index, tag, and r|w
	 */
	public static void parse() {

		// temp array to hold strings
		String temp[] = new String[3];

		for (int i = 0; i < trace_array.length; i++) {
			// split string by whitespaces
			temp = trace_array[i].split("\\s+");
			// first element will be r|w
			access_list[i] = temp[0];
			// Second element will be tag address
			address_list[i] = temp[1];

			// update read and write counter respectively
			if (temp[0].equals("r")) {
				read_count++;
			}
			if (temp[0].equals("w")) {
				write_count++;
			}

			if (Sim_cache.debug) {
				System.out.println(access_list[i]);
			}
		}

	}

	/*
	 * optimal_parse()
	 * 
	 * read in traces backwards first time address is seen mark as infinite next
	 * address next time subtract last instruction # from current, that will be
	 * the next access value
	 */
	public static void optimal_parse() {
		// hash map to hold address and last instruction number
		HashMap<String, Integer> addresses = new HashMap<String, Integer>();

		// pase through all instruction
		for (int i = trace_array.length-1; i >= 0; i--) {
			// if it is in the hash map then it has been seen before.
			// next access is previous instruction # - current instruction #
			if (addresses.containsKey(address_list[i])) {
				optimal_list[i] = addresses.get(address_list[i]) - i;
				addresses.put(address_list[i], i);
				//if first time address is seen mark as infinite next access
			} else {
				optimal_list[i] = -1;
				addresses.put(address_list[i], i);
			}
		}
	}
}
