package floetteroed.utilities;

import java.util.Iterator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <T>
 */
public class EmptyIterable<T> implements Iterable<T> {

	@Override
	public Iterator<T> iterator() {
		return new EmptyIterator<>();
	}

}
