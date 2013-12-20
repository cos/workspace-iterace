package extra166y;

public class Ops {
	public interface ProcedureWithIndex<T> {
		public void op(int i, T b);
	}

	public interface Procedure<T> {
		public void op(T b);
	}

	public interface Generator<T> {
		public T op();
	}
}
