package geoscript.layer

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.feature.Schema
import geoscript.geom.Bounds
import geoscript.geom.Point
import geoscript.render.Draw
import geoscript.workspace.Directory
import geoscript.workspace.Geobuf
import geoscript.workspace.H2
import geoscript.workspace.Workspace
import org.geotools.grid.GridElement
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.awt.Color

import static org.junit.Assert.*

/**
 * The Graticule Unit Test
 * @author Jared Erickson
 */
class GraticuleTestCase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test void createSquaresToMemory() {
        Layer layer = Graticule.createSquares(new Bounds(110.0, -45.0, 160.0, -5.0, "EPSG:4326"), 10, -1)
        assertEquals 20, layer.count
        assertEquals "grid", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 20
        }
    }

    @Test void createSquaresToShapefile() {
        File dir = temporaryFolder.newFolder("squares")
        Workspace workspace = new Directory(dir)
        Layer layer = Graticule.createSquares(new Bounds(110.0, -45.0, 160.0, -5.0, "EPSG:4326"), 10, 1,
                workspace: workspace, layer: "squares")
        assertEquals 20, layer.count
        assertEquals "squares", layer.name
        assertTrue layer.schema.has("the_geom")
        assertEquals "MultiPolygon", layer.schema["the_geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 20
        }
    }

    @Test void createHexagonsToMemory() {
        Layer layer = Graticule.createHexagons(new Bounds(0, 0, 100, 100), 5.0, -1, "flat")
        assertEquals 143, layer.count
        assertEquals "grid", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 143
        }
    }

    @Test void createHexagonsToProperty() {
        File file = temporaryFolder.newFile("graticule.properties")
        Workspace workspace = new geoscript.workspace.Property(file)
        Layer layer = Graticule.createHexagons(new Bounds(0, 0, 100, 100), 5.0, 1, "angled",
                workspace: workspace, layer: "hexagons")
        assertEquals 143, layer.count
        assertEquals "hexagons", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 143
        }
    }

    @Test void createHexagonsWithCustomSchema() {
        Schema schema = new Schema("hexagon", [
                new Field("geom", "Polygon"),
                new Field("color", "java.awt.Color")
        ])
        Bounds b = new Bounds(0,0,100,100)
        Layer layer = Graticule.createHexagons(b, 5.0, -1.0, "flat", schema: schema, setAttributes: { GridElement e, Map attributes ->
            int green = (255 * e.center.x / b.width)  as int
            int blue  = (255 * e.center.y / b.height) as int
            attributes["color"] = new Color(0, green, blue)
        })
        assertEquals 143, layer.count
        assertEquals "hexagon", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertFalse layer.schema.has("id")
        assertTrue layer.schema.has("color")
        assertEquals "java.awt.Color", layer.schema["color"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertNotNull f["color"]
        }
    }

    @Test void createHexagonsOnlyIntersecting() {
        Layer states = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        Feature feature = states.first(filter: "STATE_NAME = 'Washington'")
        Layer layer = Graticule.createHexagons(feature.bounds.expandBy(1.0), 0.4, -1.0, "flat", createFeature: {GridElement e ->
            new Point(e.center.x, e.center.y).intersects(feature.geom)
        })
        assertEquals 45, layer.count
        assertEquals "grid", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue feature.geom.intersects(f.geom)
            assertTrue f["id"] >= 0 && f["id"] <= 45
        }
    }

    @Test void createLines() {
        Layer layer = Graticule.createLines(new Bounds(110.0, -45.0, 160.0, -5.0, "EPSG:4326"), [
                [orientation: 'vertical',   level: 2, spacing: 10],
                [orientation: 'vertical',   level: 1, spacing: 2 ],
                [orientation: 'horizontal', level: 2, spacing: 10],
                [orientation: 'horizontal', level: 1, spacing: 2 ]
        ], 0.1)
        assertEquals 47, layer.count
        assertEquals "grid", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "LineString", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 200
        }
    }

    @Test void createLinesToH2() {
        File file = temporaryFolder.newFolder("lines")
        Workspace workspace = new H2("lines", file)
        Layer layer = Graticule.createLines(new Bounds(110.0, -45.0, 160.0, -5.0, "EPSG:4326"), [
                [orientation: 'vertical',   level: 2, spacing: 10],
                [orientation: 'vertical',   level: 1, spacing: 2 ],
                [orientation: 'horizontal', level: 2, spacing: 10],
                [orientation: 'horizontal', level: 1, spacing: 2 ]
        ], 0.1, workspace: workspace, layer: "lines")
        assertEquals 47, layer.count
        assertEquals "lines", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "LineString", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 200
        }
    }

    @Test void createRectangles() {
        Layer layer = Graticule.createRectangles(new Bounds(0, 0, 100, 100), 10, 5, -1)
        assertEquals 200, layer.count
        assertEquals "grid", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Polygon", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "Integer", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            assertFalse f.geom.empty
            assertTrue f["id"] >= 0 && f["id"] <= 200
        }
    }

    @Test void createRectanglesToGeobuf() {
        File file = temporaryFolder.newFolder("rectangles")
        Workspace workspace = new Geobuf(file)
        Layer layer = Graticule.createRectangles(new Bounds(0, 0, 100, 100), 10, 5, -1,
                workspace: workspace, layer: "rectangles")
        assertEquals 200, layer.count
        assertEquals "rectangles", layer.name
        assertTrue layer.schema.has("geom")
        assertEquals "Geometry", layer.schema["geom"].typ
        assertTrue layer.schema.has("id")
        assertEquals "String", layer.schema["id"].typ
        layer.eachFeature { Feature f ->
            int id = f["id"] as int
            assertFalse f.geom.empty
            assertTrue id >= 0 && id <= 200
        }
    }
}
