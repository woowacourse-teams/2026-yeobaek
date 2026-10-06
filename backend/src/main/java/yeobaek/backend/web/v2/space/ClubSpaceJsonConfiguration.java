package yeobaek.backend.web.v2.space;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.web.v2.space.SpaceRequests.CreateClubData;
import yeobaek.backend.web.v2.space.SpaceRequests.JoinClubData;

@Configuration(proxyBeanMethods = false)
public class ClubSpaceJsonConfiguration {

    @Bean
    JacksonModule clubSpaceRequestModule() {
        return new SimpleModule("v2-club-space-requests")
                .registerSubtypes(new NamedType(CreateClubData.class, SpaceKind.CLUB.value()))
                .registerSubtypes(new NamedType(JoinClubData.class, SpaceKind.CLUB.value()));
    }
}
