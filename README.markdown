MongoDB Jackson Mapper Play 2.0 Module
======================================

This project provides a very simple plugin for Play 2.0 that allows easy access of mongo-jackson-mapper wrapped connections to MongoDB.  [MongoDB Jackson Mapper](http://vznet.github.com/mongo-jackson-mapper) is a lightweight POJO mapper that uses Jackson to serialise/deserialise MongoDB documents.  Because it uses Jackson, with bson4jackson to parse responses, it is fast, very flexible and performant.  It provides most of the same CRUB methods that the MongoDB Java driver provides, plus a more convenient query and updating interface.

Installation
------------

Add the following to your ``build.sbt`` file:

    libraryDependencies += "net.vz.mongodb.jackson" % "play-mongo-jackson-mapper" % "1.0.0-rc.1"

Scala quick start
-----------------

    import org.codehaus.jackson.annotate.JsonProperty
    import reflect.BeanProperty
    import javax.persistence.Id
    import play.api.Play.current
    import play.modules.mongodb.jackson.MongoDB
    import scala.collection.JavaConversions._

    class BlogPost(@ObjectId @Id val id: String,
                 @BeanProperty @JsonProperty("date") val date: Date,
                 @BeanProperty @JsonProperty("title") val title: String,
                 @BeanProperty @JsonProperty("author") val author: String,
                 @BeanProperty @JsonProperty("content") val content: String,
                 @BeanProperty @JsonProperty("comments") val comments: List[Comment]) {
        @ObjectId @Id def getId = id;
    }

    object BlogPost {
        private lazy val db = MongoDB.collection("blogposts", classOf[BlogPost], classOf[String])

        def save(blogPost: BlogPost) { db.save(blogPost) }
        def findById(id: String) = Option(db.findOneById(id))
        def findByAuthor(author: String) = db.find().is("author", author).iterableAsScalaIterable
    }

A few notes:

* ``MongoDB.collection`` has an implicit ``Application`` argument.  The easiest way to ensure you have an implicit ``Application`` available is to import ``play.api.Play.current``.  Alternatively, use ``getCollection``, and the current application will automatically be used.
* MongoDB requires ids to have the name ``_id``, however, Scala won't let you annotate a field that starts with an underscore as ``@BeanProperty``.  The simplest solution is to use the ``@Id`` annotation, but then you also need to provide your own getter, otherwise when you serialise it, Jackson won't know that the ``id`` field should be serialised as ``@Id``.
* The reason each property needs to be ``@JsonProperty`` annotated is that the JVM doesn't allow putting method parameter names into bytecode, so Jackson can't reflect on the constructor to find out which argument is for which field.
* Queries are returned as Java iterables, so you probably want to convert them to Scala iterables.
* Returning single results as an ``Option`` allows more idiomatic use of the result.

Configuration
-------------

    # Configure the database name
    mongodb.database=databasename
    # Configure credentials
    mongodb.credentials="user:pass"
    # Configure the servers
    mongodb.servers=host1.example.com:27017,host2.example.com,host3.example.com:19999

The database name defaults to play.  The servers defaults to localhost.  Specifying a port number is optional, it defaults to the default MongoDB port.  If you specify one server, MongoDB will be used as a single server, if you specify multiple, it will be used as a replica set.

Features
--------

* Manages lifecycle of MongoDB connection pool
* Caches JacksonDBCollection instances, so looking up a JacksonDBCollection is cheap
* Configures Jackson to use the FasterXML ``DefaultScalaModule``, so scala mapping works out of the box.

