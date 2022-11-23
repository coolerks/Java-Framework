package top.integer.framework.web;

import java.lang.reflect.Method;

public class PathMapping {
    private RequestType requestType;
    private String path;
    private boolean placeHolder;

    @Override
    public String toString() {
        return "PathMapping{" +
                "requestType=" + requestType +
                ", path='" + path + '\'' +
                ", placeHolder=" + placeHolder +
                ", handlerMethod=" + handlerMethod +
                '}';
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(Method handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    private Method handlerMethod;

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(boolean placeHolder) {
        this.placeHolder = placeHolder;
    }
}
