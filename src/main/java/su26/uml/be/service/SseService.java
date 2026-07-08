package su26.uml.be.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface SseService {

    SseEmitter createEmitter();

    void broadcast(String eventName, Object data);

    void broadcast(Map<String, Object> payload);
}
