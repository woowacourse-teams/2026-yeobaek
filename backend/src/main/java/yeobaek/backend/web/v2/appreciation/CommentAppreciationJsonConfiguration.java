package yeobaek.backend.web.v2.appreciation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.web.v2.appreciation.AppreciationRequests.CommentData;

@Configuration(proxyBeanMethods = false)
public class CommentAppreciationJsonConfiguration {

    @Bean
    JacksonModule commentAppreciationRequestModule() {
        return new SimpleModule("v2-comment-appreciation-requests")
                .registerSubtypes(new NamedType(CommentData.class, AppreciationKind.COMMENT.value()));
    }
}
