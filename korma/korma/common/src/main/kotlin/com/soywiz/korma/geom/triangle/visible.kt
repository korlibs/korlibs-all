package com.soywiz.korma.geom.triangle

/*
public class VisiblePolygon {
	protected var sweepContext:SweepContext;
	protected var sweep:Sweep;
	protected var triangulated:Boolean;

	public fun VisiblePolygon() {
		this.reset();
		//PriorityQueue
	}

	public fun addPolyline(polyline:ArrayList<Point>):Unit {
		if (this.triangulated) throw('Shape already triangulated');
		this.sweepContext.addPolyline(polyline);
	}

	public fun addHole(polyline:ArrayList<Point>):Unit {
		addPolyline(polyline);
	}

	public fun addBox(p1:Point, p2:Point):Unit {
		var polyline:ArrayList<Point> = ArrayList<Point>();
		polyline.push(Point(p1.x, p1.y));
		polyline.push(Point(p2.x, p1.y));
		polyline.push(Point(p2.x, p2.y));
		polyline.push(Point(p1.x, p2.y));
		addPolyline(polyline);
	}

	public fun addPolylineString(pStr:String, dx:Double = 0.0, dy:Double = 0.0, pointsSeparator:String = ',', componentSeparator:String = ' '):Unit {
		addPolyline(parseVectorPoints(pStr, dx, dy, pointsSeparator, componentSeparator));
	}

	public fun addRectangle(x:Double, y:Double, width:Double, height:Double):Unit {
		addBox(Point(x, y), Point(x + width, y + height));
	}

	public fun addBoxString(pStr:String):Unit {
		var polyline:ArrayList<Point> = parseVectorPoints(pStr);
		if (polyline.length != 2) throw("Box should contain exactly two points");
		addBox(polyline[0], polyline[1]);
	}

	public fun reset():Unit {
		this.sweepContext = SweepContext();
		this.sweep = Sweep(sweepContext);
		this.triangulated = false;
	}

	static public fun parseVectorPoints(str:String, dx:Double = 0.0, dy:Double = 0.0, pointsSeparator:String = ',', componentSeparator:String = ' '):ArrayList<Point> {
		var points:ArrayList<Point> = ArrayList<Point>();
		for each (var xy_str:String in str.split(pointsSeparator)) {
			var xyl:Array = xy_str.replace(/^\s+/, '').replace(/\s+$/, '').split(componentSeparator);
			points.push(Point(parseFloat(xyl[0]) + dx, parseFloat(xyl[1]) + dy));
			//println(xyl);
		}
		return points;
	}

	protected fun _triangulateOnce():Unit {
		if (!this.triangulated) {
			this.triangulated = true;
			this.sweep.triangulate();
		}
	}

	/**
	 * Performs triangulation if it hasn't done yet.
	 *
	 * NOTE: With the current implementation and to retriangulate once it has been already triangulated
	 * you should first call reset() and then add a set of points again.
	 *
	 * @return List of the triangles.
	 */
	public fun get triangles():ArrayList<Triangle> {
		_triangulateOnce();
		return this.sweepContext.triangles;
	}

	public fun get edge_list():ArrayList<Edge> {
		_triangulateOnce();
		return this.sweepContext.edge_list;
	}

	public fun getTriangleAtPoint(p:Point):Triangle {
		for each (var triangle:Triangle in triangles) {
			if (triangle.pointInsideTriangle(p)) return triangle;
		}
		return null;
	}

	static public fun drawTriangleHighlight(t:Triangle, g:Graphics):Unit {
		var pl:ArrayList<Point> = t.points;
		g.lineStyle(0, 0xFFFFFF, 0.4);
		g.beginFill(0xFFFFFF, 0.4);
		{
			g.moveTo(pl[0].x, pl[0].y);
			g.lineTo(pl[1].x, pl[1].y);
			g.lineTo(pl[2].x, pl[2].y);
			g.lineTo(pl[0].x, pl[0].y);
		}
		g.endFill();
	}

	public fun drawShape(g:Graphics, drawTriangleFill:Boolean = true, drawTriangleEdges:Boolean = true, drawShapeEdges:Boolean = true, drawTriangleCenters:Boolean = true):Unit {
		var t:Triangle;
		var pl:ArrayList<Point>;

		if (drawTriangleFill) {
			for (t in this.triangles) {
				pl = t.points;
				g.beginFill(0xFF0000);
				{
					g.moveTo(pl[0].x, pl[0].y);
					g.lineTo(pl[1].x, pl[1].y);
					g.lineTo(pl[2].x, pl[2].y);
					g.lineTo(pl[0].x, pl[0].y);
				}
				g.endFill();
			}
		}

		if (drawTriangleEdges) {
			g.lineStyle(1, 0x0000FF, 1);
			for (t in this.triangles) {
				pl = t.points;
				g.moveTo(pl[0].x, pl[0].y);
				g.lineTo(pl[1].x, pl[1].y);
				g.lineTo(pl[2].x, pl[2].y);
				g.lineTo(pl[0].x, pl[0].y);
			}
		}

		if (drawShapeEdges) {
			g.lineStyle(2, 0x00FF00, 1);
			for (e in this.edge_list) {
				g.moveTo(e.p.x, e.p.y);
				g.lineTo(e.q.x, e.q.y);
			}
		}

		if (drawTriangleCenters) {
			g.lineStyle(2, 0xFFFFFF, 1);
			for each (t in this.triangles) {
				pl = t.points;
				var x:Double = (pl[0].x + pl[1].x + pl[2].x) / 3.0;
				var y:Double = (pl[0].y + pl[1].y + pl[2].y) / 3.0;
				g.moveTo(x - 2, y - 2); g.lineTo(x + 2, y + 2);
				g.moveTo(x - 2, y + 2); g.lineTo(x + 2, y - 2);
				//g.moveTo(x, y); g.drawCircle(x, y, 1);
			}
		}
	}
}
 */