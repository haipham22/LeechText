package dark.leech.text.lua.api;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class CacheResponse
        implements Connection.Response {
    private byte[] data;

    public CacheResponse(Connection.Response response) {
        this.data = response.bodyAsBytes();
    }


    public URL url() {
        return null;
    }


    public Connection.Response url(URL url) {
        return null;
    }


    public Connection.Method method() {
        return null;
    }


    public Connection.Response method(Connection.Method method) {
        return null;
    }


    public String header(String name) {
        return null;
    }


    public List<String> headers(String name) {
        return null;
    }


    public Connection.Response header(String name, String value) {
        return null;
    }


    public Connection.Response addHeader(String name, String value) {
        return null;
    }


    public boolean hasHeader(String name) {
        return false;
    }


    public boolean hasHeaderWithValue(String name, String value) {
        return false;
    }


    public Connection.Response removeHeader(String name) {
        return null;
    }


    public Map<String, String> headers() {
        return null;
    }


    public Map<String, List<String>> multiHeaders() {
        return null;
    }


    public String cookie(String name) {
        return null;
    }


    public Connection.Response cookie(String name, String value) {
        return null;
    }


    public boolean hasCookie(String name) {
        return false;
    }


    public Connection.Response removeCookie(String name) {
        
        return null;
    }


    public Map<String, String> cookies() {
        
        return null;
    }


    public int statusCode() {
        
        return 0;
    }


    public String statusMessage() {
        
        return null;
    }


    public String charset() {
        
        return null;
    }


    public Connection.Response charset(String charset) {
        
        return null;
    }


    public String contentType() {
        
        return null;
    }


    public Document parse() throws IOException {
        
        return Jsoup.parse(new String(this.data, Charset.forName("UTF-8")));
    }


    public String body() {
        
        return new String(this.data, Charset.forName("UTF-8"));
    }


    public byte[] bodyAsBytes() {
        
        return this.data;
    }


    public Connection.Response bufferUp() {
        
        return null;
    }


    public BufferedInputStream bodyStream() {
        
        return null;
    }
}
