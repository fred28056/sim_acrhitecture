import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * Cache
 * 
 * Object to hold contents and information of each Set in a Cache
 * Contains methods to add and remove items from a set as well as set management
 * 
 *Global Variables:
 * 
 * evict	Contains order of blocks to evict
 * set		contains all blocks in the set
 * dirty	contains blocks that are dirty for writeback on eviction
 * pseudo	contains pseudo tree bits
 * pseduo_branches	contains # of branches in pseduo tree
 * optimal_evict	eviction list for optimal replacement
*/
public class Cache {

	private ArrayList<String> evict;
	private HashSet<String> set;
	private HashSet<String> dirty;
	private int pseudo[];
	private int pseudo_branches;
	private Map<String, Integer> optimal_evict;

	public Cache() {
		this.evict = new ArrayList<String>();
		this.set = new HashSet<String>();
		this.dirty = new HashSet<String>();
		if (Sim_cache.associativity == 1) {
			this.pseudo = new int[1];
		} else {
			this.pseudo = new int[Sim_cache.associativity - 1];
		}
		Arrays.fill(pseudo, 1);
		this.pseudo_branches = (int) (Math.log(Sim_cache.associativity) / Math.log(2));
		this.optimal_evict = new HashMap<String, Integer>();
	}

	/*
	 * add(String block)
	 * 
	 * add specified block to the set updates eviction order depending on
	 * replacement policy
	 */

	public boolean add(String block) {

		if (Sim_cache.rep.equals("FIFO")) { // FIFO: add block to end of list
			evict.add(block);
		}
		if (Sim_cache.rep.equals("LRU")) { // LRU: add block to end of list and
											// remove from previous position if
											// applicable
			if (set.contains(block)) {
				evict.remove(block);
				evict.add(block);
			} else {
				evict.add(block);
			}
		}
		if (Sim_cache.rep.equals("Pseudo")) {
			if (evict.size() < Sim_cache.associativity) { // add block to
															// eviction list
				evict.add(block);
			}

			else { // updates pseudo binary tree
				int evict_index = evict.indexOf(block);
				int line;
				if (Sim_cache.associativity == 1) {
					line = 0;
				} else {
					line = (int) ((Sim_cache.associativity + evict_index - 2) / 2);
				}

				while (line > -1) {
					if ((evict_index % 2) == 0) {
						pseudo[line] = 0;
					} else {
						pseudo[line] = 1;
					}

					line++;
					line = line / 2;
					line--;
				}

			}
		}

		return set.add(block); // add block to set for easy searching
	}

	/*
	 * contains(String block)
	 * 
	 * search the set for the specified block
	 */

	public boolean contains(String block) {
		return set.contains(block);
	}

	/*
	 * remove()
	 * 
	 * Remove block from set that is up for eviction Block argument is address
	 * to be removed for specified replacement method
	 */
	public boolean remove() {
		String block = "";
		int index = 0;
		if (Sim_cache.rep.equals("FIFO")) { // First in will be in index 0
			index = 0;

		}
		if (Sim_cache.rep.equals("LRU")) { // Least recently used will be in
											// index 0
			index = 0;
		}
		if (Sim_cache.rep.equals("Pseudo")) { // Traverse tree for LRU
			int pseudo_replace = 0;
			for (int line = 0; line < (pseudo_branches - 1); line++) {
				pseudo_replace = pseudo_replace * 2 + pseudo[pseudo_replace];
			}
			index = pseudo_replace; // Pass index of block for eviction

		}

		block = evict.get(index);
		evict.remove(block); // Evict block
		if (dirty_contains(block)) { // Remove from dirty list
			dirty_remove(block);
		}
		return set.remove(block); // Evict from set
	}

	/*
	 * optimal_add(String address, int i)
	 * 
	 * adds the specified address and value of number of instrucions till the
	 * address is seen again into an optimal eviction list
	 * 
	 */

	public void optimal_add(String address, int i) {
		optimal_evict.put(address, i); // add to optimal eviction list
		set.add(address); // add to set
	}

	/*
	 * optimal_remove()
	 * 
	 * remove block with the highest value for next address
	 * 
	 */

	public boolean optimal_remove() {
		String replace = "";
		int next = 0;
		boolean dirty = false;
		// put optimal eviction list into temporary set
		Set<Entry<String, Integer>> optimal_set = optimal_evict.entrySet();
		// Make an iterator of set to search it
		Iterator<Entry<String, Integer>> i = optimal_set.iterator();

		while (i.hasNext()) {
			Map.Entry<String, Integer> current = (Map.Entry<String, Integer>) i.next();

			// -1 means infinite access later, mark for replacement if next
			// access
			// is infinite
			if (current.getValue() == -1) {
				next = -1;
				replace = current.getKey();

			} else if ((next >= 0) & (next > current.getValue())) {
				// finds highest next access value and marks for replacement
				// Only marks for replacement if the next block has a higher
				// access value than the current block
				next = current.getValue();
				replace = current.getKey();
			}
		}

		if (dirty_contains(replace)) { // Remove from dirty list
			dirty_remove(replace);
			dirty = true;
		}

		optimal_evict.remove(replace); // remove from eviction list and set
		set.remove(replace);
		return dirty;
	}

	public int size() { // Size of set, used for debugging
		return set.size();
	}

	public boolean evict_equals(String block) { // Compare block to next up for
												// eviction
		return evict.get(0).equals(block);
	}

	public void evict_add(String block) { // add block to eviction list
		evict.add(block);
		return;
	}

	public boolean dirty_add(String block) { // add address to dirty list
		return dirty.add(block);
	}

	public boolean dirty_remove(String block) { // remove from dirty list
		return dirty.remove(block);
	}

	public boolean dirty_contains(String block) { // check dirty list for
													// address
		return dirty.contains(block);
	}

	public boolean write_back() { // if dirty list contains block up for
									// eviction, return true
		return dirty_contains(evict.get(0));
	}

	public static Cache[] init_cache(int sets) { // initialize the cache array
													// with cache objects
		Cache[] temp = new Cache[sets];
		for (int j = 0; j < sets; j++) {
			temp[j] = new Cache();
		}
		return temp;
	}

}
