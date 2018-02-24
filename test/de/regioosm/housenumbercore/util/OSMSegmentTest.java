package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import de.regioosm.housenumbercore.util.OSMSegment.OSMType;

public class OSMSegmentTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void OSMSegmentTest() {
		OSMSegment segm = new OSMSegment();
		assertEquals( (long) 0L, (long) segm.osmId);
		assertNull(segm.osmType);
		assertNull(segm.geometryWKT);
		assertNull(segm.tags);
	}

	@Test
	public void OSMSegmentOSMTypeLongStringTest() {
		OSMSegment segm = new OSMSegment(OSMType.way, 471112131L, "LINESTRING(10.0 40.0,10.1 40.1)");
		assertEquals(OSMType.way.toString(), segm.osmType.toString());
		assertEquals( (long) 471112131L, (long) segm.osmId);
		assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", segm.geometryWKT);
		assertNull(segm.tags);
	}

	@Test
	public void OSMSegmentOSMTypeLongStringOSMTagListTest() {
		List<OSMTag> tags = new ArrayList<>();
		tags.add(new OSMTag("highway",  "service"));
		tags.add(new OSMTag("maxspeed", "50"));
		OSMSegment segm = new OSMSegment(OSMType.way, 471112131L, 
			"LINESTRING(10.0 40.0,10.1 40.1)", tags);
		assertEquals(OSMType.way.toString(), segm.osmType.toString());
		assertEquals( (long) 471112131L, (long) segm.osmId);
		assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", segm.geometryWKT);
		assertEquals("highway", segm.tags.get(0).getKey());
		assertEquals("service", segm.tags.get(0).getValue());
		assertEquals("maxspeed", segm.tags.get(1).getKey());
		assertEquals("50", segm.tags.get(1).getValue());
	}

	@Test
	public void setWayFromOsmNodesTest() {
		OSMSegment segm = new OSMSegment();
		Map<Long, Node> allNodes = new HashMap<>();
		List<WayNode> waynodes = new ArrayList<>();
		
		Node node1 = new Node(new CommonEntityData(1L, 1, new Date(), new OsmUser(1, "dummyuser"), 1L), 40.0D, 10.0D);
		Node node2 = new Node(new CommonEntityData(2L, 1, new Date(), new OsmUser(1, "dummyuser"), 1L), 40.1D, 10.1D);
		Node node3 = new Node(new CommonEntityData(3L, 1, new Date(), new OsmUser(1, "dummyuser"), 1L), 40.2D, 10.0D);
		Node node4 = new Node(new CommonEntityData(4L, 1, new Date(), new OsmUser(1, "dummyuser"), 1L), 40.1D, 9.9D);

		allNodes.put(node1.getId(), node1);
		allNodes.put(node2.getId(), node2);
		allNodes.put(node3.getId(), node3);
		allNodes.put(node4.getId(), node4);

		waynodes.add(new WayNode(node1.getId()));
		waynodes.add(new WayNode(node2.getId()));

		segm.setWayFromOsmNodes(allNodes, waynodes);
		assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", segm.geometryWKT);
	}
	
	@Test
	public void setTagsTest() {
		OSMSegment segm = new OSMSegment();

		List<OSMTag> tags = new ArrayList<>();
		tags.add(new OSMTag("highway",  "service"));
		tags.add(new OSMTag("maxspeed", "50"));

		segm.setTags(tags);

		assertEquals("highway", segm.tags.get(0).getKey());
		assertEquals("service", segm.tags.get(0).getValue());
		assertEquals("maxspeed", segm.tags.get(1).getKey());
		assertEquals("50", segm.tags.get(1).getValue());
	}

}
