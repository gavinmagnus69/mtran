public class ReturnValue extends RuntimeException {
    final Object value;
    public ReturnValue(Object value) {
        super(null, null, false, false); // No message, cause, suppression, or writable stack trace
        this.value = value;
    }
}
