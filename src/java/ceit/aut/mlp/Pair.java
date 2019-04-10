package ceit.aut.mlp;

public class Pair<T> {
	public T first, second;

	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public String toString() {
		return '<' + first.toString() + ", " + second.toString() + '>';
	}
}
