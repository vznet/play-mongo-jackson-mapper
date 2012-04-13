package paly.modules.mongodb.jackson;

import net.vz.mongodb.jackson.JacksonDBCollection;
import org.junit.Test;
import play.modules.mongodb.jackson.KeyTyped;
import play.modules.mongodb.jackson.MongoDB;
import play.test.FakeApplication;

import javax.persistence.Id;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

public class MongoDBTest {
    @Test
    public void keyTypeInferenceShouldWorkForJava() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("ehcacheplugin", "disabled");
        config.put("mongodbJacksonMapperCloseOnStop", "disabled");
        final String collName = "mockcoll" + new Random().nextInt(10000);
        FakeApplication app = fakeApplication(config);
        running(app, new Runnable() {
            @Override
            public void run() {
                JacksonDBCollection<MockObject, String> coll = MongoDB.getCollection(collName, MockObject.class);
                MockObject o = new MockObject();
                o.id = "blah";
                assertThat(coll.save(o).getSavedId(), equalTo("blah"));
            }
        });
    }

    public class MockObject implements KeyTyped<String> {
        @Id
        public String id;
    }
}
