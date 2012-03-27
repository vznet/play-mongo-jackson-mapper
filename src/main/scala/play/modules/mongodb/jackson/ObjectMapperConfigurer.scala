package play.modules.mongodb.jackson

import org.codehaus.jackson.map.ObjectMapper

/**
 * Configures an ObjectMapper.  Implementations must have a no argument constructor.
 */
trait ObjectMapperConfigurer {

  /**
   * Configure the given ObjectMapper for global use.  This will be called once, on application startup.  You may either
   * modify the object mapper passed in, or create a completely new one.  If you create a completely new one, then you
   * need to ensure that the MongoJacksonMapperModule is registered, using the MongoJacksonMapperModule.configure()
   * method.
   *
   * @param defaultMapper The default object mapper
   * @return The object mapper to use globally
   */
  def configure(defaultMapper: ObjectMapper): ObjectMapper

  /**
   * Configure an ObjectMapper for use by a particular collection, type and key combination.  This will be called once
   * for each collection name, object type and key type looked up.  Note that since ObjectMapper is mutable, if you want
   * to create a configuration specific to a particular collection, then you should probably be creating a new
   * ObjectMapper, ensuring that MongoJacksonMapperModule is registered.
   *
   * @param globalMapper The global object mapper
   * @param collectionName The name of the collection being mapped
   * @param objectType The type of the object the collection is being mapped to
   * @param keyType The type of the key of the object
   * @return The object mapper to use for this collection, type and key type combination
   */
  def configure(globalMapper: ObjectMapper, collectionName: String, objectType: Class[_], keyType: Class[_]): ObjectMapper
}