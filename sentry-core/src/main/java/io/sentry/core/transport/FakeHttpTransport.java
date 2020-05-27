package io.sentry.core.transport;

import com.google.gson.Gson;
import com.jakewharton.nopen.annotation.Open;
import io.sentry.core.Breadcrumb;
import io.sentry.core.SentryEvent;
import io.sentry.core.SentryOptions;
import io.sentry.core.protocol.User;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Open
public class FakeHttpTransport implements ITransport {

  private final SentryOptions options;

  public FakeHttpTransport(SentryOptions options) {
    this.options = options;
  }

  @Override
  public TransportResult send(SentryEvent event) {
    // if dsn is invalid return error
    String DSN = "https://ABC123@sentry.io/canva";
    if (!DSN.equals(options.getDsn())) {
      System.out.println("Sentry: request status_code=403, data={\"error\": \"Forbidden\"}");
      options
          .getLogger()
          .log(io.sentry.core.SentryLevel.DEBUG, "Request data: dsn=" + options.getDsn());
      return TransportResult.error(403, 1000);
    }

    // if data too big return error
    String data = new Gson().toJson(event);
    int limit = 100 * 1024;
    if (data.getBytes(Charset.defaultCharset()).length > limit) {
      System.out.println(
          "Sentry: request status_code=400, data={\"error\": "
              + "\"Payload too large, actual "
              + data.getBytes(Charset.defaultCharset()).length
              + " bytes, expected less than "
              + limit
              + " bytes\"}");
      options.getLogger().log(io.sentry.core.SentryLevel.DEBUG, "Request data: payload=" + data);
      return TransportResult.error(400, 1000);
    }

    System.out.println(
        "Sentry: request status_code=200, data={"
            + "\"throwable\": \""
            + (event.getThrowable() != null ? event.getThrowable().getMessage() : "null")
            + "\", "
            + "\"message\": \""
            + (event.getMessage() != null ? event.getMessage().getFormatted() : "null")
            + "\", "
            + "\"level\": \""
            + event.getLevel()
            + "\", "
            + "\"user\": \""
            + (event.getUser() != null ? printUser(event.getUser()) : "null")
            + "\", "
            + "\"tags\": \""
            + printMap(event.tags)
            + "\", "
            + "\"extras\": \""
            + printMap(event.extra)
            + "\", "
            + "\"breadcrumbs\": \""
            + printBreadcrumbs(event.getBreadcrumbs())
            + "\"}");
    return TransportResult.success();
  }

  private String printUser(User user) {
    return "{"
        + "\"id\":"
        + "\""
        + user.getId()
        + "\""
        + ", "
        + "\"email\":"
        + "\""
        + user.getEmail()
        + "\""
        + ", "
        + "\"name\":"
        + "\""
        + user.getUsername()
        + "\""
        + "}";
  }

  private String printBreadcrumbs(List<Breadcrumb> breadcrumbs) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < breadcrumbs.size(); i++) {
      Breadcrumb breadcrumb = breadcrumbs.get(i);
      sb.append("{\"message\":").append("\"").append(breadcrumb.getMessage()).append("\"}");
      if (i < breadcrumbs.size() - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  private <K, V> String printMap(Map<K, V> map) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      Map.Entry<K, V> entry = entries.get(i);
      sb.append("{\"")
          .append(entry.getKey())
          .append("\":\"")
          .append(entry.getValue())
          .append("\"}");
      if (i < entries.size() - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public void close() {}
}
