package pl.rynek.channel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ChannelServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
