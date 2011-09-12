package geoscript.render

import org.junit.Test
import static org.junit.Assert.*
import geoscript.layer.Shapefile
import geoscript.style.*

/**
 * The Png UnitTest
 * @author Jared Erickson
 */
class PngTestCase {

    @Test void render() {
        File file = new File(getClass().getClassLoader().getResource("states.shp").toURI())
        Shapefile shapefile = new Shapefile(file)
        Symbolizer sym = new Stroke('black', 2) + new Fill('gray',0.75)

        File imageFile = File.createTempFile("states",".png")
        println(imageFile)

        Png png = new Png()
        png.render([shapefile], [sym], shapefile.bounds.expandBy(0.1), [500,500], [file: imageFile])
        assertTrue imageFile.exists()
    }

}