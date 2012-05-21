package play.modules.mongodb.jackson

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import util.Random
import reflect.BeanProperty
import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import net.vz.mongodb.jackson.{JacksonDBCollection, MongoCollection, Id}
import com.mongodb.{WriteConcern, BasicDBObject, Mongo}

case class MongoDBSpec() extends Specification {

  "The MongoDB plugin" should {

    "be configurable by just uri" in new Setup {
      implicit val app = fakeApp(Map("mongodb.uri" -> "mongodb://localhost:27017/databasename"))
      running(app) {
        val addresses = MongoDB.collection(collName, classOf[MockObject], classOf[String]).getDB.getMongo.getAllAddress
        addresses.size mustEqual 1
        addresses.get(0).getHost mustEqual "localhost"
        addresses.get(0).getPort mustEqual 27017
      }
    }

    /*
    "be configurable by uri with username and password" in new Setup {
      implicit val app = fakeApp(Map("mongodb.uri" -> "mongodb://username:password@localhost:27017/databasename"))
      running(app) {
        val addresses = MongoDB.collection(collName, classOf[MockObject], classOf[String]).getDB.getMongo.getAllAddress
        addresses.size mustEqual 1
        addresses.get(0).getHost mustEqual "localhost"
        addresses.get(0).getPort mustEqual 27017
      }
    }
    */

    "be configurable by just host" in new Setup {
      implicit val app = fakeApp(Map("mongodb.servers" -> "localhost"))
      running(app) {
        val addresses = MongoDB.collection(collName, classOf[MockObject], classOf[String]).getDB.getMongo.getAllAddress
        addresses.size mustEqual 1
        addresses.get(0).getHost mustEqual "localhost"
        addresses.get(0).getPort mustEqual 27017
      }
    }

    "be configurable by host and port" in new Setup {
      implicit val app = fakeApp(Map("mongodb.servers" -> "localhost:27017"))
      running(app) {
        val addresses = MongoDB.collection(collName, classOf[MockObject], classOf[String]).getDB.getMongo.getAllAddress
        addresses.size mustEqual 1
        addresses.get(0).getHost mustEqual "localhost"
        addresses.get(0).getPort mustEqual 27017
      }
    }

    /*
    // This is commented out because it's not usual for me to have a MongoDB database requiring authentication.
    "be configurable with authentication" in new Setup {
      implicit val app = fakeApp(Map("mongodb.credentials" -> "user:password"))
      running(app) {
        val coll = MongoDB.collection(collName, classOf[MockObject], classOf[String])
        coll.count mustEqual 0
      }
    }
    */

    "be configurable as a replica set" in new Setup {
      implicit val app = fakeApp(Map("mongodb.servers" -> "localhost:27017,localhost:27017"))
      running(app) {
        val addresses = MongoDB.collection(collName, classOf[MockObject], classOf[String]).getDB.getMongo.getAllAddress
        addresses.size mustEqual 2
        addresses.get(0).getHost mustEqual "localhost"
        addresses.get(0).getPort mustEqual 27017
        addresses.get(1).getHost mustEqual "localhost"
        addresses.get(1).getPort mustEqual 27017
      }
    }

    "be able to map scala classes" in new Setup {
      implicit val app = fakeApp(Map.empty)
      running(app) {
        val coll = MongoDB.collection(collName, classOf[MockObject], classOf[String])
        val obj = new MockObject("someid", List("one", "two", "three"))
        coll.save(obj)
        val result = coll.findOneById("someid")
        result must_!= null
        result.id must_== obj.id
        result.values must_== obj.values
      }
    }

    "use a custom global configurer when configured" in new Setup {
      implicit val app = fakeApp(Map("mongodb.objectMapperConfigurer" -> classOf[MockGlobalConfigurer].getName))
      running(app) {
        val coll = MongoDB.collection(collName, classOf[MockObject], classOf[String])
        coll.getDbCollection.save(new BasicDBObject("_id", "someid").append("values", "single"))
        // This will throw an exception if the custom object mapper isn't used
        val result = coll.findOneById("someid")
        result must_!= null
        result.id must_== "someid"
        result.values must_== List("single")
      }
    }

    "use a custom per collection configurer when configured" in new Setup {
      implicit val app = fakeApp(Map("mongodb.objectMapperConfigurer" -> classOf[MockPerCollectionConfigurer].getName))
      running(app) {
        val coll = MongoDB.collection(collName, classOf[MockObject], classOf[String])
        coll.getDbCollection.save(new BasicDBObject("_id", "someid").append("values", "single"))
        // This will throw an exception if the custom object mapper isn't used
        val result = coll.findOneById("someid")
        result must_!= null
        result.id must_== "someid"
        result.values must_== List("single")
      }
    }

    "be able to infer the collection name using camel case" in new Setup {
      implicit val app = fakeApp(Map.empty)
      running(app) {
        val coll = MongoDB.collection(classOf[MockObject], classOf[String])
        coll.getName must_== "mockObject"
      }
    }

    "be able to use the @MongoCollection annotation to find the collection name" in new Setup {
      implicit val app = fakeApp(Map.empty)
      running(app) {
        val coll = MongoDB.collection(classOf[MockAnnotatedObject], classOf[String])
        coll.getName must_== "blah"
      }
    }

    "be able infer the key type using MongoDocument" in new Setup {
      implicit val app = fakeApp(Map.empty)
      running(app) {
        val coll = MongoDB.collection(collName, classOf[MockDocument])
        val doc = new MockDocument("foo")
        coll.save(doc).getSavedId must_== "foo"
      }
    }

    "use default write concern if not configured" in new Setup {
      implicit val app = fakeApp(Map.empty)
      running(app) {
        val coll = MongoDB.collection(classOf[MockObject], classOf[String])
        coll.getWriteConcern must_== coll.getDB.getMongo.getWriteConcern
      }
    }

    "use configured write concern if configured" in new Setup {
      implicit val app = fakeApp(Map("mongodb.defaultWriteConcern" -> "majority"))
      running(app) {
        val coll = MongoDB.collection(classOf[MockObject], classOf[String])
        coll.getWriteConcern must_== WriteConcern.MAJORITY
      }
    }
  }

  trait Setup extends After {
    def fakeApp(o: Map[String, String]) = {
      FakeApplication(additionalConfiguration = o ++ Map("ehcacheplugin" -> "disabled",
        "mongodbJacksonMapperCloseOnStop" -> "disabled"))
    }

    val collName = "mockcoll" + new Random().nextInt(10000)

    def after {
      val mongo = new Mongo()
      mongo.getDB("play").getCollection(collName).drop()
    }
  }

}

class MockObject(@Id val id: String,
                 @JsonProperty("values") @BeanProperty val values: List[String]) {
  @Id def getId = id;
}

class MockGlobalConfigurer extends ObjectMapperConfigurer {
  def configure(defaultMapper: ObjectMapper) =
    defaultMapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

  def configure(globalMapper: ObjectMapper, collectionName: String, objectType: Class[_], keyType: Class[_]) = globalMapper
}

class MockPerCollectionConfigurer extends ObjectMapperConfigurer {
  def configure(defaultMapper: ObjectMapper) = defaultMapper

  def configure(globalMapper: ObjectMapper, collectionName: String, objectType: Class[_], keyType: Class[_]) =
    globalMapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
}

@MongoCollection(name = "blah")
class MockAnnotatedObject(@Id val id: String) {
  @Id def getId = id;
}

class MockDocument(@Id val id: java.lang.String) extends KeyTyped[java.lang.String] {
  @Id def getId = id;
}

