package ee.paaro.catalogsync.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "catalogue";
    public static final String PRODUCT_QUEUE = "catalogue.products";
    public static final String CATEGORY_QUEUE = "catalogue.categories";
    public static final String DLX = "catalogue.dlx";
    public static final String DEAD_LETTER_QUEUE = "catalogue.dead";

    @Bean
    TopicExchange catalogueExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX, true, false);
    }

    /**
     * Anything the consumer cannot apply after its retries lands here rather
     * than being dropped or redelivered forever. A message that fails is a thing
     * somebody has to look at, so it needs somewhere to wait.
     */
    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @Bean
    Queue productQueue() {
        return QueueBuilder.durable(PRODUCT_QUEUE).deadLetterExchange(DLX).build();
    }

    @Bean
    Queue categoryQueue() {
        return QueueBuilder.durable(CATEGORY_QUEUE).deadLetterExchange(DLX).build();
    }

    @Bean
    Binding productBinding() {
        return BindingBuilder.bind(productQueue()).to(catalogueExchange()).with("product.#");
    }

    @Bean
    Binding categoryBinding() {
        return BindingBuilder.bind(categoryQueue()).to(catalogueExchange()).with("category.#");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
