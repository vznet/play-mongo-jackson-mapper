package play.modules.mongodb.jackson

import play.Plugin
import java.util.concurrent.ConcurrentHashMap
import net.vz.mongodb.jackson.JacksonDBCollection
import org.codehaus.jackson.map.ObjectMapper
import net.vz.mongodb.jackson.internal.{MongoAnnotationIntrospector, MongoJacksonHandlerInstantiator, MongoJacksonMapperModule}
import play.api.Application
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mongodb.{Mongo, ServerAddress}

/**
 * MongoDB Jackson Mapper module for play framework
 */
object MongoDB {
  private def error = throw new Exception(
    "MongoDBPlugin is not enabled"
  )

  /**
   * Get a collection.  This method takes an implicit application as a parameter, and so is the best option to use from
   * Scala, and can also be used while testing to pass in a fake application.
   *
   * @param name The name of the collection
   * @param entityType The type of the entity
   * @param keyType The type of the key
   */
  def collection[T, K](name: String, entityType: Class[T], keyType: Class[K])(implicit app: Application) =
    app.plugin[MongoDBPlugin].map(_.getCollection(name, entityType, keyType)).getOrElse(error)

  /**
   * Get a collection.  This method uses the current application, and so will not work outside of the context of a
   * running app.
   *
   * @param name The name of the collection
   * @param entityType The type of the entity
   * @param keyType The type of the key
   */
  def getCollection[T, K](name: String, entityType: Class[T], keyType: Class[K]) = {
    // This makes simpler use from Java
    import play.api.Play.current
    current.plugin[MongoDBPlugin].map(_.getCollection(name, entityType, keyType)).getOrElse(error)
  }
}

class MongoDBPlugin(val app: Application) extends Plugin {

  private val cache = new ConcurrentHashMap[(String, Class[_], Class[_]), JacksonDBCollection[_, _]]()

  private lazy val (mongo, db, mapper) = {
    // Configure MongoDB
    // DB server string is comma separated, with optional port number after a colon
    val mongoDbServers = app.configuration.getString("mongodb.servers").getOrElse("localhost")
    // Parser for port number
    object Port {
      def unapply(s: String): Option[Int] = try {
        Some(s.toInt)
      } catch {
        case _: java.lang.NumberFormatException => None
      }
    }
    import scala.collection.JavaConversions._
    // Split servers
    val mongo = mongoDbServers.split(',') map {
      // Convert each server string to a ServerAddress, matching based on arguments
      _.split(':') match {
        case Array(host: String) => new ServerAddress(host)
        case Array(host: String, Port(port)) => new ServerAddress(host, port)
        case _ => throw new IllegalArgumentException("mongodb.servers must be a comma separated list of hostnames with" +
          " optional port numbers after a colon, eg 'host1.example.org:1111,host2.example.org'")
      }
    } match {
      case Array(single: ServerAddress) => new Mongo(single)
      case multiple: Array[ServerAddress] => new Mongo(multiple.toList)
    }

    // Load database
    val dbName = app.configuration.getString("mongodb.database").getOrElse("play")
    val db = mongo.getDB(dbName)

    // Authenticate if necessary
    val credentials = app.configuration.getString("mongodb.credentials")
    if (credentials.isDefined) {
      credentials.get.split(":", 2) match {
        case Array(username: String, password: String) => {
          if (!db.authenticate(username, password.toCharArray)) {
            throw new IllegalArgumentException("MongoDB authentication failed for user: " + username + " on database: "
              + dbName);
          }
        }
        case _ => throw new IllegalArgumentException("mongodb.credentials must be a username and password separated by a colon")
      }
    }

    // Configure the object mapper
    var mapper = new ObjectMapper;
    mapper = mapper.withModule(MongoJacksonMapperModule.INSTANCE)
    // This is needed by mongo jackson mapper and the module system doesn't support adding these
    mapper.setHandlerInstantiator(new MongoJacksonHandlerInstantiator(new MongoAnnotationIntrospector(mapper.getDeserializationConfig)))
    mapper.withModule(new DefaultScalaModule)
    (mongo, db, mapper)
  }

  def getCollection[T, K](name: String, entityType: Class[T], keyType: Class[K]): JacksonDBCollection[T, K] = {
    if (cache.containsKey((name, entityType, keyType))) {
      cache.get((name, entityType, keyType)).asInstanceOf[JacksonDBCollection[T, K]]
    } else {
      val coll = JacksonDBCollection.wrap(db.getCollection(name), entityType, keyType, mapper)
      cache.putIfAbsent((name, entityType, keyType), coll)
      coll
    }
  }

  override def onStart() {
    mongo
  }

  override def onStop() {
    // This config exists for testing, because when you close mongo, it closes all connections, and specs runs the
    // tests in parallel.
    if (!app.configuration.getString("mongodbJacksonMapperCloseOnStop").filter(_ == "disabled").isDefined) {
      mongo.close()
      cache.clear()
    }
  }

  override def enabled() = !app.configuration.getString("mongodb.jackson.mapper").filter(_ == "disabled").isDefined
}