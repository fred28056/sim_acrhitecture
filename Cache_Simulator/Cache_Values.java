
/*
 * Cache_values
 * 
 * contains stats of the cache.
 * used in all other classes
 * 
 * cache_compute computes the stats for the cache and stores them cache_values object
 */

public class Cache_Values {
	// Globals
	public int size; // size of l1 cache
	public int assoc; // associativity of l1 cache
	public int bsize; // blocksize
	public int sets; // # of sets
	public int index; // index width in bits
	public long index_mask; // index mask
	public int offset; // offset width in bits
	public long mask; // address mask

	public Cache_Values(int cache_size, int associativity, int block_size, int set_size, int index_width,
			long mask_index, int offset_width, long tag_mask) {
		size = cache_size;
		assoc = associativity;
		bsize = block_size;
		sets = set_size;
		index = index_width;
		index_mask = mask_index;
		offset = offset_width;
		mask = tag_mask;
	}

	public static Cache_Values cache_compute(Cache_Values values) {
		// compute # of sets
		values.sets = get_sets(values.size, values.bsize, values.assoc);
		// compute index width
		values.index = get_index_width(values.sets);
		// compute offset width
		values.offset = get_offset_width(values.bsize);
		// compute mask
		values.mask = mask_init(values.index, values.offset);
		// compute index mask
		values.index_mask = get_index_mask(values.index, values.offset);
		// return new cache values
		return values;
	}

	// # of sets is Size/ blocksize * associativity
	public static int get_sets(int size, int blocksize, int associativity) {
		int set = blocksize * associativity;
		set = (size - 1) / set + 1;

		return set;
	}

	// index width is log2(sets)
	public static int get_index_width(int sets) {
		int index_width = 0;

		index_width = (int) ((Math.log(sets)) / Math.log(2));

		return index_width;
	}

	// offset width is log2(block size)
	public static int get_offset_width(int blocksize) {
		int offset = 0;

		offset = (int) ((Math.log(blocksize)) / Math.log(2));

		return offset;
	}

	// create tag address mask from index width and offset width
	public static long mask_init(int index, int offset) {
		// initialize mask to 64 bits to hold max address length
		long mask = 0;
		// number of bits to mask is index width + offset width
		int mask_out = index + offset;
		// keep tag address
		for (int i = 0; i < (64 - mask_out); i++) {
			mask++;
			mask = mask << 1;
		}
		// last bits of mask are 0 to remove index and offset from address
		for (int i = 0; i < (mask_out - 1); i++) {
			mask = mask << 1;
		}

		return mask;
	}

	//create index mask to mask out tag and offset width
	private static long get_index_mask(int index, int offset) {
		long index_mask = 0;
		int mask_out = index + offset;
		
		for (int i = 0; i < (64 - mask_out); i++) {
			index_mask = index_mask << 1;
		}

		for (int i = 0; i < (index); i++) {
			index_mask++;
			index_mask = index_mask << 1;
		}
		for (int i = 0; i < (offset - 1); i++) {
			index_mask = index_mask << 1;
		}
		return index_mask;
	}

}
