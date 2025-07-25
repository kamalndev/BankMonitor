package thekamaln.monitorproject.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CookieStore {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<SerializableCookie>>() {}.getType();

    public static void save(WebDriver driver, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Set<Cookie> cookies = driver.manage().getCookies();
        List<SerializableCookie> list = cookies.stream()
                .map(SerializableCookie::from)
                .collect(Collectors.toList());
        Files.writeString(file, GSON.toJson(list));
    }

    public static void load(WebDriver driver, Path file, String baseUrl) throws IOException {
        if (!Files.exists(file)) return;
        driver.get(baseUrl); // must be on-domain before adding cookies
        String json = Files.readString(file);
        List<SerializableCookie> list = GSON.fromJson(json, LIST_TYPE);
        for (SerializableCookie sc : list) {
            driver.manage().addCookie(sc.toSelenium());
        }
        driver.navigate().refresh();
    }

    public static class SerializableCookie {
        String name;
        String value;
        String domain;
        String path;
        Long expiryEpoch;   // seconds since epoch, nullable
        boolean isSecure;
        boolean isHttpOnly;


        static SerializableCookie from(Cookie cookie) {
            SerializableCookie sc = new SerializableCookie();
            sc.name = cookie.getName();
            sc.value = cookie.getValue();
            sc.domain = cookie.getDomain();
            sc.path = cookie.getPath();
            Date exp = cookie.getExpiry();
            sc.expiryEpoch = exp == null ? null : exp.toInstant().getEpochSecond();
            sc.isSecure = cookie.isSecure();
            sc.isHttpOnly = cookie.isHttpOnly();
            return sc;
        }

        Cookie toSelenium() {
            Cookie.Builder builder = new Cookie.Builder(name, value)
                    .domain(domain)
                    .path(path)
                    .isSecure(isSecure)
                    .isHttpOnly(isHttpOnly);

            if (expiryEpoch != null) {

                Date exp = Date.from(Instant.ofEpochSecond(expiryEpoch).atOffset(ZoneOffset.UTC).toInstant());
                builder.expiresOn(exp);
            }

            return builder.build();
        }
    }
}
