/**
 * Information on matched way point
 *
 * @author ab
 */
package btools.mapaccess;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public final class MatchedWaypoint {
  public OsmNode node1;
  public OsmNode node2;
  public OsmNode crosspoint;
  public OsmNode waypoint;
  public OsmNode correctedpoint;
  public String name;  // waypoint name used in error messages
  public double radius;  // distance in meter between waypoint and crosspoint
  public boolean direct;  // from this point go direct to next = beeline routing
  public int indexInTrack = 0;
  public double directionToNext = -1;
  public double directionDiff = 361;

  public List<MatchedWaypoint> wayNearest = new ArrayList<>();
  public boolean hasUpdate;

  public void writeToStream(DataOutput dos) throws IOException {
    dos.writeInt(node1.ilat);
    dos.writeInt(node1.ilon);
    dos.writeInt(node2.ilat);
    dos.writeInt(node2.ilon);
    dos.writeInt(crosspoint.ilat);
    dos.writeInt(crosspoint.ilon);
    dos.writeInt(waypoint.ilat);
    dos.writeInt(waypoint.ilon);
    dos.writeDouble(radius);
  }

  public static MatchedWaypoint readFromStream(DataInput dis) throws IOException {
    MatchedWaypoint mwp = new MatchedWaypoint();
    mwp.node1 = new OsmNode();
    mwp.node2 = new OsmNode();
    mwp.crosspoint = new OsmNode();
    mwp.waypoint = new OsmNode();

    mwp.node1.ilat = dis.readInt();
    mwp.node1.ilon = dis.readInt();
    mwp.node2.ilat = dis.readInt();
    mwp.node2.ilon = dis.readInt();
    mwp.crosspoint.ilat = dis.readInt();
    mwp.crosspoint.ilon = dis.readInt();
    mwp.waypoint.ilat = dis.readInt();
    mwp.waypoint.ilon = dis.readInt();
    mwp.radius = dis.readDouble();
    return mwp;
  }

}
