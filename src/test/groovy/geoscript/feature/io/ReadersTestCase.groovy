package geoscript.feature.io

import org.junit.Test
import static org.junit.Assert.*

/**
 * The Readers Unit Test
 * @author Jared Erickson
 */
class ReadersTestCase {

    @Test void list() {
        List<Reader> readers = Readers.list()
        assertNotNull readers
        assertTrue readers.size() > 0
    }

    @Test void find() {
        Reader reader = Readers.find("kml")
        assertNotNull reader

        reader = Readers.find("asdf")
        assertNull reader
    }
}
