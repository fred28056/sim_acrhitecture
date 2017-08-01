import java.util.ArrayList;
import java.util.HashSet;

public class Cache_FIFO {
//	private String evict[] = new String[MainClass.associativity];
	private ArrayList<String> evict = new ArrayList<String>();
	private String array[] = new String[8];
	private static HashSet<String> way = new HashSet<String>();
//	private int i = 0;
	private int out = 0;

	public boolean add(String block) {
//		evict_add(block);
		evict.add(block);
		array[0] = block;
//		System.out.println(array[0]);
		return way.add(block);
	}

	public boolean contains(String block) {
		return way.contains(block);
	}

	private boolean remove(String block) {
		evict.remove(0);
		return way.remove(block);
	}

	public int size() {
		System.out.println(way.size());
		return way.size();
	}

	public boolean evict_equals(String block) {
//		return evict[out].equals(block);
		return evict.get(out).equals(block);
	}

	public void evict_add(String block) {
//		evict[i] = block;
		evict.add(block);
//		i++;
//		if (i >= MainClass.associativity) {
//			i = 0;
//		}

		return;
	}

	public void evict_remove() {
//		out++;
//		int k = 0;
//		while(!evict_equals(evict[k])){
//			k++;
//		}
		//way.forEach(System.out::println);
//		System.out.println(way.size());
//		System.out.println(evict.size());
		
		remove(evict.get(0));
		
		//System.out.println(evict.get(out));
		//System.out.println(evict.get());
//		for(int k = 0; k < evict.size() ; k++){
//			if(evict_equals(evict.get(k))){
//				remove(evict.get(out));
//				out++;
//				if(out >= evict.size()) out = 0;
//
//			}
//		}
		
//		for(int k=0; k < MainClass.associativity ; k++) {
//			if(evict_equals(evict[k])) {
//				remove(evict[k]);
//				out++;
//				if(out >= MainClass.associativity) out = 0;
//			}
//		}
		
		return;
	}
	
	public static Cache_FIFO[] init_cache(int sets) {
		Cache_FIFO[] cache = new Cache_FIFO[sets];
		for(int j = 0; j < sets; j++) {
			cache[j] = new Cache_FIFO();
//			cache[j].way = new HashSet<String> way();
			
		}
		
		return cache;
		
	}
	
	
}
