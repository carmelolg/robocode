package peluria_robot;

import java.awt.geom.Point2D;

public class TriUtil {
	// Project the Point
	public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length, sourceLocation.y + Math.cos(angle) * length);
	}

	public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static double absoluteBearing(double x1,double y1,double x2 , double y2) {
		return Math.atan2(x2 - x1, y2 - y1);
	}

	// return value if > min and < max else return min if < min or max if > max
	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	// Give power return the velocity of bullet
	public static double bulletVelocity(double power) {
		return (20.0 - (3.0 * power));
	}

	// The largest angle that could possibly not hit a bot
	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

}
