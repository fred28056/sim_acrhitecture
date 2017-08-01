/* Luke Friedrich
 * ECE 463
 * Project 1: Cache Simulator
 * 
 * Contains Main and sim() function to simulate cache
 * 
 */

public class Sim_cache {

	// Global variables

	public static boolean debug = false;

	public static String file; // trace file
	public static int cache_size; // L1 Cache size
	public static int associativity; // L1 associativity
	public static int blocksize; // block size
	public static String rep; // Replacement policy
	public static String inclusion; // Inclusion policy

	public static Cache_Values cache_stats; // object that has stats of the
											// cache
	public static Cache[] cache_list; // Array that maintains all blocks in
										// cache

	public static void main(String[] args) {
		// Parse through the arguments
		blocksize = Integer.parseInt(args[0]);
		cache_size = Integer.parseInt(args[1]);
		associativity = Integer.parseInt(args[2]);
		int l2_size = Integer.parseInt(args[3]);
		int l2_assoc = Integer.parseInt(args[4]);
		if (args[5].equals("0")) {
			rep = "LRU";
		}
		if (args[5].equals("1")) {
			rep = "FIFO";
		}
		if (args[5].equals("2")) {
			rep = "Pseudo";
		}
		if (args[5].equals("3")) {
			rep = "Optimal";
		}
		if (args[6].equals("0")) {
			inclusion = "non-inclusive";
		}
		if (args[6].equals("1")) {
			inclusion = "inclusive";
		}
		if (args[6].equals("2")) {
			inclusion = "exclusive";
		}
		file = args[7];
		
		cache_size = 8192;
		//cache_size = 8192 * 2;
		//cache_size = 8192 * 4;
		//cache_size = 8192 * 8;
		associativity = 4;
		file = "MCF.t";

		cache_stats = new Cache_Values(cache_size, associativity, blocksize, 0, 0, 0, 0, 0);
		// compute stats of the cache
		cache_stats = Cache_Values.cache_compute(cache_stats);
		cache_list = Cache.init_cache(cache_stats.sets);

		// Read in trace file
		File_Read.read_file(file);
		// Parse the trace file for tag address, index, and r|w
		File_Read.parse();
		// If optimal replacement, parse through trace and computer next access
		// values
		if (rep.contains("Optimal")) {
			File_Read.optimal_parse();
		}

		System.out.println("===== Simulator configuration =====");
		System.out.println("BLOCKSIZE:             " + blocksize);
		System.out.println("L1_SIZE:               " + cache_size);
		System.out.println("L1_ASSOC:              " + associativity);
		System.out.println("L2_SIZE:               " + l2_size);
		System.out.println("L2_ASSOC:              " + l2_assoc);
		System.out.println("REPLACEMENT POLICY:    " + rep);
		System.out.println("INCLUSION PROPERTY:    " + inclusion);
		System.out.println("trace_file:            " + file);
		System.out.println("===== Simulation results (raw) =====");


		sim();

	}

	/*
	 * sim()
	 * 
	 * simulates cache and uses methods in the Cache object to update sets in
	 * the cache
	 * 
	 */

	public static void sim() {
		int hit = 0;
		int r_miss = 0;
		int w_miss = 0;
		int w_back = 0;

		for (int i = 0; i < File_Read.address_list.length; i++) {
			// Cycle through all trace elements
			Long long_index = (Long.valueOf(File_Read.address_list[i], 16)
					// mask out the index from address
					& cache_stats.index_mask) >> cache_stats.offset;

			// Mask out address from index
			String address = Long.toHexString(Long.valueOf(File_Read.address_list[i], 16) & cache_stats.mask);

			// convert index to int so it can be used to index the cache array
			int index = long_index.intValue();

			if ((cache_list[index].contains(address))) {
				// if set contains address mark as hit
				if (debug) {
					hit++;
				}
				
				if (rep.equals("LRU")) {
					cache_list[index].add(address); // Update LRU eviction list
				}

				if (rep.equals("Optimal")) {
					cache_list[index].optimal_add(address, File_Read.optimal_list[i]);
				}
				// If a write update dirty address list
				if (File_Read.access_list[i].equals("w")) {

					cache_list[index].dirty_add(address);
				}

			} else if (cache_list[index].size() < associativity) {
				// add address if unfilled ways in set

				if (rep.equals("Optimal")) {
					cache_list[index].optimal_add(address, File_Read.optimal_list[i]);
				} else {
					cache_list[index].add(address);
				}

				if (File_Read.access_list[i].equals("r")) { // mark as miss for
															// first miss
					r_miss++;
				}
				if (File_Read.access_list[i].equals("w")) {
					w_miss++;
					// if a write mark as dirty
					cache_list[index].dirty_add(address);
				}

			} else {
				// if not hit or compulsory miss then mark miss and replace a
				// block
				// check for read or write miss and if write mark as dirty
				if (File_Read.access_list[i].equals("r")) {
					r_miss++;
				}
				if (File_Read.access_list[i].equals("w")) {
					w_miss++;
					cache_list[index].dirty_add(address);
				}

				// optimal requires special write back handling
				if (!rep.equals("Optimal")) {
					// check if a write back and increment
					if (cache_list[index].write_back()) {
						w_back++;
					}
				}

				// Optimal requires its own removal function
				if (rep.equals("Optimal")) {
					// optimal_remove checks for write back during its check for
					// replacement
					if (cache_list[index].optimal_remove()) {
						// if optimal_remove returns true then increment
						// write back counter
						w_back++;
					}
				} else {
					// remove for other replacement policies
					cache_list[index].remove();
				}

				// add address to set
				if (rep.equals("Optimal")) {
					cache_list[index].optimal_add(address, File_Read.optimal_list[i]);
				} else {
					// handles other replacement policies
					cache_list[index].add(address);
				}
			}

		}

		System.out.println("a. number of L1 reads:        " + File_Read.read_count);
		System.out.println("b. number of L1 read misses:  " + r_miss);
		System.out.println("c. number of L1 writes:       " + File_Read.write_count);
		System.out.println("d. number of L1 write misses: " + w_miss);
		System.out.println("e. L1 miss rate:              "
				+ ((float) (r_miss + w_miss) / (float) (File_Read.read_count + File_Read.write_count)));
		System.out.println("f. number of L1 write backs:  " + w_back);
		System.out.println("g. number of L2 reads:        0");
		System.out.println("h. number of L2 read misses:  0");
		System.out.println("i. number of L2 writes:       0");
		System.out.println("j. number of L2 write misses: 0");
		System.out.println("k. L2 miss rate:              0");
		System.out.println("l. number of L2 write backs:  0");
		System.out.println("m. total memory traffic:      " + (r_miss + w_miss + w_back));
	}
}
