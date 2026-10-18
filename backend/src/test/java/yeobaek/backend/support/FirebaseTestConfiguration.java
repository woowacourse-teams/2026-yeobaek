package yeobaek.backend.support;

import static org.mockito.Mockito.mock;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class FirebaseTestConfiguration {

    @Bean
    static BeanFactoryPostProcessor firebaseSdkTestDoubles() {
        return beanFactory -> {
            beanFactory.registerSingleton("firebaseApp", mock(FirebaseApp.class));
            beanFactory.registerSingleton("firebaseMessaging", mock(FirebaseMessaging.class));
        };
    }
}
