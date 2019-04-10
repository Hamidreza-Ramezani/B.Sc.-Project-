package ceit.aut.mlp;

public class MlpIOException extends Exception {
	private static final long serialVersionUID = 4638071305437867428L;

	public MlpIOException() {
	}

	public MlpIOException(String detailMessage) {
		super(detailMessage);
	}

	public MlpIOException(Throwable throwable) {
		super(throwable);
	}

	public MlpIOException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
