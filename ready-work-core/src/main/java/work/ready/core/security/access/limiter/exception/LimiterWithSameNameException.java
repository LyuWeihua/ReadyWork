package work.ready.core.security.access.limiter.exception;

public class LimiterWithSameNameException extends IllegalArgumentException {
  private static final long serialVersionUID = -327233779708979738L;

  private static final String ERROR_MESSAGE = "Some limits have the same name";

  public LimiterWithSameNameException() {
    super(ERROR_MESSAGE);
  }

  public LimiterWithSameNameException(String message) {
    super(ERROR_MESSAGE + " : " + message);
  }
}
