package floetteroed.utilities;

import java.util.Iterator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <T>
 */
public class EmptyIterator<T> implements Iterator<T> {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public T next() {
		return null;
	}

}
