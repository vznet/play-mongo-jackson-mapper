package play.modules.mongodb.jackson

/**
 * An object that declares the type of its key.  This interface serves to just define the type of the key, so that it
 * doesn't need to be passed to every single call to get the database, but rather can be worked out implicitly.  It is
 * entirely optional to use it.
 */
trait KeyTyped[K] {
}