package top.integer.framework.core.ioc;

public class Pair {
    String key;
    Object value;

    public Pair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
