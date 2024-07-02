package org.github.gestalt.config.test.classes;

import jakarta.annotation.Nullable;

public class DBInfoNullableGetter {
    private int port;

    private String uri;
    private String password;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Nullable
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
