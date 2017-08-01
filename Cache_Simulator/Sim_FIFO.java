
public class Sim_FIFO {

	// public static final Set<String> cache_list = new
	// HashSet<String>(Arrays.asList(new String[MainClass.cache_size]));

	// @SuppressWarnings("unchecked")
	// public static final Set<String>[] cache_list = new
	// Set[MainClass.cache_stats.sets];

	// public static String[][] c_list = new String
	// [MainClass.cache_stats.sets][MainClass.cache_stats.assoc];
	// public static ArrayList<HashSet<String>> c_list = new
	// ArrayList<HashSet<String>>();

	public static void fifo() {
		int hit = 0;
		int r_miss = 0;
		int w_miss = 0;
		// Cache_FIFO[] cache_list = MainClass.cache_array;
		long max_index = 0;

		for (int i = 0; i < File_Read.address_list.length; i++) {
			Long long_index = (Long.valueOf(File_Read.address_list[i], 16)
					& MainClass.cache_stats.index_mask) >> MainClass.cache_stats.offset;
			String address = Long.toHexString(Long.valueOf(File_Read.address_list[i], 16) & MainClass.cache_stats.mask);

			int index = long_index.intValue();
			// System.out.println(index);
			if (index > max_index) {
				max_index = index;
			}
			
			//System.out.println(i);

			// if (MainClass.debug) {
			// System.out.println(File_Read.address_list[i]);
			// }
			//
//			try {

				if ((MainClass.cache_list[index].contains(address)) & (MainClass.cache_list[index].size() > 0)) {
					hit++;

				} else if (MainClass.cache_list[index].size() < MainClass.associativity) {
					MainClass.cache_list[index].add(address);
					if (File_Read.access_list[i].equals("r")) {
						r_miss++;
					}
					if (File_Read.access_list[i].equals("w")) {
						w_miss++;
					}

				} else {
					System.out.println(address + "\n");
					System.out.println(MainClass.cache_list[index].contains(address));
					MainClass.cache_list[index].add(address);
					MainClass.cache_list[index].evict_remove();
					//MainClass.cache_list[index].add(address);

					if (File_Read.access_list[i].equals("r")) {
						r_miss++;
					}
					if (File_Read.access_list[i].equals("w")) {
						w_miss++;
					}

				}

//			} catch (NullPointerException ex) {
//				//MainClass.cache_list[index].add(address);
//				Cache_FIFO[] cache_array = MainClass.cache_list;
//				System.out.println("Null Pointer");
//				System.out.println("Index: " + index);
//				System.out.println("address: " + File_Read.address_list[i]);
//				System.out.println(" " + MainClass.cache_list[index]);
//				System.out.println(MainClass.cache_list[index].contains(address));
//				System.out.println("address: " + File_Read.address_list[i]);
//			}

		}

		// for (int i = 0; i < File_Read.address_list.length; i++) {
		// if (MainClass.debug) {
		// System.out.println(File_Read.address_list[i]);
		// }
		//
		// if (cache_list.contains(File_Read.address_list[i])) {
		// hit++;
		//
		// } else if (first < MainClass.cache_size) {
		// cache_list.add(File_Read.address_list[i]);
		// evict[k] = File_Read.address_list[i];
		// k++;
		// first++;
		// if (File_Read.access_list[i].equals("r")) {
		// r_miss++;
		// }
		// if (File_Read.access_list[i].equals("w")) {
		// w_miss++;
		// }
		//
		// } else {
		// cache_list.remove(evict[out]);
		// cache_list.add(File_Read.address_list[i]);
		// evict[k] = File_Read.address_list[i];
		// k++;
		// out++;
		//
		// if (File_Read.access_list[i].equals("r")) {
		// r_miss++;
		// }
		// if (File_Read.access_list[i].equals("w")) {
		// w_miss++;
		// }
		//
		// }
		//
		// }
		System.out.println("# of Hits: " + hit);
		System.out.println("# of Read Misses: " + r_miss);
		System.out.println("# of Write Misses: " + w_miss);
		System.out.println("Max index: " + max_index);
		// System.out.println(cache_list);
	}
}
