public class ReturnValue extends RuntimeException {
    public final Object value;

    public ReturnValue(Object value) {
        this.value = value;
    }
}